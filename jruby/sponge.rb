#!/usr/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')

import org.gavrog.jane.fpgroups.CosetAction
import org.gavrog.joss.dsyms.basic.DSCover
import org.gavrog.joss.dsyms.derived.FundamentalGroup

a = DSymbol.new "1 3:1,1,1,1:4,3,4"
b = Covers.pseudoToroidalCover3D(a)

f = FundamentalGroup.new(b)
g = f.presentation
puts DSCover.new(f, CosetAction.new(g, g.generators.map { |x| x.raisedTo(2) }))
