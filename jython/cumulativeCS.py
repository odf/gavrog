#!/bin/env jython

import sys
import java.io

from org.gavrog.joss.pgraphs.io import Net

out = java.io.OutputStreamWriter(sys.stdout)


for G in Net.iterator(sys.argv[1]):
    out.write("\n## %s\n" % G.name)
    out.flush()

    cs = G.coordinationSequence(G.nodes().next())

    s = 0
    for k in range(101):
        x = cs.next()
        s += x
        if k > 0:
            out.write("%4d  %8d  %10d  %8.4f\n" % (k, x, s, float(s) / k**3))
        out.flush()
