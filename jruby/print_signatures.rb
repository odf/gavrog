#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')
import org.gavrog.joss.dsyms.derived.Signature

puts "ID    <f>    n  signature"
puts

n = 0
DSFile.new(ARGV[0]).each do |ds|
  n += 1
  cover = Covers.pseudoToroidalCover3D(ds)
  nt = nf = 0
  cover.tiles.each do |t|
    nt += 1
    nf += t.cover.faces.count
  end
  favg = nf / Float(nt)
  sig = Signature.ofTiling(cover)
  if favg < 14
    puts "#{"%03d" % n}  #{"%5.2f" % favg} #{"%3d" % nt}  #{sig}"
  end
end

puts
puts "#{n} structures processed."
