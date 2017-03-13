#!/bin/env jython

import sys
import java.io
import org.gavrog


def dsymbolFromCyclicAdjacencies(adjs):
    vertexToChamber = {}
    edgeToChamber = {}
    chamberToVertex = {}

    size = 0

    for v in adjs:
        vertexToChamber[v] = size
        for w in adjs[v]:
            if w == v:
                raise RuntimeException("found a loop at vertex %s" % v)
            else:
                edgeToChamber[v, w] = size
                chamberToVertex[size] = v
                chamberToVertex[size + 1] = v
                size += 2

    ds = org.gavrog.joss.dsyms.basic.DynamicDSymbol(2)
    elms = ds.grow(size)

    for v, w in edgeToChamber:
        D = edgeToChamber[v, w]
        E = edgeToChamber[w, v]
        if E is None:
            print ("# WARNING: missing %s in adjacencies for %s" % (v, w))
        ds.redefineOp(0, elms[D], elms[E + 1])

    for v in adjs:
        d = 2 * len(adjs[v])
        D = vertexToChamber[v]
        for i in range(1, d, 2):
            ds.redefineOp(1, elms[D + i], elms[D + (i + 1) % d])

    for D in range(0, size, 2):
        ds.redefineOp(2, elms[D], elms[D + 1])

    for D in range(size):
        ds.redefineV(0, 1, elms[D], 1)
        ds.redefineV(1, 2, elms[D], 1)

    return org.gavrog.joss.dsyms.basic.DSymbol(ds), chamberToVertex


if __name__ == '__main__':
    import re

    text = sys.stdin.read()
    data = [ [ int(s) for s in re.split(r' +', line.strip()) ]
             for line in re.split(r'\n+', text.strip()) ]
    adjs = dict((a[0], a[1:]) for a in data)

    ds, _ = dsymbolFromCyclicAdjacencies(adjs)

    print ds
