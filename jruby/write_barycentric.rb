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
  pos = net.barycentricPlacement
  for v in net.nodes
    puts v, pos[v]
  end
end

# ============================================================
#   EOF
# ============================================================
