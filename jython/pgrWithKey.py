#!/bin/env jython

import sys
import java.io

from org.gavrog.joss.pgraphs.io import Net, Output

out = java.io.OutputStreamWriter(sys.stdout)


for G in Net.iterator(sys.argv[1]):
    key = G.minimalImage().systreKey

    tmp = java.io.StringWriter()
    Output.writePGR(tmp, G, G.name)

    if G.isConnected() and G.isLocallyStable():
        s = tmp.toString().replace('\nEND\n', "\n  KEY %s\nEND\n" % key)

    out.write(s)
    out.write('\n')
    out.flush()
