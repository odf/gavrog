import json
import re
import sys

import org.gavrog


def checkGraph(graph, writeInfo):
    if not graph.isLocallyStable():
        writeInfo('Error - next-nearest neighbor collisions')
        return False

    if graph.isLadder():
        writeInfo('Error - non-crystallographic (ladder)')
        return False

    if graph.hasSecondOrderCollisions():
        writeInfo('Error - second-order collisions')
        return False

    return True


def makeCsLookup(data):
    result = {}

    for entry in data:
        symbol = entry['symbol']
        if re.search(r'-[bcz][*0-9]*$', symbol) and symbol != 'llw-z':
            continue

        cs = tuple(sorted([
            tuple(v['coordinationSequence']) for v in entry['vertices']
        ]))

        if result.get(cs) is None:
            result[cs] = { 'rcsr': [], 'cgd': [] }
        result[cs]['rcsr'].append(symbol)
        result[symbol] = cs

    return result


inputPath, data2dPath, data3dPath = sys.argv[1:]

with open(data2dPath) as fp:
    data2d = json.load(fp)

with open(data3dPath) as fp:
    data3d = json.load(fp)

lookup2d = makeCsLookup(data2d)
lookup3d = makeCsLookup(data3d)

inputs = org.gavrog.joss.pgraphs.io.Net.iterator(inputPath)


while inputs.hasNext():
    graph = inputs.next()
    name = graph.name

    writeInfo = lambda msg: sys.stdout.write("%s: %s\n" % (name, msg))
    ok = checkGraph(graph, writeInfo)
    if not ok:
        continue

    M = graph.minimalImageMap()
    G = M.imageGraph

    cs = []
    for orb in G.nodeOrbits():
        v = orb.iterator().next()
        seq = []
        iter = G.coordinationSequence(v)
        iter.next()
        for i in range(10):
            seq.append(iter.next())
        cs.append(tuple(seq))
    cs = tuple(sorted(cs))

    lookup = lookup3d if G.dimension == 3 else lookup2d
    if lookup.get(cs) is None:
        lookup[cs] = { 'rcsr': [], 'cgd': [] }

    entry = lookup[cs]

    if name in entry['cgd']:
        print "%s: duplicate" % name
    elif name in entry['rcsr']:
        print "%s: ok!" % name
    else:
        if len(entry['rcsr']) == 0:
            print "%s: does not match anything" % name
        else:
            print "%s: mismatch (found %s)" % (
                name, ','.join(map(str, entry['rcsr']))
            )

        for seq in cs:
            print "    %s" % ' '.join(map(str, seq))

        if lookup.get(name):
            print "  RCSR entry for %s has:" % name
            for seq in lookup[name]:
                print "    %s" % ' '.join(map(str, seq))

    entry['cgd'].append(name)
