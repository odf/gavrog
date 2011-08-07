#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')

run_filter(ARGV[0], ARGV[1], "dualize", &:dual)
