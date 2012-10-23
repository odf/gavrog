/*
   Copyright 2012 Olaf Delgado-Friedrichs

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package org.gavrog.joss.dsyms.generators;

import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.joss.algorithms.CheckpointEvent;
import org.gavrog.joss.algorithms.ResumableGenerator;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

import buoy.event.EventProcessor;

/**
 * Generates all minimal, locally euclidean, tile-k-transitive tilings by a
 * given combinatorial tile or list thereof.
 */
public class TileKTransitive extends ResumableGenerator<DSymbol> {
    private final boolean verbose;

    private final Iterator<List<DSymbol>> partLists;
    private ResumableGenerator<DSymbol> extended;
    private Iterator<DSymbol> symbols;

    private int count2dSymbols = 0;
    private int count3dSets = 0;
    private int count3dSymbols = 0;
    private int countMinimal = 0;
    private int checkpoint[] = new int[] { 0, 0, 0 };
    private int resume[] = new int[] { 0, 0, 0 };
    private String resume1 = null;
    
    /**
     * Constructs an instance.
     * 
     * @param tile the single prototile to use in the tilings.
     * @param k the number of transitivity classes of tiles aimed for.
     * @param verbose if true, some logging information is produced.
     */
    public <T> TileKTransitive(final DelaneySymbol<T> tile, final int k,
            final boolean verbose)
    {
    	this(makeList(tile), k, verbose);
    }

    private static <T> List<DelaneySymbol<T>> makeList(
            final DelaneySymbol<T> tile)
    {
        final List<DelaneySymbol<T>> list = new ArrayList<DelaneySymbol<T>>();
        list.add(tile);
        return list;
    }
    
    
    /**
     * Constructs an instance.
     * 
     * @param tiles the list of prototiles to use in the tilings.
     * @param k the number of transitivity classes of tiles aimed for.
     * @param verbose if true, some logging information is produced.
     */
    public <T> TileKTransitive(
            final List<DelaneySymbol<T>> tiles,
            final int k,
            final boolean verbose)
    {
        this.verbose = verbose;

        final List<DSymbol> covers = new ArrayList<DSymbol>();
        for (final DelaneySymbol<T> t: tiles) {
        	covers.addAll(Iterators.asList(Covers.allCovers(t.minimal())));
        }
        final DSymbol[] a = new DSymbol[covers.size()];
        this.partLists = Iterators.selections(covers.toArray(a), k);

        this.extended = null;
        this.symbols = null;
    }

    /**
     * Sets the point in the search tree at which the algorithm should resume.
     * 
     * @param resume specifies the checkpoint to resume execution at.
     */
    public void setResumePoint(final String spec) {
    	if (spec == null || spec.length() == 0) {
    		return;
    	}
    	final String fields[] = spec.trim().split("-");
    	this.resume[0] = Integer.valueOf(fields[0]);
    	if (fields[1].startsWith("[")) {
    		this.resume1 = fields[1].substring(1, fields[1].length() - 1)
					.replaceAll("\\.", "-");
    	} else {
        	this.resume[1] = Integer.valueOf(fields[2]);
        	this.resume[2] = Integer.valueOf(fields[2]);
    	}
    }

