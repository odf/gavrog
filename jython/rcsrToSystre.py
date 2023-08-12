#!/bin/env jython
import java

from org.gavrog.joss.pgraphs.io import NetParser


ignored_warnings = [
    "No explicit edges given - using nearest nodes"
]


def run():
    import json
    import sys

    with open(sys.argv[1]) as fp:
        data = json.load(fp)

    for entry in data:
        net = create_net(entry)
        warnings, errors = check_net(net)

        warnings = [w for w in warnings if not w in ignored_warnings]

        if errors or warnings:
            print("\n%4d %s" % (entry['serialNumber'], net.name))
            for err in errors:
                print("  Error: %s" % err)
            for wrn in warnings:
                print("  Warning: %s" % wrn)


def check_net(net_raw):
    warnings = list(net_raw.warnings)
    errors = list(net_raw.errors)

    if errors:
        return warnings, errors

    if not net_raw.isConnected():
        errors.append("disconnected net")
    elif not net_raw.isLocallyStable():
        errors.append("next-nearest neighbor collisions")
    elif not net_raw.isStable():
        warnings.append("net has collisions")

        if net_raw.isLadder():
            warnings.append("ladder net")
        elif net_raw.hasSecondOrderCollisions():
            warnings.append("possible ladder net")

    if errors:
        return warnings, errors

    return warnings, errors


def create_net(entry):
    cgd = as_cgd_string(entry)
    reader = java.io.StringReader(cgd)
    return NetParser(reader).parseNet()


def as_cgd_string(entry):
    lines = []

    lines.append("CRYSTAL")
    lines.append("  NAME %s" % entry['symbol'])
    lines.append(
        "  GROUP %s" %
        entry['spacegroupSymbol'].replace('(', '').replace(')', '')
    )

    cell = entry["cell"]
    lines.append(
        "  CELL %.5f %.5f %.5f %.4f %.4f %.4f" %
        (
            cell["a"], cell["b"], cell["c"],
            cell["alpha"], cell["beta"], cell["gamma"]
        )
    )

    for node in entry["vertices"]:
        name = node["name"]
        cnum = node["coordinationNumber"]
        coords = node["coordinates"]["numerical"]
        lines.append(
            "  NODE %s %d %.5f %.5f %.5f" %
            (name, cnum, coords[0], coords[1], coords[2])
        )

    for edge in entry["edges"]:
        name = edge["name"]
        cnum = edge["coordinationNumber"]
        coords = edge["coordinates"]["numerical"]
        lines.append(
            "  EDGE_CENTER %s %d %.5f %.5f %.5f" %
            (name, cnum, coords[0], coords[1], coords[2])
        )

    lines.append("END")

    return "\n".join(lines)


if __name__ == "__main__":
    run()
