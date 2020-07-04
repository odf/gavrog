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

    net = net.minimalImage()

    finder = org.gavrog.joss.geometry.SpaceGroupFinder(net.getSpaceGroup())
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

    return warnings, errors


def coordination_sequence(net, v, n):
    cs = net.coordinationSequence(v)
    cs.next()

    out = []
    for i in range(n):
        out.append(cs.next())

    return out


if __name__ == "__main__":
    run()
