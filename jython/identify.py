#!/bin/env jython

# ============================================================
#   Global imports
# ============================================================

import sys

from org.gavrog.joss.pgraphs.io import Net
from org.gavrog.joss.pgraphs.io import Archive


# ============================================================
#   Prepare for RCSR and Zeolite Atlas lookup
# ============================================================

def archive_read(archive, path):
    from java.lang import ClassLoader
    from java.io import InputStreamReader, BufferedReader

    # --- make sure this works from within .jar files and such
    stream = ClassLoader.getSystemResourceAsStream(path)
    reader = BufferedReader(InputStreamReader(stream))
    archive.addAll(reader)

# --- create an empty archive
archive = Archive("1.0")

# --- add entries from RCSR and zeolite archive files
archive_read(archive, "org/gavrog/apps/systre/rcsr.arc")
archive_read(archive, "org/gavrog/apps/systre/zeolites.arc")


# ============================================================
#   Main loop: read nets and print their symbols if found
# ============================================================

for G in Net.iterator(sys.argv[1]):
    if not G.isLocallyStable():
        print ">>>unstable<<<"
    else:
        found = archive.get(G.minimalImage().systreKey)
        if found:
            print "%s" % found.name
        else:
            print ">>>unknown<<<"

# ============================================================
#   EOF
# ============================================================
