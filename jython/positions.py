#!/bin/env jython

import sys
import java.io

from org.gavrog.joss.pgraphs.io import Net


for G in Net.iterator(sys.argv[1]):
    print G.name

    if not G.isConnected():
        print "  Error: net '%s' is not connected" % G.name
    else:
        dim = G.dimension
        pos = G.barycentricPlacement()

        for v in G.nodes():
            p = pos[v]
            print "  %s %s" % (v.id(), ' '.join([str(p[i]) for i in range(dim)]))

    print
