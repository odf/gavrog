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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.joss.algorithms.CheckpointEvent;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

/**
 */
public class Kelvin extends FrankKasperExtended {
	final static private IndexList iTiles = new IndexList(0, 1, 2);
	final static private IndexList iFaces2d = new IndexList(0, 1);
	
	final private Writer output;
	final private boolean countOnly;
	final private int start;
	final private int stop;
	private int count = 0;
	final private Stopwatch timer = new Stopwatch();
	final private Set<DSymbol> goodVertexFigures = new HashSet<DSymbol>();
	final private Set<DSymbol> badVertexFigures = new HashSet<DSymbol>();
	final private int interval;
	
	public Kelvin(final int k,
			         final boolean verbose,
			         final boolean testParts,
			         final boolean countOnly,
			         final int start, final int stop,
			         final int checkpointInterval,
			         final Writer output) {
		super(k, verbose, testParts);
		this.countOnly = countOnly;
		this.start = start;
		this.stop = stop;
		this.interval = checkpointInterval * 1000;
		this.output = output;
		this.timer.start();
		
		this.addEventLink(CheckpointEvent.class, this);
	}
	
	protected boolean partsListOkay(final List<DSymbol> parts) {
		final boolean ok = super.partsListOkay(parts);
		if (ok) {
			++count;
		}
		if (countOnly) {
			return false;
		}
		return ok && (start <= count) && (stop <= 0 || stop > count);
	}
	
	protected boolean vertexFigureOkay(final DSymbol ds) {
		if (goodVertexFigures.contains(ds)) {
			return true;
		} else if (goodVertexFigures.contains(ds)) {
			return false;
		} else {
			final DynamicDSymbol t = new DynamicDSymbol(ds.dual());
			final IndexList idx = new IndexList(0, 1);
			final List<Integer> choices = new LinkedList<Integer>();
			for (final int D: t.orbitReps(idx)) {
				final int r = t.r(0, 1, D);
				if (r == 4 || r == 5) {
					t.redefineV(0, 1, D, 1);
				} else if (r == 3 || r == 6) {
					t.redefineV(0, 1, D, 6 / r);
				} else if (r == 1 || r == 2) {
					t.redefineV(0, 1, D, 4 / r);
					choices.add(D);
				} else {
					throw new RuntimeException("Oops!");
				}
			}
			// this tests whether the resulting tile has more than 28 vertices
			if (t.curvature2D().isLessThan(new Fraction(t.size(), 42))) {
				return false;
			}

			final Iterator<DSymbol> iter = new IteratorAdapter<DSymbol>() {
				final int n = choices.size();
				int a[] = null;

				protected DSymbol findNext() throws NoSuchElementException {
					while (true) {
						if (a == null) {
							a = new int[n + 1]; // better not risk null result
							for (int i = 0; i < n; ++i) {
								choose(i, 4);
							}
						} else {
							int i = n - 1;
							while (i >= 0 && a[i] == 6) {
								--i;
							}
							if (i < 0) {
								throw new NoSuchElementException("at end");
							}
							choose(i, 6);
							while (i < n - 1) {
								choose(++i, 4);
							}
						}
						return new DSymbol(t);
					}
				}

				private void choose(final int i, final int m) {
					final int D = choices.get(i);
					final int r = t.r(0, 1, D);
					t.redefineV(0, 1, D, m / r);
					a[i] = m;
				}
			};

			boolean good = false;
			while (iter.hasNext()) {
				final DSymbol x = iter.next();
				if (x.isSpherical2D()) {
					final DSymbol cover = Covers.finiteUniversalCover(x);
					final int n = cover.numberOfOrbits(iFaces2d);
					if (n >= 12 && n <= 16) {
						good = true;
						break;
					}
				}
			}
			if (good) {
				goodVertexFigures.add(ds);
			} else {
				badVertexFigures.add(ds);
			}
			return good;
		}
	}
	
	public int getCount() {
		return count;
	}
	
	protected void processEvent(final Object event) {
		final CheckpointEvent ce = (CheckpointEvent) event;
		if (ce.getMessage() != null || interval == 0
				|| this.timer.elapsed() > interval) {
			writeCheckpoint(ce.isOld(), ce.getMessage());
		}
	}
	
