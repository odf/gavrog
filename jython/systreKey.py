#!/bin/env jython

import sys
import java.io

from org.gavrog.joss.pgraphs.io import Net, Output


for G in Net.iterator(sys.argv[1]):
    print G.name
    dim = G.dimension
    inv = G.invariant()

    for i in range(1, len(inv), dim + 2):
        print '  %s' % inv[i : i + dim + 2]
    print
