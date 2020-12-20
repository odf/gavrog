#!/bin/env jython
import java
import json
import math
import sys
import traceback

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

            try:
                warnings, errors = process_net(net, writeln=writeln)
            except:
                warnings = []
                errors = ["exception thrown while processing"]
                stacktrace = traceback.format_exc()
                writeln('  "stack_trace": %s,' % json.dumps(stacktrace))

            status = "Error" if errors else "OK"

            writeln('  "warnings": %s,' % json.dumps(warnings))
            writeln('  "errors": %s,' % json.dumps(errors))
            writeln('  "status": "%s"' % status)
            writeln('}')
            writeln()


def process_net(netRaw, writeln):
    writeln('  "net_name": "%s",' % netRaw.name)

    warnings = list(netRaw.warnings)
    errors = list(netRaw.errors)

    if errors:
        return warnings, errors

    if not netRaw.isConnected():
        errors.append("disconnected net")
    elif not netRaw.isLocallyStable():
        errors.append("next-nearest neighbor collisions")
    elif not netRaw.isStable():
        warnings.append("net has collisions")

        if netRaw.isLadder():
            warnings.append("ladder net")
        elif netRaw.hasSecondOrderCollisions():
            warnings.append("possible ladder net")

    if errors:
        return warnings, errors

    if not netRaw.isMinimal():
        warnings.append("reduced from supercell")

    nodeToName, mergedNames = node_name_mapping(netRaw.minimalImageMap())
    net = netRaw.minimalImage()

    finder = geometry.SpaceGroupFinder(net.getSpaceGroup())
    writeln('  "spacegroup_name": "%s",' % finder.groupName)
    if finder.extension:
        writeln('  "spacegroup_extension": "%s",' % finder.extension)

    orbit_reps = [list(orb)[0] for orb in net.nodeOrbits()]
    seqs = [coordination_sequence(net, v, 10) for v in orbit_reps]
    psyms = [net.pointSymbol(v) for v in orbit_reps]

    writeln('  "net_nodes_unitcell": %s,' % net.numberOfNodes())
    writeln('  "net_nodes_asym": %s,' % len(orbit_reps))

    writeln('  "net_valency": %s,' % json.dumps([s[0] for s in seqs]))
    writeln('  "net_coordination_seqs": [')
    writeln('      %s' % ',\n      '.join(json.dumps(s) for s in seqs))
    writeln('    ],')
    writeln('  "net_wells_point_symbols": %s,' % json.dumps(psyms))

    writeln('  "net_systre_key": "%s",' % net.systreKey)

    for kind in ['barycentric', 'maximal', 'relaxed']:
        e = make_embedding(net, kind)

        if kind == 'barycentric':
            dof = e.degreesOfFreedom()
            writeln('  "net_degrees_of_freedom": %s,' % dof)

        if not is_positive_definite(e.gramMatrix):
            warnings.append("%s embedding has collapsed unit cell" % kind)

        pnet = pgraphs.embed.ProcessedNet(net, 'X', nodeToName, finder, e)
        write_embedding(pnet, "net_%s" % kind, writeln)

    return warnings, errors


def node_name_mapping(phi):
    imageNode2Orbit = {}
    for orbit in phi.imageGraph.nodeOrbits():
        orbit = frozenset(orbit)
        for v in orbit:
            imageNode2Orbit[v] = orbit

    orbit2name = {}
    node2name = {}
    mergedNames = []
    mergedNamesSeen = set()

    for v in phi.sourceGraph.nodes():
        name = phi.sourceGraph.getNodeName(v)
        w = phi.getImage(v)
        orbit = imageNode2Orbit[w]

        if name != orbit2name.get(orbit, name):
            pair = (name, orbit2name[orbit])
            if pair not in mergedNamesSeen:
                mergedNames.append(pair)
                mergedNamesSeen.add(pair)
        else:
            orbit2name[orbit] = name

        node2name[w] = orbit2name[orbit]

    return node2name, mergedNames