	public void writeCheckpoint(final boolean isOld, final String msg) {
		try {
			this.timer.reset();
			final String p = isOld ? "# OLD" : "#@";
			final String c = getCheckpoint();
			final String s = msg != null ? String.format(" (%s)", msg) : "";
			output.write(String.format("%s CHECKPOINT %s%s\n", p, c, s));
			output.flush();
		} catch (Throwable ex) {
		}
	}
	
	private static boolean allTileSizesBetween(final DSymbol ds, final int min,
			final int max) {
		for (final int D: ds.orbitReps(iTiles)) {
			final DSymbol tile =
			        new DSymbol(new Subsymbol<Integer>(ds, iTiles, D));
			final DSymbol cover = Covers.finiteUniversalCover(tile);
			final int n = cover.numberOfOrbits(iFaces2d);
			if (n < min || n > max) {
				return false;
			}
		}
		return true;
	}
	
	private static String info(final DSymbol ds) {
		final StringBuffer buf = new StringBuffer(50);
		final DSymbol cover = Covers.pseudoToroidalCover3D(ds);
		final int count[] = new int[5];
		int nt = 0;
		int sum_nf = 0;
		int sum_nfsqr = 0;
		for (final int D: cover.orbitReps(iTiles)) {
			final DSymbol tile =
			        new DSymbol(new Subsymbol<Integer>(cover, iTiles, D));
			final int k = tile.numberOfOrbits(iFaces2d);
			if (k < 12) {
				return "found tile with less than 12 faces";
			}
			if (k > 16) {
				return "found tile with more than 16 faces";
			}
			++count[k-12];
			sum_nf += k;
			sum_nfsqr += k * k;
			++nt;
		}
		buf.append("composition: [");
		for (int i = 0; i < 5; ++i) {
			buf.append(' ');
			buf.append(count[i] > 0 ? String.valueOf(count[i]) : "-");
		}
		final double avg_nf = sum_nf / (double) nt;
		buf.append(" ]   <f> = ");
		buf.append(avg_nf);
		buf.append("   stdev = ");
		buf.append(Math.sqrt(sum_nfsqr / (double) nt - avg_nf * avg_nf));
		return buf.toString();
	}
	
	public static void usage() {
		System.err.print(
			  "Usage: java -jar Kelvin.jar [OPTION]... K [FILE]\n"
			+ "\n"
			+ "Recognized options:\n"
			+ "  -e        skip euclidicity test\n"
			+ "  -i N      interval in seconds between writing checkpoints\n"
			+ "  -p        skip pre-filtering by vertex stabilizers\n"
			+ "  -r A-B-C  resume generation at a checkpoint\n"
			+ "  -s P/Q    generate the P-th of Q parts\n"
			+ "  -t        skip on-the-fly testing of completed tiles\n"
			+ "  -v        run in verbose mode\n"
			);
		System.exit(1);
	}
	
