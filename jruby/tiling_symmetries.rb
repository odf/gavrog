#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')
import org.gavrog.joss.tilings.Tiling

run_filter(ARGV[0], ARGV[1], "tiling_symmetries") do |ds|
  Tiling.new(ds).getSpaceGroup.getName
end
