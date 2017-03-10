#!/bin/env jython

import optparse
import os.path
import sys

import java.io
import java.util

import org.gavrog


class OrderedDict:
    def __init__(self):
        self.__dict = {}
        self.__keys = []

    def __iter__(self):
        return self.__keys.__iter__()

    def __getitem__(self, key):
        return self.__dict[key]

    def __setitem__(self, key, value):
        if not self.__dict.has_key(key):
            self.__keys.append(key)
        self.__dict[key] = value


def pluralize(n, s):
    return "%d %s%s" % (n, s, "s" if n > 1 else "")


def isArchiveFileName(filename):
    return os.path.splitext(filename)[1] == '.arc'


def makeArchive(reader=None):
    archive = org.gavrog.joss.pgraphs.io.Archive('1.0')
    if reader:
        archive.addAll(reader)
    return archive


def readBuiltinArchive(name):
    loader = java.lang.ClassLoader.getSystemClassLoader()
    rcsrPath = "org/gavrog/apps/systre/%s" % name
    rcsrStream = loader.getResourceAsStream(rcsrPath)

    return makeArchive(java.io.InputStreamReader(rcsrStream))


def readArchiveFromFile(fname):
    return makeArchive(java.io.FileReader(fname))


def prefixedLineWriter(prefix=''):
    def write(s=''):
        print "%s%s" % (prefix, s)

    return write


def reportSystreError(errorType, message, writeInfo):
    writeInfo("!!! ERROR (%s) - %s." % (errorType, message))


def nodeNameMapping(phi):
    imageNode2Orbit = {}
    for orbit in phi.imageGraph.nodeOrbits():
        for v in orbit:
            imageNode2Orbit[v] = orbit

    orbit2name = {}
    node2name = {}
    mergedNames = []
    mergedNamesSeen = set()

    for v in phi.sourceGraph.nodes():
        name = phi.sourceGraph.getNodeName(v)
        w = phi.getImage(v)
        orbit = imageNode2Orbit[w]

        if name != orbit2name.get(orbit, name):
            pair = (name, orbit2name[orbit])
            if pair not in mergedNamesSeen:
                mergedNames.append(pair)
                mergedNamesSeen.add(pair)
        else:
            orbit2name[orbit] = name

        node2name[w] = orbit2name[orbit]

    return node2name, mergedNames


def checkGraph(graph, writeInfo):
    if not graph.isLocallyStable():
        msg = ("Structure has collisions between next-nearest neighbors."
               + " Systre does not currently support such structures.")
        reportSystreError("STRUCTURE", msg, writeInfo)
        return False

    if graph.isLadder():
        msg = "Structure is non-crystallographic (a 'ladder')"
        reportSystreError("STRUCTURE", msg, writeInfo)
        return False

    if graph.hasSecondOrderCollisions():
        msg = ("Structure has second-order collisions."
               + " Systre does not currently support such structures.")
        reportSystreError("STRUCTURE", msg, writeInfo)
        return False

    if not graph.isStable():
        writeInfo("Structure has collisions.")
        writeInfo()

    return True


def showCoordinationSequences(G, nodeToName, writeInfo):
    writeInfo("   Coordination sequences:")

    cum = 0
    complete = True

    for orbit in G.nodeOrbits():
        v = orbit.iterator().next()
        cs = G.coordinationSequence(v)
        cs.next()
        s = 1
        out = ["      Node %s:   " % nodeToName[v]]

        for i in range(10):
            if s > 100000:
                complete = False
                out.append('...')
                break

            x = cs.next()
            out.append(x)
            s += x

        cum += orbit.size() * s

        writeInfo(' '.join(map(str, out)))

    writeInfo()

    if complete:
        td10 = cum / G.numberOfNodes()
        writeInfo("   TD10 = %s" % int(td10 + 0.5))
    else:
        writeInfo("   TD10 not computed.")

    writeInfo()


def showGraphBasics(graph, writeInfo):
    writeInfo("   Input structure described as %d-periodic." % graph.dimension)
    writeInfo("   Given space group is %s." % graph.givenGroup)
    writeInfo("   %s and %s in repeat unit as given."
              % (pluralize(graph.numberOfNodes(), "node"),
                 pluralize(graph.numberOfEdges(), "edge")))
    writeInfo()


def showPointSymbols(G, nodeToName, writeInfo):
    writeInfo("   Wells point symbols:")
    for orbit in G.nodeOrbits():
        v = orbit.iterator().next()
        writeInfo("      Node %s:   %s" % (nodeToName[v], G.pointSymbol(v)))
    writeInfo()