	public static void main(final String[] args) {
		try {
			boolean verbose = false;
			boolean testParts = true;
			boolean testTiles = true;
			boolean testEuclidicity = true;
			int start = 0;
			int stop = 0;
			int section = 0;
			int nrOfSections = 0;
			int checkpointInterval = 3600;
			String resume = null;
			
			int i = 0;
			while (i < args.length && args[i].startsWith("-")) {
				if (args[i].equals("-v")) {
					verbose = !verbose;
				} else if (args[i].equals("-e")) {
					testEuclidicity = !testEuclidicity;
				} else if (args[i].equals("-i")) {
					checkpointInterval = Integer.parseInt(args[++i]);
				} else if (args[i].equals("-p")) {
					testParts = !testParts;
				} else if (args[i].equals("-r")) {
					resume = args[++i];
				} else if (args[i].equals("-s")) {
					final String tmp[] = args[++i].split("/");
					section = Integer.parseInt(tmp[0]);
					nrOfSections = Integer.parseInt(tmp[1]);
					if (nrOfSections < 1) {
						String msg = "Number of sections must be positive.";
						System.err.println(msg);
						System.exit(1);
					} else if (section < 1 || section > nrOfSections) {
						String msg = "Section # must be between 1 and "
								+ nrOfSections;
						System.err.println(msg);
						System.exit(1);
					}
				} else if (args[i].equals("-t")) {
					testTiles = !testTiles;
				} else if (args[i].equals("-x")) {
					final String tmp[] = args[++i].split("-");
					start = Integer.parseInt(tmp[0]);
					stop = Integer.parseInt(tmp[1]) + 1;
				} else {
					usage();
				}
				++i;
			}

			if (args.length <= i) {
				usage();
			}
			
			final int k = Integer.parseInt(args[i]);
			final Writer output;
			if (args.length > i + 1) {
				output = new FileWriter(args[i + 1]);
			} else {
				output = new OutputStreamWriter(System.out);
			}

			int countTileSizeOk = 0;
			int countGood = 0;
			int countAmbiguous = 0;

			final Stopwatch timer = new Stopwatch();
			final Stopwatch eTestTimer = new Stopwatch();
			int n = 0;
			timer.start();
			
			output.write("# Program Kelvin with k = " + k + ".\n");
			output.write("# Options:\n");
			output.write("#     verbose mode:                    ");
			output.write((verbose ? "on" : "off") + "\n");
			output.write("#     pre-filter by vertex symmetries: ");
			output.write((testParts ? "on" : "off") + "\n");
			output.write("#     test completed tiles on the fly: ");
			output.write((testTiles ? "on" : "off") + "\n");
			output.write("#     euclidicity test:                ");
			output.write((testEuclidicity ? "on" : "off") + "\n");
			if (checkpointInterval > 0) {
				output.write("#     checkpoint interval:             ");
				output.write(checkpointInterval + "sec\n");
			} else {
				output.write("#     checkpoints:                     off\n");
			}
			output.flush();
			
			if (nrOfSections > 0) {
				final Kelvin tmp = new Kelvin(k, verbose, testParts, true, 0,
						0, 0, null);
				while (tmp.hasNext()) {
					tmp.next();
				}
				n = tmp.getCount();
				final int m = nrOfSections;
				final int s = section;
				start = (int) Math.round(n / (double) m * (s-1)) + 1;
				stop  = (int) Math.round(n / (double) m * s) + 1;
			}
			
			if (nrOfSections > 0) {
				output.write("# Running section " + section + " of "
						+ nrOfSections + " (cases " + start + " to "
						+ (stop - 1) + " of " + n + ").\n");
			} else if (stop > 0) {
				output.write("# Running cases " + start + " to " + stop + ".\n");
			}
			if (resume != null) {
				output.write(String.format("# Resuming at %s.\n", resume));
			}
			output.write("\n");
			output.flush();
			
			final Kelvin iter = new Kelvin(k, verbose, testParts, false, start,
					stop, checkpointInterval, output);
			if (resume != null) {
				iter.setResumePoint(resume);
			}
			iter.setTestVertexFigures(testTiles);

			for (final DSymbol ds: iter) {
				final DSymbol out = ds.dual();
				if (allTileSizesBetween(out, 12, 16)) {
					++countTileSizeOk;
					if (testEuclidicity) {
						eTestTimer.start();
						EuclidicityTester tester = new EuclidicityTester(out);
						final boolean bad = tester.isBad();
						final boolean ambiguous = tester.isAmbiguous();
						eTestTimer.stop();
						if (!bad) {
							iter.writeCheckpoint(false, "new symbol found");
							if (ambiguous) {
								output.write("#@ name euclidicity dubious\n");
								++countAmbiguous;
							}
							output.write("# " + info(out) + "\n");
							output.write(out + "\n");
							++countGood;
						}
					} else {
						output.write(out + "\n");
					}
					output.flush();
				}
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
			output.write("#     Total time for computing deductions was "
					+ iter.getTimeForComputingDeductions() + ".\n");
			output.write("#       Time for finding cuts was "
					+ iter.getTimeForFindingCuts() + ".\n");
			if (testTiles) {
				output.write("#       Time for testing tiles was "
						+ iter.getTimeForVertexFigureTests() + ".\n");
			}
			if (testEuclidicity) {
				output.write("#   Time for euclidicity tests was "
						+ eTestTimer.format() + ".\n");
			}
			output.write("# [timing method: " + eTestTimer.mode() + "]\n");
			output.write("\n");
			output.write("# " + iter.statistics() + "\n");
			output.write("# Of the latter, " + countTileSizeOk
					+ " had between 12 and 16 faces in each tile.\n");
			if (testEuclidicity) {
				output.write("# Of those, " + countGood
						+ " were found euclidean.\n");
				if (countAmbiguous > 0) {
					output.write("# For " + countAmbiguous
							+ " symbols, euclidicity remains undetermined.\n");
				}
			}
			output.flush();
		} catch (final Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
