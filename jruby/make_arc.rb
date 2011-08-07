#!/bin/env jruby

# Takes a file with structure names and Systre keys and turns it into a Systre
# archive.
#
# Assumes that the name and complete key are on one line that ends in a ')'
# character and that the name does not contain the characters '(', ')' or ','.
#
# Example:
# sqc1 (3, 1, 1, -1, 0, 0, 1, 1, 0, -1, 0, 1, 1, 0, 0, -1)

include Java
import org.gavrog.joss.pgraphs.io.Archive

version = "1.0"

arc = Archive.new version

File.open(ARGV[0]).each do |line|
  if line.strip.length > 0 and line.strip[-1,1] != ")"
    raise "Oops: ---#{line.strip}---"
  end
  fields = line.gsub(/[(), ]+/, ' ').strip.split
  if fields.length > 0
    name = fields[0]
    key = fields[1..-1].join " "
    arc.add Archive::Entry.new(key, version, name)
  end
end

File.open(ARGV[1], 'w') do |f|
  arc.key_set.each do |k|
    f.write(arc.get_by_key(k))
  end
end
