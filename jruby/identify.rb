#!/bin/env jruby

include Java

# ============================================================
#   Imports
# ============================================================

import org.gavrog.joss.pgraphs.io.Net
import org.gavrog.joss.pgraphs.io.Archive

import java.lang.ClassLoader
import java.io.InputStreamReader
import java.io.BufferedReader

# ============================================================
#   Prepare for RCSR and Zeolite Atlas lookup
# ============================================================

def archive_read(archive, path)
    # --- make sure this works from within .jar files and such
    stream = ClassLoader.getSystemResourceAsStream(path)
    reader = BufferedReader.new(InputStreamReader.new(stream))
    archive.add_all reader
end

# --- create an empty archive
archive = Archive.new "1.0"

# --- add entries from RCSR and zeolite archive files
archive_read archive, "org/gavrog/apps/systre/rcsr.arc"
archive_read archive, "org/gavrog/apps/systre/zeolites.arc"


# ============================================================
#   Main loop: read nets and print their symbols if found
# ============================================================

Net.iterator(ARGV[0]).each do |net|
    if not net.locally_stable?
        puts ">>>unstable<<<"
    elsif net.ladder?
    	puts ">>>ladder<<<"
    elsif found = archive.get(net.minimal_image.systre_key)
        puts found.name
    else
        puts ">>>unknown<<<"
    end
end

# ============================================================
#   EOF
# ============================================================
