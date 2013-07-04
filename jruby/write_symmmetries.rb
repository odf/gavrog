#!/bin/env jruby

include Java

# ============================================================
#   Imports
# ============================================================

import org.gavrog.joss.pgraphs.io.Net


# ============================================================
#   Main loop: read nets and print barycentric embeddings
# ============================================================

Net.iterator(ARGV[0]).each do |net|
  puts "Net #{net.name}"
  if not net.connected?
    puts "  (Not connected. Processing first component only.)"
    net = Net.new(net.connected_components.first.graph, net.name, 'P1')
  end

  if not net.locally_stable?
    puts "  Not locally stable. Giving up."
  elsif net.ladder?
    puts "  Net is a ladder. Giving up."
  else
    puts "  Net has collisions." if not net.stable?

    n = net.number_of_nodes
    net = net.minimal_image

    if net.number_of_nodes < n
      puts "  Minimal image has #{net.number_of_nodes} vs. #{n} nodes."
    end

    puts "  Found #{net.symmetry_operators.size} symmetries."
    puts "  Space group is #{net.space_group.name}."
  end
end

# ============================================================
#   EOF
# ============================================================
