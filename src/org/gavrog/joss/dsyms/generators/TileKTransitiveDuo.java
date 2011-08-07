/*
   Copyright 2008 Olaf Delgado-Friedrichs

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
import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.joss.algorithms.CheckpointEvent;
import org.gavrog.joss.algorithms.ResumableGenerator;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

import buoy.event.EventProcessor;

/**
 * Generates all minimal, locally euclidean, tile-k-transitive tilings by a
 * given combinatorial tile.
 * 
 * @author Olaf Delgado
 * @version $Id: TileKTransitiveDuo.java,v 1.1 2008/06/16 08:02:50 odf Exp $
 */
public class TileKTransitiveDuo extends ResumableGenerator<DSymbol> {
    private final boolean verbose;
    private final boolean simple;

    private final Iterator partLists;
    private Iterator extended;
    private Iterator symbols;

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
     * @param tile the tile to use in the tilings.
     * @param k the number of transitivity classes of tiles aimed for.
     * @param verbose if true, some logging information is produced.
     */
    public TileKTransitiveDuo(final DelaneySymbol tileA, final int nrA,
			final DelaneySymbol tileB, final int nrB, final boolean simple,
			final boolean verbose) {
        this.verbose = verbose;
        this.simple = simple;

        final List coversA = Iterators.asList(Covers.allCovers(tileA.minimal()));
        final List coversB = Iterators.asList(Covers.allCovers(tileB.minimal()));
        final Iterator listsA = Iterators.selections(coversA.toArray(), nrA);
        final Iterator listsB = Iterators.selections(coversB.toArray(), nrB);
        this.partLists = Iterators.cantorProduct(listsA, listsB);

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
                    	final Pair item = (Pair) partLists.next();
                        final List tiles = new ArrayList();
                        tiles.addAll((List) item.getFirst());
                        tiles.addAll((List) item.getSecond());
                        if (!partsListOkay(tiles)) {
                        	continue;
                        }
                        final DynamicDSymbol tmp = new DynamicDSymbol(2);
                        for (final Iterator iter = tiles.iterator(); iter
                                .hasNext();) {
                            tmp.append((DSymbol) iter.next());
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
                            System.out.println(setAsString(ds));
                        }
                        extended = extendTo3d(ds);
                        if (extended instanceof ResumableGenerator) {
                        	final ResumableGenerator gen =
                        		(ResumableGenerator) extended;
							gen.addEventLink(CheckpointEvent.class, this,
								"repostCheckpoint");
							if (checkpoint[0] == resume[0] && resume1 != null) {
								gen.setResumePoint(resume1);
							}
						}
                    } else {
                        throw new NoSuchElementException("At end");
                    }
                }
                final DSymbol ds = (DSymbol) extended.next();
                ++checkpoint[1];
                checkpoint[2] = 0;
                postCheckpoint();
                if (tooEarly()) {
                	continue;
                }
                ++this.count3dSets;
                if (this.verbose) {
                    System.out.println("    " + setAsString(ds));
                }
                symbols = defineBranching(ds);
            }
            final DSymbol ds = (DSymbol) symbols.next();
            ++checkpoint[2];
            postCheckpoint();
            if (tooEarly()) {
            	continue;
            }
            ++count3dSymbols;
            if (this.verbose) {
                System.out.println("        " + branchingAsString(ds));
                System.out.flush();
            }
            if (ds.equals(ds.minimal())) {
                ++countMinimal;
                return new DSymbol(ds.canonical());
            }
        }
    }

	private void postCheckpoint() {
		dispatchEvent(new CheckpointEvent(this, tooEarly(), null));
	}

	@SuppressWarnings("unused")
	private void repostCheckpoint(final Object ev) {
		final CheckpointEvent ce = (CheckpointEvent) ev;
		dispatchEvent(new CheckpointEvent(this, ce.isOld(), ce.getMessage()));
	}
	
    /**
     * Retreives the current checkpoint value as a string.
     * 
     * @return the current checkpoint.
     */
    public String getCheckpoint() {
    	if (extended != null && extended instanceof ResumableGenerator) {
			return String.format("%d-[%s]", checkpoint[0],
					((ResumableGenerator) extended).getCheckpoint().replaceAll(
							"-", "."));
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
    protected boolean partsListOkay(final List list) {
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
    protected Iterator defineBranching(final DelaneySymbol ds) {
		if (this.simple) {
			final DynamicDSymbol out = new DynamicDSymbol(new DSymbol(ds));
			final IndexList idx = new IndexList(2, 3);
			for (final Iterator reps = out.orbitReps(idx); reps.hasNext();) {
				final Object D = reps.next();
				final int r = out.r(2, 3, D);
				if (r == 3) {
					out.redefineV(2, 3, D, 1);
				} else if (r == 1) {
					out.redefineV(2, 3, D, 3);
				} else {
					throw new RuntimeException("this should not happen: r = "
							+ r + " at D = " + D);
				}
			}
			if (out.isLocallyEuclidean3D()) {
				return Iterators.singleton(new DSymbol(out));
			} else {
				return Iterators.empty();
			}
		} else {
			return new DefineBranching3d(ds);
		}
	}

    /**
	 * Override this to restrict or change the generation of 3-neighbor
	 * relations.
	 * 
	 * @param ds
	 *            a Delaney symbol.
	 * @return an iterator over all admissible extensions.
	 */
    protected Iterator extendTo3d(final DSymbol ds) {
		if (this.simple) {
			return new CombineTiles(ds) {
				protected List<Move> getExtraDeductions(final DelaneySymbol ds,
						final Move move) {
					final List<Move> out = new ArrayList<Move>();
					final Object D = move.element;
					Object E = D;
					int r = 0;
					List<Object> cuts = new ArrayList<Object>();
					do {
						E = ds.op(2, E);
						if (ds.definesOp(3, E)) {
							E = ds.op(3, E);
						} else {
							cuts.add(E);
						}
						++r;
					} while (E != D);

					switch (cuts.size()) {
					case 0:
						if (r == 2 || r > 3) {
							return null;
						}
						break;
					case 1:
						if (r > 3) {
							return null;
						} else if (r == 3) {
							final Object A = cuts.get(0);
							out.add(new Move(A, A, -1, -1, false, 0));
						}
						break;
					case 2:
						if (r > 6) {
							return null;
						} else if (r == 6) {
							final Object A = cuts.get(0);
							final Object B = cuts.get(1);
							out.add(new Move(A, B, -1, -1, false, 0));
						}
						break;
					default:
						throw new RuntimeException("this should not happen");
					}

					return out;
				}
			};
		} else {
			return new CombineTiles(ds);
		}
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
			  "Usage: java [PATH] [OPTION]... ds1 k1 ds2 k2 [FILE]\n"
			+ "\n"
			+ "where"
			+ "  PATH = org.gavrog.joss.dsyms.generators.TileKTransitiveDuo\n"
			+ "  ds1, ds2: D-symbols for topological tile types 1 and 2"
			+ "  k1, k2:   numbers of tile orbits of types 1 and 2"
			+ "\n"
			+ "Recognized options:\n"
			+ "  -e        skip euclidicity test\n"
			+ "  -i N      interval in seconds between writing checkpoints\n"
			+ "  -r A-B-C  resume generation at a checkpoint\n"
			+ "  -s        generate only simple tilings\n"
			+ "  -v        run in verbose mode\n"
			);
		System.exit(1);
	}
	
    public static void main(final String[] args) {
		try {
			boolean verbose = false;
			boolean check = true;
			boolean simple = false;
			int checkpointInterval = 3600;
			String resume = null;

			int i = 0;
			while (i < args.length && args[i].startsWith("-")) {
				if (args[i].equals("-v")) {
					verbose = !verbose;
				} else if (args[i].equals("-e")) {
					check = !check;
				} else if (args[i].equals("-s")) {
					simple = !simple;
				} else if (args[i].equals("-i")) {
					checkpointInterval = Integer.parseInt(args[++i]);
				} else if (args[i].equals("-r")) {
					resume = args[++i];
				} else {
					usage();
				}
				++i;
			}

			if (args.length < i + 4) {
				usage();
			}

			final DSymbol dsA = new DSymbol(args[i]);
			final int nrA = Integer.parseInt(args[i + 1]);
			final DSymbol dsB = new DSymbol(args[i + 2]);
			final int nrB = Integer.parseInt(args[i + 3]);
			i += 4;

			final Writer output;
			if (args.length > i + 1) {
				output = new FileWriter(args[i + 1]);
			} else {
				output = new OutputStreamWriter(System.out);
			}

			int countGood = 0;
			int countAmbiguous = 0;

			final Stopwatch timer = new Stopwatch();
			final Stopwatch eTestTimer = new Stopwatch();
			timer.start();

			output.write("# Program TileKTransitiveDuo.\n");
			output.write("# Arguments:\n");
			output.write("#     ds1 = " + dsA + "\n");
			output.write("#     k1  = " + nrA + "\n");
			output.write("#     ds2 = " + dsB + "\n");
			output.write("#     k2  = " + nrB + "\n");
			output.write("# Options:\n");
			output.write("#     simple tilings only:             ");
			output.write((simple ? "on" : "off") + "\n");
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
			
			final TileKTransitiveDuo iter = new TileKTransitiveDuo(dsA, nrA,
					dsB, nrB, simple, verbose);
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
