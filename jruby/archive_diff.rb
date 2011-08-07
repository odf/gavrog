#!/bin/env jruby

include Java

import org.gavrog.joss.pgraphs.io.Archive

import java.io.FileReader

arc1 = Archive.new "1.0"
arc2 = Archive.new "1.0"

arc1.add_all(FileReader.new(ARGV[0]))
arc2.add_all(FileReader.new(ARGV[1]))

removed = []
added   = []
renamed = []
changed = []

for key1 in arc1.key_set
  name1 = arc1.get(key1).name
  name2 = (e = arc2.get(key1)) && e.name
  key2  = (e = arc2.get(name1)) && e.key
  if name2.nil?
    removed << name1
  elsif name2 != name1
    renamed << [name1, name2]
  end
  if key2 && key2 != key1
    changed << name1
  end
end

for key in arc2.key_set
  name = arc2.get(key).name
  if arc1.get(key).nil? and arc1.get(name).nil?
    added << name
  end
end

unless removed.size == 0
  puts "Removed:"
  for name in removed.sort
    puts "    #{name}"
  end
end

unless renamed.size == 0
  puts "Renamed:"
  for name1, name2 in renamed.sort
    puts "    #{name1} => #{name2}"
  end
end

unless added.size == 0
  puts "Added:"
  for name in added.sort
    puts "    #{name}"
  end
end

unless changed.size == 0
  puts "Same name for different nets:"
  for name in changed.sort
    puts "    #{name}"
  end
end

