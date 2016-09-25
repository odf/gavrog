#!/bin/env jython

# ============================================================
#   Imports
# ============================================================

# --- Jython stuff
import sys

# --- Java stuff
from java.lang import ClassLoader
from java.io import InputStreamReader, BufferedReader

# --- Gavrog stuff
from org.gavrog.joss.geometry import SpaceGroupFinder, CrystalSystem
from org.gavrog.joss.pgraphs.io import Net
from org.gavrog.joss.pgraphs.io import Archive


# ============================================================
#   Prepare for RCSR lookup
# ============================================================

# --- get RCSR archive file (possibly from a .jar or the web)
rcsr_path = "org/gavrog/apps/systre/rcsr.arc"
rcsr_stream = ClassLoader.getSystemResourceAsStream(rcsr_path)
reader = BufferedReader(InputStreamReader(rcsr_stream))

# --- create an archive object from it
archive = Archive("1.0")
archive.addAll(reader)


# ============================================================
#   Helper function
# ============================================================

def minimalImageWithNodeNames(net):
    if net.isMinimal():
        return net, dict((v, [str(net.getNodeName(v))]) for v in net.nodes())
    else:
        phi = net.minimalImageMap()
        imageGraph = phi.getImageGraph()

        names = {}
        for v in net.nodes():
            names.setdefault(phi.getImage(v), set()).add(net.getNodeName(v))

        nodeToNames = {}
        for v in imageGraph.nodes():
            nodeToNames[v] = sorted(map(str, names[v]))

        return imageGraph, nodeToNames


# ============================================================
#   Main data processing
# ============================================================


# --- dictionary of seen nets
seen = {}

# --- count the nets that we read
count = 0

# --- main loop
for G0 in Net.iterator(sys.argv[1]):
    count += 1

    # --- retrieve the net's name or make one up
    name = G0.name
    if name is None:
        name = "unamed-%03d" % count
    print "Net %03d (%s): " % (count, name),

    errors = [err.message for err in G0.errors]
    if errors:
        print '\tcontains errors'
        for err in errors:
            print "\t%s" % err
        print
        continue

    # --- IMPORTANT: use the minimal ideal translation unit
    G, nodeToNames = minimalImageWithNodeNames(G0)

    # --- compute the Systre key and check if we've seen it
    key = G.systreKey
    old = seen.get(key)
    if old:
        # --- skip further output for duplicates
        print "\tduplicates %s" % old
        print
        continue
    else:
        print "\tnew entry"
    seen[key] = name

    # --- print node names, possibly composites from original names
    print "\tNodes:"
    for v in G.nodes():
        print "\t\t%s %s" % (v.id(), " ".join(nodeToNames[v]))

    # --- print coordination sequences for the unique nodes
    print "\tCS:",
    n = 0
    for orb in G.nodeOrbits():
        for v in orb:
            i = 0
            for x in G.coordinationSequence(v):
                if i == 0:
                    if n == 0:
                        print "\t%d" % x,
                    else:
                        print "\t\t%d" % x,
                else:
                    print x,
                i += 1
                if i > 16:
                    break
            break
        print
        n += 1

    # --- print the name of the space group
    print "\tGroup:",
    spacegroup = G.spaceGroup
    print "\t%s" % spacegroup.name

    # --- look net up in RCSR and print symbol, if found
    print "\tRCSR:",
    found = archive.get(key)
    if found:
        print "\t%s" % found.name
    else:
        print "\tnot found"

    # --- print out the barycentric positions
    print "\tPositions:"
    pos = G.barycentricPlacement()
    for v in G.nodes():
        print "\t\t%s" % v.id(),
        p = pos[v]
        for i in range(p.dimension):
            print "%9.5f" % p[i],
        print

    print "\tEdges:"
    for e in G.edges():
        v = e.source()
        w = e.target()
        s = G.getShift(e)
        print "\t\t%s %s " % (v.id(), w.id()),
        for i in range(s.dimension):
            print "%2s" % s[i],
        print

    # --- print out the minimal graph in canonical form (the Systre key)
    print "\tCode:",
    print "\t%s" % key

    # --- print out the graph expressed in terms of a conventional unit cell
    cover = G.conventionalCellCover()
    cover_pos = cover.barycentricPlacement()
    finder = SpaceGroupFinder(spacegroup)
    system = finder.crystalSystem
    special_angle = ((system == CrystalSystem.TRIGONAL)
                    or (system == CrystalSystem.HEXAGONAL_2D)
                    or (system == CrystalSystem.HEXAGONAL_3D))
    print "\tUnit cell parameters:"
    print "\t\t1 1 1 90 90",
    if special_angle:
        print "120"
    else:
        print "90"
    print "\tUnit cell positions:"
    for v in cover.nodes():
        print "\t\t%d" % v.id(),
        p = cover_pos[v]
        for i in range(p.dimension):
            print "%9.5f" % p[i],
        print
    print "\tUnit cell edges:"
    for e in cover.edges():
        v = e.source()
        w = e.target()
        s = cover.getShift(e)
        print "\t\t%s %s " % (v.id(), w.id()),
        for i in range(s.dimension):
            print "%2s" % s[i],
        print

    # --- simple consistency tests for the conventional cell version
    assert G == cover.minimalImage()
    assert cover.isBarycentric(cover_pos)

    # --- don't crowd
    print

# ============================================================
#   EOF
# ============================================================
