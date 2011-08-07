#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')

min  = ARGV[0].to_i
max  = ARGV[1].to_i
fin  = ARGV[2]
fout = ARGV[3]

msg = "filter_by_tile_sizes: tile size range #{min}-#{max}"

run_filter(fin, fout, msg) do |ds|
  ds.tiles.all? { |tile| (min..max).include? tile.cover.faces.count }
end
