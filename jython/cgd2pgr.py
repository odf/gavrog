#!/bin/env jython

import sys
import java.io

from org.gavrog.joss.pgraphs.io import Net, Output

out = java.io.OutputStreamWriter(sys.stdout)


for G in Net.iterator(sys.argv[1]):
    Output.writePGR(out, G, G.name)
    out.write('\n')
    out.flush()
