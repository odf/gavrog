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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.NiftyList;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

/**
 */
public class Transitivity2112 extends IteratorAdapter<DSymbol> {
	final static private IndexList idcsFace2d = new IndexList(0, 1);
	final static private IndexList idcsVert2d = new IndexList(1, 2);
	final static private IndexList idcsTile3d = new IndexList(0, 1, 2);
	final static private IndexList idcsFace3d = new IndexList(0, 1, 3);
	final static private IndexList idcsEdge3d = new IndexList(0, 2, 3);
	final static private IndexList idcsVert3d = new IndexList(1, 2, 3);

    // --- used by SingleFaceTiles
    final static private Map<NiftyList<Integer>, List<DSymbol>> singleBases =
            new HashMap<NiftyList<Integer>, List<DSymbol>>();

    /**
     * Generates all feasible 2-dimensional symbols extending a given
     * 1-dimensional one. This version works by introducing extra degree 2
     * vertices into tiles with a smaller face in all possible ways. The base
     * tiles for this process are generated by the direct method of {@link
     * #BaseSingleFaceTiles}
     */
    private class SingleFaceTiles extends IteratorAdapter<DSymbol> {
    	final private DSymbol inputFace;
        final private int minVert;
        final private int targetSize;
        final private Iterator<DSymbol> baseFaces;
        private Iterator<DSymbol> baseTiles;
        private Iterator<DSymbol> augmented;
        