def showSpaceGroup(givenGroup, finder, writeInfo):
    writeInfo("   Ideal space group is %s." % finder.groupName)

    givenName = org.gavrog.joss.geometry.SpaceGroupCatalogue.normalizedName(
        givenGroup)
    if finder.groupName != givenName:
        writeInfo("   Ideal group or setting differs from given (%s vs %s)."
                  % (finder.groupName, givenName))

    if finder.extension == '1':
        writeInfo("     (using first origin choice)")
    elif finder.extension == '2':
        writeInfo("     (using second origin choice)")
    elif finder.extension == 'H':
        writeInfo("     (using hexagonal setting)")
    elif finder.extension == 'R':
        writeInfo("     (using rhombohedral setting)")

    writeInfo()


def showAndCountGraphMatches(invariant, archives, writeInfo):
    count = 0

    for name in archives:
        arc = archives[name]
        found = arc.getByKey(invariant)
        if found:
            count += 1
            if name == '__rcsr__':
                writeInfo("   Structure was identified with RCSR symbol:")
            elif name == '__internal__':
                writeInfo("   Structure already seen in this run.")
            else:
                writeInfo("   Structure was found in archive \"%s\":" % name)

            writeInfo("       Name:            %s" % found.name)
            if found.description:
                writeInfo("       Description:     %s" % found.description)
            if found.reference:
                writeInfo("       Reference:       %s" % found.reference)
            if found.getURL():
                writeInfo("       URL:             %s" % found.getURL())

            writeInfo()

    return count


def embedAndShowGraph(
    graph,
    name,
    options,
    writeInfo=prefixedLineWriter(),
    writeData=prefixedLineWriter()):

    #TODO implement this
    pass


def processDisconnectedGraph(
    graph,
    name,
    options,
    writeInfo=prefixedLineWriter(),
    writeData=prefixedLineWriter(),
    archives=OrderedDict(),
    outputArchiveFp=None):

    writeInfo("   Structure is not connected.")
    writeInfo("   Processing components separately.")
    writeInfo()

    #TODO implement this


def processGraph(
    graph,
    name,
    options,
    writeInfo=prefixedLineWriter(),
    writeData=prefixedLineWriter(),
    archives=OrderedDict(),
    outputArchiveFp=None):

    showGraphBasics(graph, writeInfo)

    if not graph.isConnected():
        return processDisconnecteGraph(
            G,
            name,
            options,
            writeInfo=writeInfo,
            writeData=writeData,
            archives=archives,
            outputArchiveFp=outputArchiveFp)

    if not checkGraph(graph, writeInfo):
        return

    M = graph.minimalImageMap()
    G = M.imageGraph

    if G.numberOfEdges() < graph.numberOfEdges():
        writeInfo("   Ideal repeat unit smaller than given (%d vs %d edges)."
                  % (G.numberOfEdges(), m))
    else:
        writeInfo("   Given repeat unit is accurate.")

    ops = G.symmetryOperators()
    writeInfo("   Point group has %d elements." % len(ops))
    writeInfo("   %s of node." % pluralize(len(list(G.nodeOrbits())), "kind"))
    writeInfo()

    nodeToName, mergedNames = nodeNameMapping(M)
    writeInfo("   Equivalences for non-unique nodes:")
    for old, new in mergedNames:
        writeInfo("      %s --> %s" % (old, new))
    writeInfo()

    showCoordinationSequences(G, nodeToName, writeInfo)

    if options.computePointSymbols and G.dimension >= 3:
        showPointSymbols(G, nodeToName, writeInfo)

    finder = org.gavrog.joss.geometry.SpaceGroupFinder(
        org.gavrog.joss.geometry.SpaceGroup(graph.dimension, ops))
    showSpaceGroup(graph.givenGroup, finder, writeInfo)

    invariant = G.systreKey
    if options.outputSystreKey:
        writeInfo("   Systre key: \"%s\"" % invariant)
        writeInfo()

    countMatches = showAndCountGraphMatches(invariant, archives, writeInfo)
    if countMatches == 0:
        writeInfo("   Structure is new for this run.")
        writeInfo()
        entry = org.gavrog.joss.pgraphs.io.Archive.Entry(
            invariant, G.invariantVersion, name)
        archives['__internal__'].add(entry)

        if outputArchiveFp:
            outputArchiveFp.write(entry.toString() + '\n')

    embedAndShowGraph(
        graph, name, options, writeInfo=writeInfo, writeData=writeData)


