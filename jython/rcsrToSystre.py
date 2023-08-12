#!/bin/env jython
import java

from org.gavrog.joss.pgraphs.io import NetParser


def run():
    import json
    import sys

    with open(sys.argv[1]) as fp:
        data = json.load(fp)

    for entry in data:
        net = create_net(entry)


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
