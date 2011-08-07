#!/bin/env jruby

include Java
import org.gavrog.joss.pgraphs.io.NetParser
import org.gavrog.joss.tilings.FaceList

def convert(input, output)
  File.open(output, "w") do |f|
    parser = NetParser.new(input)
    while !parser.at_end
      data = parser.parse_data_block
      if data.type.match(/TILING/i)
        name = data.get_entries_as_string "name"
        begin
          ds = FaceList.new(data).symbol
        rescue Exception => ex
          f.puts "#\@ error \"#{ex.to_s}\" on #{name || "unnamed"}"
        else
          f.puts "#\@ name #{name}" if name
          f.puts ds
        end
        f.flush
      end
    end
  end
end

convert ARGV[0], ARGV[1]