def processDataFile(
    fname,
    options,
    writeInfo=prefixedLineWriter(),
    writeData=prefixedLineWriter(),
    archives=OrderedDict(),
    outputArchiveFp=None):

    count = 0
    fileBaseName = os.path.splitext(os.path.basename(fname))[0]

    writeInfo("Data file %s" % fname)

    for G in org.gavrog.joss.pgraphs.io.Net.iterator(fname):
        writeData()
        if count:
            writeData()
            writeData()

        count += 1

        name = G.name or "%s-#%03d" % (fileBaseName, count)

        writeInfo("Structure #%d - %s" % (count, name))
        writeData()

        hasWarnings = False
        for text in G.warnings:
            writeInfo("   (%s)" % text)
            hasWarnings = True

        if hasWarnings:
            writeData()

        hasErrors = False
        for err in G.errors:
            writeInfo("!!! ERROR (INPUT) - %s" % err.message)
            hasErrors = True

        if hasErrors:
            continue

        processGraph(
            G,
            name,
            options,
            writeInfo=writeInfo,
            writeData=writeData,
            archives=archives,
            outputArchiveFp=outputArchiveFp)

        writeInfo("Finished structure #%d - %s" % (count, name))

    writeData()
    writeInfo("Finished data file \"%s\"." % fname)


def parseOptions():
    parser = optparse.OptionParser("usage: %prog [OPTIONS] FILE...")

    parser.add_option('-a', '--output-archive-name', metavar='FILE',
                      dest='outputArchiveName', type='string',
                      help='file name for optional output archive')
    parser.add_option('-b', '--barycentric',
                      dest='relaxPositions',
                      default=True, action='store_false',
                      help='output barycentric instead of relaxed positions')
    parser.add_option('-c', '--output-format-cgd',
                      dest='outputFormatCGD',
                      default=False, action='store_true',
                      help='produce .cgd file format instead of default output')
    parser.add_option('-e', '--equal-edge-priority', metavar='N',
                      dest='relaxPasses', type='int', default=3,
                      help='equal edge lengths priority (default 3)')
    parser.add_option('-f', '--prefer-first-origin',
                      dest='preferSecondOrigin',
                      default=True, action='store_false',
                      help='prefer first over second origin')
    parser.add_option('-k', '--output-systre-key',
                      dest='outputSystreKey',
                      default=False, action='store_true',
                      help='include the Systre key in the output')
    parser.add_option('-n', '--no-builtin',
                      dest='useBuiltinArchive',
                      default=True, action='store_false',
                      help='do not use the builtin Systre archive')
    parser.add_option('-o', '--use-original-embedding',
                      dest='useOriginalEmbedding',
                      default=False, action='store_true',
                      help='start relaxation from original positions')
    parser.add_option('-p', '--point-symbols',
                      dest='computePointSymbols',
                      default=False, action='store_true',
                      help='compute Wells point symbols')
    parser.add_option('-r', '--prefer-rhombohedral',
                      dest='preferHexagonal',
                      default=True, action='store_false',
                      help='prefer rhombohedral over hexagonal setting')
    parser.add_option('-s', '--relaxation-steps', metavar='N',
                      dest='relaxSteps', type='int', default=10000,
                      help='iterations in net relaxation (default 10000)')
    parser.add_option('-t', '--skip-embedding',
                      dest='computeEmbedding',
                      default=True, action='store_false',
                      help='do not compute embeddings for nets')
    parser.add_option('-u', '--full-unit-cell',
                      dest='outputFullCell',
                      default=False, action='store_true',
                      help='output full unit cell, not just repeat unit')
    parser.add_option('-x', '--archives-as-input',
                      dest='archivesAsInput',
                      default=False, action='store_true',
                      help='process archive files as normal net input')

    return parser.parse_args()


def run():
    options, args = parseOptions()

    if options.archivesAsInput:
        inputFileNames = args
        archiveFileNames = []
    else:
        archiveFileNames = filter(isArchiveFileName, args)
        inputFileNames = filter(lambda s: not isArchiveFileName(s), args)

    archives = OrderedDict()
    if options.useBuiltinArchive:
        archives['__rcsr__'] = readBuiltinArchive('rcsr.arc')
    for fname in archiveFileNames:
        archives[fname] = readArchiveFromFile(fname)
    archives['__internal__'] = makeArchive()

    arcFp = options.outputArchiveName and file(options.outputArchiveName, 'wb')

    infoPrefix = '## ' if options.outputFormatCGD else ''

    try:
        for name in inputFileNames:
            processDataFile(
                name,
                options,
                writeInfo=prefixedLineWriter(infoPrefix),
                writeData=prefixedLineWriter(),
                archives=archives,
                outputArchiveFp=arcFp)
    finally:
        if arcFp:
            arcFp.flush()
            arcFp.close()


if __name__ == "__main__":
    java.util.Locale.setDefault(java.util.Locale.US)

    run()
