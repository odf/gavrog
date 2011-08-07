#!/bin/env jruby

require 'java'
require File.join(File.dirname(__FILE__), 'gavrog.rb')
Tiling = org.gavrog.joss.tilings.Tiling

def skeleton(ds)
  begin
    Tiling.new(ds).getSkeleton()
  rescue java.lang.IllegalArgumentException => ex
    ex = ex.cause while ex.cause
    raise ex unless ["duplicate edge", "trivial loop"].include? ex.message
    nil
  end
end

def is_convex(net)
  if net
    pos = net.barycentric_placement
    net.nodes.all? { |v|
      net.good_combinations(net.all_incidences(v), pos).any?
    }
  end
end

if ARGV[0] == "-n"
  run_filter(ARGV[1], ARGV[2], "find non-convex") { |ds|
    not is_convex(skeleton(ds))
  }
else
  run_filter(ARGV[0], ARGV[1], "find convex") { |ds|
    is_convex(skeleton(ds))
  }
end
