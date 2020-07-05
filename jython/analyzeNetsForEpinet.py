#!/bin/env jython
import math
import json
import sys

import org.gavrog.joss.pgraphs as pgraphs
import org.gavrog.joss.geometry as geometry


def run():
    def writeln(s=''):
        print s

    for source_path in sys.argv[1:]:
        index_in_source = 0

        for net in pgraphs.io.Net.iterator(source_path):
            index_in_source += 1

            writeln('{')
            writeln('  "source_path": "%s",' % source_path)
            writeln('  "index_in_source": %d,' % index_in_source)

            warnings, errors = process_net(net, writeln=writeln)
            status = "Error" if errors else "OK"

            writeln('  "warnings": %s,' % json.dumps(warnings))
            writeln('  "errors": %s,' % json.dumps(errors))
            writeln('  "status": "%s"' % status)
            writeln('}')
            writeln()


def process_net(net, writeln):
    writeln('  "net_name": "%s",' % net.name)

    warnings = list(net.warnings)
    errors = list(net.errors)

    if errors:
        return warnings, errors

    if not net.isConnected():
        errors.append("disconnected net")
    elif not net.isStable():
        warnings.append("net has collisions")

        if not net.isLocallyStable():
            errors.append("next-nearest neighbor collisions")
        elif net.isLadder():
            errors.append("ladder net")
        elif net.hasSecondOrderCollisions():
            errors.append("possible ladder net")

    if errors:
        return warnings, errors

    if not net.isMinimal():
        warnings.append("reduced from supercell")

    net = net.minimalImage()

    finder = geometry.SpaceGroupFinder(net.getSpaceGroup())
    writeln('  "spacegroup_name": "%s",' % finder.groupName)
    if finder.extension:
        writeln('  "spacegroup_extension": "%s",' % finder.extension)

    orbit_reps = [list(orb)[0] for orb in net.nodeOrbits()]
    seqs = [coordination_sequence(net, v, 10) for v in orbit_reps]

    writeln('  "net_nodes_unitcell": %s,' % net.numberOfNodes())
    writeln('  "net_nodes_asym": %s,' % len(orbit_reps))

    writeln('  "net_valency": %s,' % json.dumps([s[0] for s in seqs]))
    writeln('  "net_coordination_seqs": [')
    writeln('      %s' % ',\n      '.join(json.dumps(s) for s in seqs))
    writeln('    ],')

    writeln('  "net_systre_key": "%s",' % net.systreKey)

    write_embedding_data(net, 'net_barycentric', finder, False, writeln)
    write_embedding_data(net, 'net_relaxed', finder, True, writeln)

    return warnings, errors


def write_embedding_data(net, prefix, finder, relaxPositions, writeln):
    embedder = pgraphs.embed.Embedder(net, None, False)

    embedder.setRelaxPositions(False)
    embedder.setPasses(0)
    embedder.go(500)
    embedder.normalize()

    embedder.setRelaxPositions(relaxPositions)
    embedder.setPasses(3)
    embedder.go(10000)
    embedder.normalize()

    gram_primitive = embedder.gramMatrix
    to_std = finder.toStd.basis
    gram = to_std.times(gram_primitive).times(to_std.transposed())

    a = math.sqrt(gram.get(0, 0))
    b = math.sqrt(gram.get(1, 1))
    c = math.sqrt(gram.get(2, 2))
    alpha = acosdeg(gram.get(1, 2) / b / c)
    beta = acosdeg(gram.get(0, 2) / a / c)
    gamma = acosdeg(gram.get(0, 1) / a / b)

    writeln('  "%s_unitcell_a": %s,' % (prefix, a))
    writeln('  "%s_unitcell_b": %s,' % (prefix, b))
    writeln('  "%s_unitcell_c": %s,' % (prefix, c))
    writeln('  "%s_unitcell_alpha": %s,' % (prefix, alpha))
    writeln('  "%s_unitcell_beta": %s,' % (prefix, beta))
    writeln('  "%s_unitcell_gamma": %s,' % (prefix, gamma))

    pos = dict(
        (v, point_as_list(p.times(finder.toStd)))
        for v, p in embedder.positions.items()
    )
    orbit_reps = [list(orb)[0] for orb in net.nodeOrbits()]

    writeln('  "%s_atoms": %s,' % (prefix, [pos[v] for v in orbit_reps]))

    return pos, gram


def acosdeg(x):
    return math.acos(x) / math.pi * 180.0


def point_as_list(p):
    return [p.get(i) for i in range(p.dimension)]


def coordination_sequence(net, v, n):
    cs = net.coordinationSequence(v)
    cs.next()

    out = []
    for i in range(n):
        out.append(cs.next())

    return out


if __name__ == "__main__":
    run()
