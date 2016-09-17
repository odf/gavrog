#!/bin/env jython

import sys
import java.io

from org.gavrog.joss.pgraphs.io import Net, Output


for G in Net.iterator(sys.argv[1]):
    print G.name

    if not G.isConnected():
        print "  Error: net '%s' is not connected" % G.name
    elif not G.isLocallyStable():
        print "  Error: net '%s' is not locally stable" % G.name
    else:
        dim = G.dimension
        inv = G.minimalImage().invariant()

        for i in range(1, len(inv), dim + 2):
            print '  %s' % inv[i : i + dim + 2]

    print
