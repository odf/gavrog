#!/bin/env jruby

include Java

# ============================================================
#   Imports
# ============================================================

import org.gavrog.box.simple.DataFormatException
import org.gavrog.joss.pgraphs.io.Net
import org.gavrog.joss.pgraphs.io.NetParser
import org.gavrog.joss.pgraphs.io.Archive

import java.lang.ClassLoader
import java.io.InputStreamReader
import java.io.BufferedReader

# ============================================================
#   Prepare for lookup in old RCSR archive
# ============================================================

def archive_read(archive, path)
  # --- make sure this works from within .jar files and such
  stream = ClassLoader.getSystemResourceAsStream(path)
  reader = BufferedReader.new(InputStreamReader.new(stream))
  archive.add_all reader
end

# --- create an empty archive
archive = Archive.new "1.0"

# --- add entries from RCSR archive file
archive_read archive, "org/gavrog/apps/systre/rcsr.arc"


# ============================================================
#   Create a new archive and a corresponding output file
# ============================================================

new_archive = Archive.new "1.0"
arc_file = File.new(ARGV[1], "w")


# ============================================================
#   Main loop: read and process nets
# ============================================================

Net.iterator(ARGV[0]).each do |net|
  message = nil
  name = net.name
  
  if not net.ok?
    message = ">>>#{net.errors.map{ |x| x.message }.join("===")}<<<"
  elsif not net.connected?
    message = ">>>not connected<<<"
  elsif not net.locally_stable?
    message = ">>>unstable<<<"
  elsif net.ladder?
    message = ">>>ladder<<<"
  else
    key = net.minimal_image.systre_key
    if found = new_archive.get(key)
      message = ">>>same key as #{found.name}<<<"
    elsif new_archive.get(name)
      message = ">>>duplicate name<<<"
    else
      if found = archive.get(key)
        unless name == found.name
          message = "in old archive as #{found.name}"
        end
      else
        if archive.getByName(name).nil?
          message = "NEW!!!"
        else
          message = "the old archive has a different net with this name"
        end
      end
      new_archive.add net.minimal_image, name
      arc_file.puts new_archive.get(key)
      arc_file.puts
      arc_file.flush
    end
  end
  
  unless message.nil?
    puts "#{net.name}:\t#{message}"
  end
end


# ============================================================
#   Close the output file
# ============================================================

arc_file.close


# ============================================================
#   EOF
# ============================================================
