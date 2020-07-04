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

    return warnings, errors


if __name__ == "__main__":
    run()
