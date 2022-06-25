#!/bin/env jython

import sys

from org.gavrog.joss.dsyms.basic import IndexList
from org.gavrog.joss.dsyms.generators import InputIterator

idcsV = IndexList(1, 2, 3)
idcsE = IndexList(0, 2, 3)
idcsF = IndexList(0, 1, 3)
idcsT = IndexList(0, 1, 2)

for ds in InputIterator(sys.argv[1]):
    if (
        ds.numberOfOrbits(idcsV) == 2 and
        ds.numberOfOrbits(idcsE) == 1 and
        ds.numberOfOrbits(idcsF) == 1 and
        ds.numberOfOrbits(idcsT) == 2
    ):
        print(ds)
