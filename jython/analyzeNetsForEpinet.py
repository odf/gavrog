#!/bin/env jython
import json
import sys

import org.gavrog


def run():
    def writeln(s=''):
        print s

    for source_path in sys.argv[1:]:
        index_in_source = 0

        for net in org.gavrog.joss.pgraphs.io.Net.iterator(source_path):
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

    minmap = net.minimalImageMap()
    net = minmap.imageGraph
    name_for_node, merged_names = node_name_mapping(minmap)

    finder = org.gavrog.joss.geometry.SpaceGroupFinder(net.getSpaceGroup())
    writeln('  "spacegroup_name": "%s",' % finder.groupName)

    if finder.extension:
        writeln('  "spacegroup_extension": "%s",' % finder.extension)

    return warnings, errors


def node_name_mapping(phi):
    orbit_for_image_node = {}
    for orbit in phi.imageGraph.nodeOrbits():
        orbit = frozenset(orbit)
        for v in orbit:
            orbit_for_image_node[v] = orbit

    name_for_orbit = {}
    name_for_node = {}
    merged_names = []
    merged_names_seen = set()

    for v in phi.sourceGraph.nodes():
        name = phi.sourceGraph.getNodeName(v)
        w = phi.getImage(v)
        orbit = orbit_for_image_node[w]

        if name != name_for_orbit.get(orbit, name):
            pair = (name, name_for_orbit[orbit])
            if pair not in merged_names_seen:
                merged_names.append(pair)
                merged_names_seen.add(pair)
        else:
            name_for_orbit[orbit] = name

        name_for_node[w] = name_for_orbit[orbit]

    return name_for_node, merged_names


if __name__ == "__main__":
    run()
