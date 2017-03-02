#!/bin/env jython

import sys
import optparse
import os.path

import java.io
import java.util

import org.gavrog


def isArchive(filename):
    return os.path.splitext(filename)[1] == '.arc';


def run():
    parser = optparse.OptionParser("usage: %prog [OPTIONS] FILE...")

    parser.add_option('-a', '--output-archive-name',
                      dest='outputArchiveName', type='string',
                      help='file name for optional output archive')
    parser.add_option('-b', '--barycentric',
                      dest='relaxPositions',
                      default=True, action='store_false',
                      help='output barycentric instead of relaxed positions')
    parser.add_option('-d', '--duplicate-is-error',
                      dest='duplicateIsError',
                      default=False, action='store_true',
                      help='terminate if a net is encountered twice')
    parser.add_option('-e', '--equal-edge-priority', metavar='N',
                      dest='relaxPasses', type='int', default=3,
                      help='importance of edge lengths over unit cell volume')
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
    parser.add_option('-r', '--prefer-rhombohedral',
                      dest='preferHexagonal',
                      default=True, action='store_false',
                      help='prefer rhombohedral over hexagonal setting')
    parser.add_option('-s', '--relaxation-steps', metavar='N',
                      dest='relaxSteps', type='int', default=10000,
                      help='max. number of iterations when relaxing positions')
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

    (options, args) = parser.parse_args()

    if options.archivesAsInput:
        inputFileNames = args
        archiveFileNames = []
    else:
        archiveFileNames = filter(isArchive, args)
        inputFileNames = filter(lambda s: not isArchive(s), args)

    print "options  = %s" % (options,)
    print "archives = %s" % (archiveFileNames,)
    print "inputs   = %s" % (inputFileNames,)


if __name__ == "__main__":
    java.util.Locale.setDefault(java.util.Locale.US)

    run()