        public SingleFaceTiles(final DSymbol face, final int minVert) {
        	time4SingleFaced.start();
        	this.inputFace = face;
            this.minVert = minVert;
            this.targetSize = face.size();
            if (minVert >= 3) {
                this.baseFaces = Iterators.singleton(face);
            } else {
                this.baseFaces = baseFaces(face).iterator();
            }
            this.baseTiles = Iterators.empty();
            this.augmented = Iterators.empty();
            time4SingleFaced.stop();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected DSymbol findNext() throws NoSuchElementException {
        	time4SingleFaced.start();
            while (true) {
                if (this.augmented.hasNext()) {
                	final DSymbol ds = this.augmented.next();
                	final int D = ds.elements().next();
                	final DSymbol face = new DSymbol(
                	        new Subsymbol<Integer>(ds, idcsFace2d, D));
                	if (face.equals(this.inputFace)) {
                		time4SingleFaced.stop();
                		++countSingleFaced;
                		return ds;
                	}
                } else if (this.baseTiles.hasNext()) {
                    final DSymbol tile = this.baseTiles.next();
                    if (this.minVert >= 3) {
                        if (this.minVert <= minVert(tile)) {
                            return tile;
                        } else {
                            continue;
                        }
                    } else {
                        this.augmented =
                                new SplitEdges2d(tile, this.targetSize);
                    }
                } else if (this.baseFaces.hasNext()) {
                    final DSymbol face = this.baseFaces.next();
                    final NiftyList<Integer> invariant = face.invariant();
                    if (!singleBases.containsKey(invariant)) {
                        final Iterator<DSymbol> tiles =
                                new BaseSingleFaceTiles(face, 3);
                        singleBases.put(invariant, Iterators.asList(tiles));
                    }
                    this.baseTiles = singleBases.get(invariant).iterator();
                } else {
                	time4SingleFaced.stop();
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }

    /**
     * Generates all feasible 2-dimensional symbols extending a given
     * 1-dimensional one. This class uses a direct method and is used by {@link
     * #SingleFaceTiles} to generate base tiles with minimal vertex degree 3.
     */
    private class BaseSingleFaceTiles extends IteratorAdapter<DSymbol> {
        final private Rational minCurv = new Fraction(1, 12);
        final private int minVert;
        Iterator<DSymbol> preTiles = Iterators.empty();
        Iterator<DSymbol> tiles = Iterators.empty();

        public BaseSingleFaceTiles(final DSymbol face, final int minVert) {
            time4BaseSingleFaced.start();
            this.minVert = minVert;
			if (minVert >= 3 && face.m(0, 1, face.elements().next()) > 5) {
				this.preTiles = Iterators.empty();
			} else {
				this.preTiles = new CombineTiles(face);
			}
			time4BaseSingleFaced.stop();
		}

        /*
		 * (non-Javadoc)
		 * 
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
        protected DSymbol findNext() throws NoSuchElementException {
            time4BaseSingleFaced.start();
            while (true) {
                if (this.tiles.hasNext()) {
                    final DSymbol ds = this.tiles.next();
                    if (!ds.isSpherical2D()) {
                        continue;
                    }
                    for (final int D: ds.elements()) {
                        if (ds.m(1, 2, D) > 2) {
                            time4BaseSingleFaced.stop();
                            ++countBaseSingleFaced;
                            return ds;
                        }
                    }
                } else if (this.preTiles.hasNext()) {
                    final DSymbol ds = this.preTiles.next();
                    this.tiles = new DefineBranching2d(ds, 2, minVert, minCurv);
                } else {
                    time4BaseSingleFaced.stop();
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }

    private class Glue extends IteratorAdapter<DSymbol> {
    	final DSymbol ds;
		final int D0;
		final Iterator<Integer> targets;
		final Set<DSymbol> seen = new HashSet<DSymbol>();

		public Glue(final DSymbol ds) {
			this.ds = ds;
			final Iterator<Integer> reps = ds.orbitReps(idcsFace2d);
			final int firstRep = reps.next();
			final int targetRep = (reps.hasNext() ? reps.next() : firstRep);
			if (ds.orbitIsLoopless(idcsFace2d, firstRep)) {
				this.D0 = firstRep;
				this.targets = ds.orbit(idcsFace2d, targetRep);
			} else {
				final List<Integer> loops0 = new ArrayList<Integer>();
				final List<Integer> loops1 = new ArrayList<Integer>();
				for (final int D: ds.orbit(idcsFace2d, firstRep)) {
					if (D == ds.op(0, D)) {
						loops0.add(D);
					} else if (D == ds.op(1, D)) {
						loops1.add(D);
					}
				}
				final int type = loops0.size();
				if (type > 0) {
					this.D0 = loops0.get(0);
				} else {
					this.D0 = loops1.get(0);
				}
				loops0.clear();
				loops1.clear();
				for (final int D: ds.orbit(idcsFace2d, targetRep)) {
					if (D == ds.op(0, D)) {
						loops0.add(D);
					} else if (D == ds.op(1, D)) {
						loops1.add(D);
					}
				}
				if (type > 0) {
					this.targets = loops0.iterator();
				} else {
					this.targets = loops1.iterator();
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected DSymbol findNext() throws NoSuchElementException {
			while (true) {
				if (this.targets.hasNext()) {
					final int E0 = this.targets.next();
					final DynamicDSymbol tmp = new DynamicDSymbol(3);
					tmp.append(this.ds);
					int D = D0;
					int E = E0;
					do {
						for (int k = 0; k <= 1; ++k) {
							tmp.redefineOp(3, D, E);
							D = tmp.op(k, D);
							E = tmp.op(k, E);
						}
					} while (D != D0);
					boolean bad = false;
					for (final int elm: tmp.elements()) {
						if (!tmp.definesOp(3, elm)) {
							bad = true;
							break;
						}
					}
					if (bad) {
						continue;
					}
					final DSymbol res = new DSymbol(tmp);
					if (
                        this.seen.contains(res) ||
                        res.numberOfOrbits(idcsTile3d) != 2 ||
                        res.numberOfOrbits(idcsFace3d) != 1 ||
                        res.numberOfOrbits(idcsEdge3d) != 1 ||
                        //res.numberOfOrbits(idcsVert3d) != 2 ||
                        hasTrivialVertices(res)
                    ) {
						continue;
					}
					this.seen.add(res);
					if (Utils.mayBecomeLocallyEuclidean3D(res)) {
						return res;
					}
				} else {
					throw new NoSuchElementException("at end");
				}
			}
		}
    }
    
    /**
	 * Generates all minimal, euclidean, 3-dimensional symbols containing two
	 * 2-dimensional symbols each made from the single given 1-dimensional one.
	 */
    private class TWO extends IteratorAdapter<DSymbol> {
    	final List<DSymbol> tiles;
    	int i, j;
        Iterator<DSymbol> tilings = Iterators.empty();
        
    	public TWO(final DSymbol face, final int minVert) {
    		this.tiles = Iterators.asList(new SingleFaceTiles(face, minVert));
    		i = j = 0;
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected DSymbol findNext() throws NoSuchElementException {
            while (true) {
                if (this.tilings.hasNext()) {
                    return this.tilings.next();
                } else if (this.i < this.tiles.size()) {
                	final DSymbol t1 = this.tiles.get(i);
                	final DSymbol t2 = this.tiles.get(j);
                	++j;
                	if (j >= this.tiles.size()) {
                		j = ++i;
                	}
                	final DynamicDSymbol ds = new DynamicDSymbol(t1);
                	ds.append(t2);
                	this.tilings = new Glue(new DSymbol(ds));
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
		}
    }
    
    final static DSymbol baseTile(final DSymbol tile) {
        final DynamicDSymbol ds = new DynamicDSymbol(tile);
        final List<Integer> reps = Iterators.asList(ds.orbitReps(idcsVert2d));
        for (final int D: reps) {
            if (ds.m(1, 2, D) == 2) {
                ds.redefineV(1, 2, D, 1);
                ds.collapse(Iterators.asList(ds.orbit(idcsVert2d, D)), 1);
            }
        }
        return new DSymbol(ds);
    }
    
    final private int maxSize;
    final private int minVert;

    private int size;
    private Iterator<DSymbol> faces = Iterators.empty();
    private Iterator<DSymbol> preTilings = Iterators.empty();
    private Iterator<DSymbol> tilings = Iterators.empty();

    private int badVertices = 0;
    private int nonMinimal = 0;
    private int badInvariant = 0;
    private int nonEuclidean = 0;
    private int countSingleFaced = 0;
    private int countDoubleFaced = 0;
	private int countBaseSingleFaced = 0;
	private int countBaseDoubleFaced = 0;
    private int countONE = 0;
    private int countTWO = 0;
    private int countDOUBLE = 0;
    
    private Stopwatch time4invar = new Stopwatch();
    private Stopwatch time4euclid = new Stopwatch();
    private Stopwatch time4Two = new Stopwatch();
    private Stopwatch time4SingleFaced = new Stopwatch();
    private Stopwatch time4BaseSingleFaced = new Stopwatch();
    private Stopwatch time4Final = new Stopwatch();

    public Transitivity2112(final int minSize, final int maxSize,
            final int minVertexDegree) {
    	if (minVertexDegree >= 3 && (maxSize > 20 || maxSize < 1)) {
    		this.maxSize = 20;
    	} else {
    		this.maxSize = maxSize;
    	}
        this.minVert = minVertexDegree;
        
        this.size = minSize % 2 == 0 ? minSize - 2 : minSize - 1;
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.box.collections.IteratorAdapter#findNext()
     */
    protected DSymbol findNext() throws NoSuchElementException {
        while (true) {
            if (moreTilings()) {
                time4Final.start();
                final DSymbol ds = this.tilings.next();
                time4Final.stop();
                if (isGood(ds)) {
                    return ds;
                }
            } else if (morePreTilings()) {
                startCaseTimer();
                final DSymbol ds = this.preTilings.next();
                stopCaseTimer();

                ++countTWO;

                time4Final.start();
                this.tilings = new DefineBranching3d(ds);
                time4Final.stop();
            } else if (this.faces.hasNext()) {
                final DSymbol face = faces.next();
                this.preTilings = new TWO(face, this.minVert);
            } else if (this.maxSize < 1 || this.size < this.maxSize) {
                this.size += 2;
                this.faces = new Faces(this.size / 2, 3, 4);
            } else {
                throw new NoSuchElementException("at end");
            }
        }
    }
    
    private boolean moreTilings()
    {
        time4Final.start();
        boolean moreTilings = this.tilings.hasNext();
        time4Final.stop();
        return moreTilings;
    }
    
    private void startCaseTimer() {
        time4Two.start();
    }
    
    private void stopCaseTimer() {
        time4Two.stop();
    }
    
    private boolean morePreTilings()
    {
        startCaseTimer();
        boolean more = this.preTilings.hasNext();
        stopCaseTimer();
        return more;
    }
    
    /**
     * Determines a list of 1-dimensional symbols such that every
     * 2-dimensional symbol extending the given 1-dimensional can be
     * obtained by introducing degree 2 vertices into a 2-dimensional
     * extension of some symbol in the list.
     * 
     * @param face the 1-dimensional input symbol.
     * @return the list of base symbols for the input symbol.
     */
    private static List<DSymbol> baseFaces(final DSymbol face) {
        final List<DSymbol> results = new LinkedList<DSymbol>();
        final int v = face.v(0, 1, face.elements().next());
        final boolean loopless = face.isLoopless();
        final int d = v * (loopless ? 1 : 2);
        for (int n = 4; n <= face.size() * d; n += 2) {
            if (n % d != 0) {
                continue;
            }
            final int size = n / d;
            if (size > face.size()) {
                continue;
            }
            for (final DSymbol ds: new Faces(size, 2, 4)) {
                if (ds.v(0, 1, ds.elements().next()) == v
                        && ds.isLoopless() == loopless)
                {
                    results.add(ds);
                }
            }
        }
        return results;
    }

    /**
     * Returns the minimum vertex degree present in a tile.
     * 
     * @param tile a 2-dimensional symbol encoding the tile.
     * @return the minimum vertex degree for the input tile.
     */
    private static int minVert(final DSymbol tile) {
        int min = Integer.MAX_VALUE;
        for (final int D: tile.orbitReps(idcsVert2d)) {
            min = Math.min(min, tile.m(1, 2, D));
        }
        return min;
    }
    
	private boolean hasTrivialVertices(final DSymbol ds) {
        for (final int D: ds.orbitReps(idcsVert3d)) {
            boolean bad = true;
            for (final int E: ds.orbit(idcsVert3d, D)) {
                if (ds.m(1, 2, E) > 2) {
                    bad = false;
                    break;
                }
            }
            if (bad) {
                ++this.badVertices;
                return true;
            }
        }
        return false;
    }

    private boolean isGood(final DSymbol ds) {
        if (!ds.isMinimal()) {
            ++this.nonMinimal;
            return false;
        }
        this.time4invar.start();
        final boolean ok = EuclidicityTester.invariantOkay(ds);
        this.time4invar.stop();
        if (!ok) {
        	++this.badInvariant;
            return false;
        }
        this.time4euclid.start();
        final boolean bad = new EuclidicityTester(ds).isBad();
        this.time4euclid.stop();
        if (bad) {
            ++this.nonEuclidean;
            return false;
        }

        return true;
    }
    
    public int getBadVertices() {
        return this.badVertices;
    }

	public int getBadInvariant() {
    	return this.badInvariant;
    }
    
    public int getNonEuclidean() {
        return this.nonEuclidean;
    }

    public int getNonMinimal() {
        return this.nonMinimal;
    }
    
    public int getCountSingleFaced() {
        return this.countSingleFaced;
    }

    public int getCountDoubleFaced() {
        return this.countDoubleFaced;
    }

	public int getCountBaseDoubleFaced() {
		return this.countBaseDoubleFaced;
	}

	public int getCountBaseSingleFaced() {
		return this.countBaseSingleFaced;
	}

    public int getCountONE() {
        return this.countONE;
    }

    public int getCountTWO() {
        return this.countTWO;
    }

    public int getCountDOUBLE() {
        return this.countDOUBLE;
    }

    public String timeForInvariantTest() {
    	return this.time4invar.format();
    }
    
    public String timeForEuclidicityTest() {
        return this.time4euclid.format();
    }
    
    public String timeForCaseTwo() {
        return this.time4Two.format();
    }
    
    public String timeForSingleFacedTiles() {
        return this.time4SingleFaced.format();
    }
    
    public String timeForBaseSingleFacedTiles() {
        return this.time4BaseSingleFaced.format();
    }
    
    public String timeForFinalBranching() {
        return this.time4Final.format();
    }
    
    /**
     * Main method.
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        final int n = args.length;
        final int minSize = (n > 0 ? Integer.parseInt(args[0]) : 1);
        final int maxSize = (n > 1 ? Integer.parseInt(args[1]) : 8);
        final int minVert = (n > 2 ? Integer.parseInt(args[2]) : 2);

        System.out.println("# Generated by Gavrog on "
				+ Calendar.getInstance().getTime());
        System.out.println("#");
        System.out.println("# Class: Transitivity2112");
        System.out.print("# Command line arguments:");
        for (int i = 0; i < n; ++i) {
        	System.out.print(" " + args[i]);
        }
        System.out.println();
        System.out.println("#");
        System.out.println("# Running Java "
				+ System.getProperty("java.version") + " on "
				+ System.getProperty("os.name"));
        System.out.println("#");
        final Stopwatch total = new Stopwatch();
        total.start();
        final Transitivity2112 symbols = new Transitivity2112(minSize, maxSize,
                minVert);
		final int count = Iterators.print(System.out, symbols, "\n");
		total.stop();
        System.out.println();
        System.out.println("#");
		System.out.println("# Generated " + count + " symbols.");
		System.out.println("# Rejected " + symbols.getBadVertices()
				+ " symbols with trivial vertices,");
		System.out.println("#          " + symbols.getNonMinimal()
				+ " non-minimal symbols,");
		System.out.println("#          " + symbols.getBadInvariant()
				+ " symbols with non-Euclidean orbifold invariants and");
		System.out.println("#          " + symbols.getNonEuclidean()
				+ " further non-Euclidean symbols");
		System.out.println("# Execution time was " + total.format() + ".");
        System.out.println("#     Used " + symbols.timeForInvariantTest()
                + " for invariant tests.");
        System.out.println("#     Used " + symbols.timeForEuclidicityTest()
                + " for other Euclidicity tests.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForCaseTwo()
                + " to generate " + symbols.getCountTWO()
                + " Delaney sets.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForBaseSingleFacedTiles()
				+ " to generate " + symbols.getCountBaseSingleFaced()
				+ " single-faced base tiles.");
        System.out.println("#     Used " + symbols.timeForSingleFacedTiles()
				+ " to generate " + symbols.getCountSingleFaced()
				+ " single-faced tiles.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForFinalBranching()
                + " to generate final branching.");
        System.out.println("#");
        System.out.println("# Program finished on "
				+ Calendar.getInstance().getTime());
    }
}
