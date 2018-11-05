#!/bin/env jython

import sys
import java.io

from org.gavrog.joss.pgraphs.io import Net, Output

out = java.io.OutputStreamWriter(sys.stdout)


for G in Net.iterator(sys.argv[1]):
    tmp = java.io.StringWriter()
    Output.writePGR(tmp, G, G.name)
    s = tmp.toString()

    if G.isConnected() and G.isLocallyStable():
        key = G.minimalImage().systreKey
        s = s.replace('\nEND\n', "\n  KEY %s\nEND\n" % key)

    out.write(s)
    out.write('\n')
    out.flush()
