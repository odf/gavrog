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

        nodes = [(int(G.getNodeName(v)), v) for v in G.nodes()]
        nodes.sort()

        for name, v in nodes:
            p = pos[v]
            positionAsString = ' '.join([str(p[i]) for i in range(dim)])
            print "  %d %s" % (name, positionAsString)

    print