    private boolean tooEarly() {
    	if (checkpoint[0] != resume[0]) {
    		return checkpoint[0] < resume[0];
    	} else if (resume1 == null) {
    		if (checkpoint[1] != resume[1]) {
    			return checkpoint[1] < resume[1];
    		} else {
    			return checkpoint[2] < resume[2];
    		}
    	}
    	return false;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javaDSym.util.IteratorAdapter#findNext()
     */
    protected DSymbol findNext() throws NoSuchElementException {
        while (true) {
            while (symbols == null || !symbols.hasNext()) {
                while (extended == null || !extended.hasNext()) {
                    if (partLists.hasNext()) {
                        final List<DSymbol> tiles = partLists.next();
                        if (!partsListOkay(tiles)) {
                        	continue;
                        }
                        final DynamicDSymbol tmp = new DynamicDSymbol(2);
                        for (final DSymbol ds: tiles) {
                            tmp.append(ds);
                        }
                        ++checkpoint[0];
                        checkpoint[1] = checkpoint[2] = 0;
                        postCheckpoint();
                        if (tooEarly()) {
                        	continue;
                        }
                        final DSymbol ds = new DSymbol(tmp);
                        ++this.count2dSymbols;
                        if (this.verbose) {
                            System.err.println(setAsString(ds));
                        }
                        extended = extendTo3d(ds);
                        extended.addEventLink(CheckpointEvent.class, this,
                                "repostCheckpoint");
                        if (checkpoint[0] == resume[0] && resume1 != null) {
                            extended.setResumePoint(resume1);
                        }
                    } else {
                        throw new NoSuchElementException("At end");
                    }
                }
                final DSymbol ds = extended.next();
                ++checkpoint[1];
                checkpoint[2] = 0;
                postCheckpoint();
                if (tooEarly()) {
                	continue;
                }
                ++this.count3dSets;
                if (this.verbose) {
                    System.err.println("    " + setAsString(ds));
                }
                symbols = defineBranching(ds);
            }
            final DSymbol ds = symbols.next();
            ++checkpoint[2];
            postCheckpoint();
            if (tooEarly()) {
            	continue;
            }
            ++count3dSymbols;
            if (this.verbose) {
                System.err.println("        " + branchingAsString(ds));
                System.err.flush();
            }
            if (ds.equals(ds.minimal())) {
                ++countMinimal;
                return new DSymbol(ds.canonical());
            }
        }
    }

    @SuppressWarnings("unused")
    private void repostCheckpoint(final Object ev) {
        final CheckpointEvent ce = (CheckpointEvent) ev;
        dispatchEvent(new CheckpointEvent(this, ce.isOld(), ce.getMessage()));
    }

    private void postCheckpoint() {
		dispatchEvent(new CheckpointEvent(this, tooEarly(), null));
	}

    /**
     * Retreives the current checkpoint value as a string.
     * 
     * @return the current checkpoint.
     */
    public String getCheckpoint() {
    	if (extended != null && extended instanceof ResumableGenerator) {
			return String.format("%d-[%s]", checkpoint[0],
			        extended.getCheckpoint().replaceAll("-", "."));
		} else {
			return String.format("%s-%s-%s", checkpoint[0], checkpoint[1],
					checkpoint[2]);
		}
	}
    
    /**
     * Override this to restrict the equivariant tile combinations used.
     * 
     * @param list a list of D-symbols encoding tiles.
     * @return true if this combination should be used.
     */
    protected boolean partsListOkay(final List<DSymbol> list) {
        return true;
    }

    /**
     * Override this to restrict or change the generation of branching number
     * combination.
     * 
     * @param ds a Delaney symbol.
     * @return an iterator over all admissible extensions of ds with complete
     *         branching.
     */
    protected Iterator<DSymbol> defineBranching(final DSymbol ds) {
        return new DefineBranching3d(ds);
    }

    /**
     * Override this to restrict or change the generation of 3-neighbor
     * relations.
     * 
     * @param ds a Delaney symbol.
     * @return an iterator over all admissible extensions.
     */
    protected ResumableGenerator<DSymbol> extendTo3d(final DSymbol ds) {
        return new CombineTiles(ds);
    }

    public String statistics() {
        return "Constructed " + count2dSymbols + " spherical symbols, "
                + count3dSets + " partial spatial symbols and "
                + count3dSymbols + " complete spatial symbols, of which "
                + countMinimal + " were minimal.";
    }

    private static String setAsString(final DSymbol ds) {
        final String tmp = ds.toString();
        final int i = tmp.lastIndexOf(':');
        return tmp.substring(0, i);
    }

    private static String branchingAsString(final DSymbol ds) {
        final String tmp = ds.toString();
        final int i = tmp.lastIndexOf(':');
        return tmp.substring(i + 1);
    }

	public static void usage() {
		System.err.print(
			  "Usage: java [PATH] [OPTION]... k ds1 ds2...\n"
			+ "\n"
			+ "where"
			+ "  PATH = org.gavrog.joss.dsyms.generators.TileKTransitive\n"
			+ "  k:          tile-transitivity of generated tilings"
			+ "  ds1 ds2...: D-symbols for topological tile types"
			+ "\n"
			+ "Recognized options:\n"
			+ "  -o [FILE] specifies an output file\n"
			+ "  -e        skip euclidicity test\n"
			+ "  -i N      interval in seconds between writing checkpoints\n"
			+ "  -r A-B-C  resume generation at a checkpoint\n"
			+ "  -v        run in verbose mode\n"
			);
		System.exit(1);
	}
	
    public static void main(final String[] args) {
		try {
			boolean verbose = false;
			boolean check = true;
			int checkpointInterval = 3600;
			String outfile = null;
			String resume = null;

			int i = 0;
			while (i < args.length && args[i].startsWith("-")) {
				if (args[i].equals("-v")) {
					verbose = !verbose;
				} else if (args[i].equals("-o")) {
					outfile = args[++i];
				} else if (args[i].equals("-e")) {
					check = !check;
				} else if (args[i].equals("-i")) {
					checkpointInterval = Integer.parseInt(args[++i]);
				} else if (args[i].equals("-r")) {
					resume = args[++i];
				} else {
					usage();
				}
				++i;
			}

			if (args.length < i + 2) {
				usage();
			}
			final int k = Integer.parseInt(args[i]);
			++i;
			
			final List<DelaneySymbol<Integer>> tiles =
			        new ArrayList<DelaneySymbol<Integer>>();
			while (i < args.length) {
				tiles.add(new DSymbol(args[i]));
				++i;
			}

			final Writer output;
			if (outfile != null) {
				output = new FileWriter(outfile);
			} else {
				output = new OutputStreamWriter(System.out);
			}

			int countGood = 0;
			int countAmbiguous = 0;

			final Stopwatch timer = new Stopwatch();
			final Stopwatch eTestTimer = new Stopwatch();
			timer.start();

			output.write("# Program TileKTransitive with k = " + k + ".\n");
			output.write("# Tiles:\n");
			for (final DelaneySymbol<Integer> t: tiles) {
				output.write("#     " + t + "\n");
			}
			output.write("# Output:\n");
			if (outfile != null) {
				output.write("#     " + outfile + "\n");
			} else {
				output.write("#     (stdout)\n");
			}
			output.write("# Options:\n");
			output.write("#     euclidicity test:                ");
			output.write((check ? "on" : "off") + "\n");
			output.write("#     verbose mode:                    ");
			output.write((verbose ? "on" : "off") + "\n");
			output.write("#     checkpoint interval:             ");
			output.write(checkpointInterval + "sec\n");
			if (resume != null) {
				output.write(String.format("# Resuming at %s.\n", resume));
			}
			output.write("\n");
			output.flush();
			
			final TileKTransitive iter = new TileKTransitive(tiles, k, verbose);
			final Stopwatch chkptTimer = new Stopwatch();
			final int interval = 1000 * checkpointInterval;
	        iter.addEventLink(CheckpointEvent.class, new EventProcessor() {
				@Override
				public void handleEvent(final Object event) {
                    final CheckpointEvent ce = (CheckpointEvent) event;
					if (ce.getMessage() != null
							|| chkptTimer.elapsed() > interval) {
						chkptTimer.reset();
						try {
							output.write(ce + "\n");
							output.flush();
						} catch (Throwable ex) {
						}
					}
				}
			});
	        chkptTimer.start();
			if (resume != null) {
				iter.setResumePoint(resume);
			}

			for (final DSymbol out : iter) {
				if (check) {
					eTestTimer.start();
					EuclidicityTester tester = new EuclidicityTester(out);
					final boolean bad = tester.isBad();
					final boolean ambiguous = tester.isAmbiguous();
					eTestTimer.stop();
					if (!bad) {
						if (ambiguous) {
							output.write("#@ name euclidicity dubious\n");
							++countAmbiguous;
						}
						output.write(out + "\n");
						++countGood;
					}
				} else {
					output.write(out + "\n");
				}
				output.flush();
			}
			timer.stop();

			output.write("\n");
			output.write("# Total execution time in user mode was "
					+ timer.format() + ".\n");
			output.write("#   Time for generating 3d sets was "
					+ Stopwatch.global("CombineTiles.total").format() + ".\n");
			output.write("#     Time for computing signatures was "
					+ Stopwatch.global("CombineTiles.signatures").format()
					+ ".\n");
			if (check) {
				output.write("# Time for euclidicity tests was "
						+ eTestTimer.format() + ".\n");
			}
			output.write("\n");
			output.write("# " + iter.statistics() + "\n");
			if (check) {
				output.write("# Of those, " + countGood
						+ " were found euclidean.\n");
				if (countAmbiguous > 0) {
					output.write("# For " + countAmbiguous
							+ " symbols, euclidicity remains undetermined.\n");
				}
			}
			output.flush();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