def make_embedding(graph, kind):
    embedder = pgraphs.embed.Embedder(graph, None, False)

    if kind == 'maximal':
        embedder.setMaximizeVolume(True)
        embedder.setRelaxPositions(False)
        embedder.setPasses(0)
        embedder.go(10000)
        embedder.normalize()
    else:
        embedder.setMaximizeVolume(False)
        embedder.setRelaxPositions(False)
        embedder.setPasses(0)
        embedder.go(500)
        embedder.normalize()

        embedder.setMaximizeVolume(False)
        embedder.setRelaxPositions(kind == 'relaxed')
        embedder.setPasses(3)
        embedder.go(10000)
        embedder.normalize()

    return embedder


def write_embedding(net, prefix, writeln):
    dim = net.graph.dimension
    cgd = serialized_net(net, asCGD=True)

    nodes = []
    edges = []

    for line in cgd.split('\n'):
        fields = line.strip().split()
        if len(fields) == 0:
            continue

        if fields[0] == 'CELL':
            if dim == 3:
                a, b, c, alpha, beta, gamma = map(float, fields[1:])
                writeln('  "%s_unitcell_a": %s,' % (prefix, a))
                writeln('  "%s_unitcell_b": %s,' % (prefix, b))
                writeln('  "%s_unitcell_c": %s,' % (prefix, c))
                writeln('  "%s_unitcell_alpha": %s,' % (prefix, alpha))
                writeln('  "%s_unitcell_beta": %s,' % (prefix, beta))
                writeln('  "%s_unitcell_gamma": %s,' % (prefix, gamma))
            elif dim == 2:
                a, b, gamma = map(float, fields[1:])
                writeln('  "%s_unitcell_a": %s,' % (prefix, a))
                writeln('  "%s_unitcell_b": %s,' % (prefix, b))
                writeln('  "%s_unitcell_gamma": %s,' % (prefix, gamma))
        elif fields[0] == 'CELL_VOLUME':
            volume = float(fields[1])
            writeln('  "%s_unitcell_volume": %s,' % (prefix, volume))
        elif fields[0] == 'NODE':
            nodes.append(map(float, fields[3:]))
        elif fields[0] == 'EDGE':
            edges.append(map(float, fields[1:]))
        elif fields[0] == '#' and len(fields) > 1:
            if fields[1] == 'EDGE_MIN':
                minEdge = float(fields[2])
                writeln('  "%s_shortest_edge_length": %s,' % (prefix, minEdge))
            elif fields[1] == 'EDGE_AVG':
                avgEdge = float(fields[2])
                writeln('  "%s_average_edge_length": %s,' % (prefix, avgEdge))
            elif fields[1] == 'EDGE_MAX':
                maxEdge = float(fields[2])
                writeln('  "%s_longest_edge_length": %s,' % (prefix, maxEdge))
            if fields[1] == 'ANGLE_MIN':
                minAngle = float(fields[2])
                writeln('  "%s_smallest_angle": %s,' % (prefix, minAngle))
            elif fields[1] == 'ANGLE_AVG':
                avgAngle = float(fields[2])
                writeln('  "%s_average_angle": %s,' % (prefix, avgAngle))
            elif fields[1] == 'ANGLE_MAX':
                maxAngle = float(fields[2])
                writeln('  "%s_largest_angle": %s,' % (prefix, maxAngle))
            elif fields[1] == 'SMALLEST_SEPARATION':
                sep = float(fields[2])
                writeln('  "%s_smallest_atom_separation": %s,' % (prefix, sep))

    writeln('  "%s_atoms": %s,' % (prefix, nodes))
    writeln('  "%s_edges": %s,' % (prefix, edges))


def serialized_net(net, asCGD=False, writeFullCell=False, prefix=''):
    stringWriter = java.io.StringWriter()
    writer = java.io.PrintWriter(stringWriter)
    net.writeEmbedding(writer, asCGD, writeFullCell, prefix)
    return stringWriter.toString()


def is_positive_definite(gram):
    if gram.get(0, 0).doubleValue() < 0.001:
        return False
    elif gram.getSubMatrix(0, 0, 2, 2).determinant().doubleValue() < 0.001:
        return False
    elif gram.determinant().doubleValue() < 0.001:
        return False
    else:
        return True


def coordination_sequence(net, v, n):
    cs = net.coordinationSequence(v)
    cs.next()

    out = []
    for i in range(n):
        out.append(cs.next())

    return out


if __name__ == "__main__":
    run()
