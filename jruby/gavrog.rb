module Gavrog
  include Java
  import org.gavrog.joss.dsyms.basic.DSymbol
  import org.gavrog.joss.dsyms.basic.Subsymbol
  import org.gavrog.joss.dsyms.derived.Covers
  import org.gavrog.joss.dsyms.generators.InputIterator
  
  DSFile = InputIterator
  
  def self.included(mod)
    [ org.gavrog.joss.dsyms.basic.DSymbol,
      org.gavrog.joss.dsyms.basic.DSCover,
      org.gavrog.joss.dsyms.basic.DynamicDSymbol,
      org.gavrog.joss.dsyms.basic.Subsymbol
    ].each do |x|
      x.class_eval do
        include DelaneySymbolExtensions
      end
    end
  end

  class Face
    def initialize(ds, elm)
      @ds = ds
      @elm = int(elm)
    end
    
    def degree
      @ds.m(0, 1, @elm)
    end
  end
  
  class Tile
    def initialize(ds, elm)
      @ds = ds
      @elm = int(elm)
    end
    
    def cover
      sub = DSymbol.new(Subsymbol.new(@ds, int([0, 1, 2]), @elm))
      Covers.finiteUniversalCover(sub)
    end
  end
  
  def int(arg)
    if arg.respond_to? :each
      arg.map { |x| int(x) }
    else
      java.lang.Integer.new(arg)
    end
  end

  def run_filter(input, output, message = nil)
    File.open(output, "w") do |file|
      in_count = out_count = 0
      
      DSFile.new(input).each do |ds|
        in_count += 1
        out = yield(ds)
        case out
        when true then
          out_count += 1
          file.puts ds
        when false, nil then
          # do nothing
        else
          out_count += 1
          file.puts out
        end
      end
      
      file.puts "# #{message}" if message
      file.puts "# read #{in_count} and wrote #{out_count} symbols"
    end
  end

  module DelaneySymbolExtensions
    def reps(*args)
      if args.size == 1 && args[0].respond_to?(:each)
        orbit_reps(int(args[0]))
      else
        orbit_reps(int(args))
      end
    end
    
    def subsymbols(idcs)
      reps(idcs).map do |elm|
        DSymbol.new(Subsymbol.new(self, idcs.map, int(elm)))
      end
    end
    
    def faces
      idcs = indices.map
      idcs.delete 2
      reps(idcs).map { |elm| Face.new(self, elm) }
    end
    
    def tiles
      idcs = indices.map
      idcs.delete dim
      reps(idcs).map { |elm| Tile.new(self, elm) }
    end
  end
end

module Enumerable
  def count
    inject(0) { |n, x| n += 1 }
  end
end

class Symbol
  def to_proc
    proc { |obj, *args| obj.send(self, *args) }
  end
end

include Gavrog
