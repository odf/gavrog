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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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
public class FaceTransitive extends IteratorAdapter {
	final static private boolean TEST = false;
	final static private List idcsFace2d = new IndexList(0, 1);
	final static private List idcsEdge2d = new IndexList(0, 2);
	final static private List idcsVert2d = new IndexList(1, 2);
	final static private List idcsFace3d = new IndexList(0, 1, 3);
	final static private List idcsVert3d = new IndexList(1, 2, 3);

    /**
     * Generates all feasible 1-dimensional symbols of a given size.
     */
    public static class Faces extends IteratorAdapter {
		final private int minFace;
        final Iterator sets;
        int v;
        DynamicDSymbol currentSet = null;
        
        public Faces(final int size, int minFace) {
        	this.minFace = minFace;
            if (size % 2 == 1) {
                final DynamicDSymbol ds = new DynamicDSymbol(1);
                final List<Integer> elms = ds.grow(size);
                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(0, elms.get(i-1), elms.get(i));
                    ds.redefineOp(1, elms.get(i), elms.get(i+1));
                }
                ds.redefineOp(1, elms.get(0), elms.get(0));
                ds.redefineOp(0, elms.get(size-1), elms.get(size-1));
                this.sets = Iterators.singleton(new DSymbol(ds));
            } else {
                final List tmp = new LinkedList();
                final DynamicDSymbol ds = new DynamicDSymbol(1);
                final List<Integer> elms = ds.grow(size);
                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(0, elms.get(i-1), elms.get(i));
                    ds.redefineOp(1, elms.get(i), elms.get((i+1) % size));
                }
                tmp.add(new DSymbol(ds));

                ds.redefineOp(1, elms.get(0), elms.get(0));
                ds.redefineOp(1, elms.get(size-1), elms.get(size-1));
                tmp.add(new DSymbol(ds));

                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(1, elms.get(i-1), elms.get(i));
                    ds.redefineOp(0, elms.get(i), elms.get((i+1) % size));
                }
                ds.redefineOp(0, elms.get(0), elms.get(0));
                ds.redefineOp(0, elms.get(size-1), elms.get(size-1));
                tmp.add(new DSymbol(ds));

                this.sets = tmp.iterator();
            }
        }
        
        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.currentSet != null && this.v <= 4) {
                    final DynamicDSymbol ds = this.currentSet;
                    final int D = ds.elements().next();
                    ds.redefineV(0, 1, D, this.v);
                    ++this.v;
                    if (ds.m(0, 1, D) < this.minFace) {
                    	continue;
                    }
                    return new DSymbol(ds);
                } else if (this.sets.hasNext()) {
                    final DSymbol ds = (DSymbol) this.sets.next();
                    this.currentSet = new DynamicDSymbol(ds);
                    this.v = 1;
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }

    /**
     * Augments a tile in all possible ways by splitting edges (introducing
     * vertices of degree two) so that the symbol for the resulting tile has a
     * specified size.
     */
    private class Augmenter extends IteratorAdapter {
        final private DSymbol base;
        final private int targetSize;
        final int orbRep[];
        final int orbSize[];
        final int orbAdded[];
        int currentSize;
        final Set results = new HashSet();

        /**
         * Constructs an instance.
         * @param base the symbol representing the input tile
         * @param size the target size for the augmented symbol
         */
        public Augmenter(final DSymbol base, final int size) {
            // --- store parameters
            this.base = base;
            this.targetSize = size;
            this.currentSize = base.size();
            
            // --- collect (0,2)-orbits
            final List<List<Integer>> orbits = new ArrayList<List<Integer>>();
            for (final int D: base.orbitReps(idcsEdge2d)) {
				final List orbit = Iterators.asList(base.orbit(idcsEdge2d, D));
                orbits.add(orbit);
            }

            // --- sort by decreasing size
            Collections.sort(orbits, new Comparator() {
                public int compare(final Object arg0, final Object arg1) {
                    final List l0 = (List) arg0;
                    final List l1 = (List) arg1;
                    return l1.size() - l0.size();
                }
            });
            
            // --- create arrays
            final int n = orbits.size();
            this.orbRep = new int[n];
            this.orbSize = new int[n];
            this.orbAdded = new int[n];
            
            // --- fill arrays
            for (int i = 0; i < n; ++i) {
                final List<Integer> orb = orbits.get(i);
                this.orbRep[i] = orb.get(0);
                this.orbSize[i] = orb.size();
                this.orbAdded[i] = 0;
            }
            
            // --- a little trick to make findNext() code simpler
            if (this.currentSize == this.targetSize) {
                this.orbAdded[n-1] = -this.orbSize[n-1];
                this.currentSize -= this.orbSize[n-1];
            }
        }
        
        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                final int target = this.targetSize;
                int i = this.orbRep.length - 1;
                while (i >= 0 && this.currentSize + this.orbSize[i] > target) {
                    this.currentSize -= this.orbAdded[i];
                    this.orbAdded[i] = 0;
                    --i;
                }
                if (i < 0) {
                    throw new NoSuchElementException("at end");
                }
                this.orbAdded[i] += this.orbSize[i];
                this.currentSize += this.orbSize[i];

                if (this.currentSize == this.targetSize) {
                    final DSymbol ds = augmented();
                    final List invariant = ds.invariant();
                    if (!this.results.contains(invariant)) {
                        this.results.add(invariant);
                        return ds;
                    }
                }
            }
        }

        /**
         * Constructs the augmented symbol based on the current values in
         * <code>orbAdded</code>.
         * @return the augmented symbol.
         */
        private DSymbol augmented() {
            final DynamicDSymbol ds = new DynamicDSymbol(this.base);
            for (int i = 0; i < this.orbRep.length; ++i) {
                final int size = this.orbSize[i];
                final int added = this.orbAdded[i];
                if (added == 0) {
                    continue;
                }

                // --- holds orbit and added elements
                final int D[] = new int[size + added];
                // --- encodes original 0 operation
                final int op0[];
                // --- encodes original 2 operation
                final int op2[];

                // --- fill "op" arrays and start "D" array
                D[0] = this.orbRep[i];
                switch (size) {
                case 1:
                    op0 = new int[] { 0 };
                    op2 = new int[] { 0 };
                    break;
                case 2:
                    if (ds.op(0, D[0]).equals(D[0])) {
                        D[1] = ds.op(2, D[0]);
                        op0 = new int[] { 0, 1 };
                    } else {
                        D[1] = ds.op(0, D[0]);
                        op0 = new int[] { 1, 0 };
                    }
                    if (ds.op(2, D[0]).equals(D[0])) {
                        op2 = new int[] { 0, 1 };
                    } else {
                        op2 = new int[] { 1, 0 };
                    }
                    break;
                case 4:
                    D[1] = ds.op(0, D[0]);
                    D[2] = ds.op(2, D[1]);
                    D[3] = ds.op(0, D[2]);
                    op0 = new int[] { 1, 0, 3, 2 };
                    op2 = new int[] { 3, 2, 1, 0 };
                    break;
                default:
                    throw new RuntimeException("this should not happen");
                }
                
                // --- remember v01 values and remove 0 edges
                final int v[] = new int[size];
                for (int k = 0; k < size; ++k) {
                    final int E = D[k];
                    v[k] = ds.v(0, 1, E);
                    ds.undefineOp(0, E);
                }
                
                // --- add new elements for augmentation to array
                final List<Integer> newElements = ds.grow(added);
                for (int k = 0; k < added; ++k) {
                    D[size + k] = newElements.get(k);
                }
                
                // --- set some v values
                int n = added / size;
                for (int k = 1; k <= n; ++k) {
                    for (int m = 0; m < size; ++m) {
                        ds.redefineV(0, 1, D[k*size + m], v[m]);
                    }
                }
                
                // --- connect the elements
                int idx = 0;
                for (int k = 0; k <= n; ++k) {
                    for (int m = 0; m < size; ++m)  {
                        final int E = D[k*size + m];
                        final int E2 = D[k*size + op2[m]];
                        ds.redefineOp(2, E, E2);
                        final int Ei;
                        if (k < n) {
                            Ei = D[(k+1)*size + m];
                        } else {
                            Ei = D[k*size + op0[m]];
                        }
                        ds.redefineOp(idx, E, Ei);
                    }
                    idx = 1 - idx;
                }

                // --- set more v values
                for (int k = size; k < D.length; ++k) {
                    final int E = D[k];
                    ds.redefineV(1, 2, E, 2 / ds.r(1, 2, E));
                }
            }
            return new DSymbol(ds);
        }
    }
    
    // --- used by SingleFaceTiles
    final static private Map singleBases = new HashMap();

    /**
     * Generates all feasible 2-dimensional symbols extending a given
     * 1-dimensional one. This version works by introducing extra degree 2
     * vertices into tiles with a smaller face in all possible ways. The base
     * tiles for this process are generated by the direct method of {@link
     * #BaseSingleFaceTiles}
     */
    private class SingleFaceTiles extends IteratorAdapter {
    	final private DSymbol inputFace;
        final private int minVert;
        final private int targetSize;
        final private Iterator baseFaces;
        private Iterator baseTiles;
        private Iterator augmented;
        
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
        protected Object findNext() throws NoSuchElementException {
        	time4SingleFaced.start();
            while (true) {
                if (this.augmented.hasNext()) {
                	final DSymbol ds = (DSymbol) this.augmented.next();
                	final Object D = ds.elements().next();
                	final DSymbol face = new DSymbol(new Subsymbol(ds,
							idcsFace2d, D));
                	if (face.equals(this.inputFace)) {
                		time4SingleFaced.stop();
                		++countSingleFaced;
                		return ds;
                	}
                } else if (this.baseTiles.hasNext()) {
                    final DSymbol tile = (DSymbol) this.baseTiles.next();
                    if (this.minVert >= 3) {
                        if (this.minVert <= minVert(tile)) {
                            return tile;
                        } else {
                            continue;
                        }
                    } else {
                        this.augmented = new Augmenter(tile, this.targetSize);
                    }
                } else if (this.baseFaces.hasNext()) {
                    final DSymbol face = (DSymbol) this.baseFaces.next();
                    final List invariant = face.invariant();
                    if (!singleBases.containsKey(invariant)) {
                        final Iterator tiles = new BaseSingleFaceTiles(face, 3);
                        singleBases.put(invariant, Iterators.asList(tiles));
                    }
                    this.baseTiles = ((List) singleBases.get(invariant)).iterator();
                } else {
                	time4SingleFaced.stop();
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }

    // --- used by DoubleFaceTiles
    final static private Map doubleBases = new HashMap();

    /**
     * Generates all feasible 2-dimensional symbols made from two copies of a
     * given 1-dimensional one. This version works by introducing extra degree 2
     * vertices into tiles with a smaller face in all possible ways. The base
     * tiles for this process are generated by the direct method of {@link
     * #BaseDoubleFaceTiles}
     */
    private class DoubleFaceTiles extends IteratorAdapter {
        final private DSymbol inputFace;
        final private int minVert;
        final private int targetSize;
        final private Iterator baseFacePairs;
        private Iterator baseTiles;
        private Iterator augmented;
        
        public DoubleFaceTiles(final DSymbol face, final int minVert) {
            time4DoubleFaced.start();
            this.inputFace = face;
            this.minVert = minVert;
            this.targetSize = 2 * face.size();
            if (minVert >= 3) {
                this.baseFacePairs = Iterators.singleton(Arrays
                        .asList(new Object[] { face, face }));
            } else {
                this.baseFacePairs = Iterators.selections(baseFaces(face)
                        .toArray(), 2);
            }
            this.baseTiles = Iterators.empty();
            this.augmented = Iterators.empty();
            time4DoubleFaced.stop();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            time4DoubleFaced.start();
            while (true) {
                if (this.augmented.hasNext()) {
                    final DSymbol ds = (DSymbol) this.augmented.next();
                    boolean okay = true;
                    for (final Iterator reps = ds
                            .orbitReps(idcsFace2d); reps.hasNext();) {
                        final Object D = reps.next();
                        final DSymbol face = new DSymbol(new Subsymbol(ds,
                                idcsFace2d, D));
                        if (!face.equals(this.inputFace)) {
                            okay = false;
                            break;
                        }
                    }
                    if (okay) {
                        time4DoubleFaced.stop();
                        ++countDoubleFaced;
                        return ds;
                    }
                } else if (this.baseTiles.hasNext()) {
                    final DSymbol tile = (DSymbol) this.baseTiles.next();
                    if (this.minVert >= 3) {
                        if (this.minVert <= minVert(tile)) {
                            return tile;
                        } else {
                            continue;
                        }
                    } else {
                        this.augmented = new Augmenter(tile, this.targetSize);
                    }
                } else if (this.baseFacePairs.hasNext()) {
                    final List item = (List) this.baseFacePairs.next();
                    final DSymbol f1 = (DSymbol) item.get(0);
                    final DSymbol f2 = (DSymbol) item.get(1);
                    final DynamicDSymbol tmp = new DynamicDSymbol(f1);
                    tmp.append(f2);
                    final DSymbol ds = new DSymbol(tmp);
                    final List invariant = ds.invariant();
                    if (!doubleBases.containsKey(invariant)) {
                        final Iterator tiles = new BaseDoubleFaceTiles(ds, 3);
                        doubleBases.put(invariant, Iterators.asList(tiles));
                    }
                    this.baseTiles = ((List) doubleBases.get(invariant)).iterator();
                } else {
                    time4DoubleFaced.stop();
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
    private class BaseSingleFaceTiles extends IteratorAdapter {
        final private Rational minCurv = new Fraction(1, 12);
        final private int minVert;
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();

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
        protected Object findNext() throws NoSuchElementException {
            time4BaseSingleFaced.start();
            while (true) {
                if (this.tiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tiles.next();
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
                    final DSymbol ds = (DSymbol) this.preTiles.next();
                    this.tiles = new DefineBranching2d(ds, 2, minVert, minCurv);
                } else {
                    time4BaseSingleFaced.stop();
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }
    
    /**
     * Generates all feasible 2-dimensional symbols made from two copies of a
     * given 1-dimensional one.
     */
    private class BaseDoubleFaceTiles extends IteratorAdapter {
		final private int minVert;
        final private Rational minCurv = new Fraction(1, 12);
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();
        
        public BaseDoubleFaceTiles(final DSymbol ds, final int minVert) {
            time4BaseDoubleFaced.start();
        	this.minVert = minVert;
        	boolean good = false;
        	for (final int D: ds.orbitReps(idcsFace2d)) {
        		if (ds.m(0, 1, D) <= 5) {
        			good = true;
        		}
			}
        	if (good) {
        		this.preTiles = new CombineTiles(ds);
        	} else {
        		this.preTiles = Iterators.empty();
        	}
            time4BaseDoubleFaced.stop();
        }

        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            time4BaseDoubleFaced.start();
            while (true) {
                if (this.tiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tiles.next();
                    if (!ds.isSpherical2D()) {
                        continue;
                    }
                    for (final int D: ds.elements()) {
                        if (ds.m(1, 2, D) > 2) {
                            time4BaseDoubleFaced.stop();
                            ++countBaseDoubleFaced;
                            return ds;
                        }
                    }
                } else if (this.preTiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTiles.next();
                    this.tiles = new DefineBranching2d(ds, 2, minVert, minCurv);
                } else {
                    time4BaseDoubleFaced.stop();
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }
    
    /**
     * Generates all feasible 2-dimensional symbols made from two copies of a
     * given 1-dimensional one.
     */
    private class SimpleDoubleFaceTiles extends IteratorAdapter {
        final private int minVert;
        final private Rational minCurv = new Fraction(1, 12);
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();
        
        public SimpleDoubleFaceTiles(final DSymbol face, final int minVert) {
            this.minVert = minVert;
            final DynamicDSymbol ds = new DynamicDSymbol(face);
            ds.append(face);
            this.preTiles = new CombineTiles(new DSymbol(ds));
        }

        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.tiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tiles.next();
                    if (!ds.isSpherical2D()) {
                        continue;
                    }
                    for (final int D: ds.elements()) {
                        if (ds.m(1, 2, D) > 2) {
                            return ds;
                        }
                    }
                } else if (this.preTiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTiles.next();
                    this.tiles = new DefineBranching2d(ds, 3, minVert, minCurv);
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }

    private class Glue extends IteratorAdapter {
    	final DSymbol ds;
		final int D0;
		final Iterator<Integer> targets;
		final Set seen = new HashSet();

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
		protected Object findNext() throws NoSuchElementException {
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
					if (this.seen.contains(res)) {
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
	 * Generates all minimal, euclidean, 3-dimensional symbols extending a given
	 * 1-dimensional symbol.
	 */
    private class ONE extends IteratorAdapter {
    	Iterator tiles = Iterators.empty();
        Iterator preTilings = Iterators.empty();
        Iterator tilings = Iterators.empty();
        
    	public ONE(final DSymbol face, final int minVert) {
            time4One.start();
    		this.tiles = new SingleFaceTiles(face, minVert);
            time4One.stop();
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected Object findNext() throws NoSuchElementException {
            time4One.start();
            while (true) {
                time4Final.start();
                boolean moreTilings = this.tilings.hasNext();
                time4Final.stop();
                if (moreTilings) {
                    time4Final.start();
                    final DSymbol ds = (DSymbol) this.tilings.next();
                    time4Final.stop();
                    if (isGood(ds)) {
                        time4One.stop();
                        ++countONE;
                        return ds;
                    }
                } else if (this.preTilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTilings.next();
                    if (!hasTrivialVertices(ds)) {
                        time4Final.start();
                        this.tilings = new DefineBranching3d(ds);
                        time4Final.stop();
                    }
                } else if (this.tiles.hasNext()) {
                	final DSymbol ds = (DSymbol) this.tiles.next();
                	this.preTilings = new Glue(ds);
                } else {
                    time4One.stop();
                    throw new NoSuchElementException("at end");
                }
            }
		}
    }
    
    /**
	 * Generates all minimal, euclidean, 3-dimensional symbols containing two
	 * 2-dimensional symbols each made from the single given 1-dimensional one.
	 */
    private class TWO extends IteratorAdapter {
    	final List tiles;
    	int i, j;
        Iterator preTilings = Iterators.empty();
        Iterator tilings = Iterators.empty();
        
    	public TWO(final DSymbol face, final int minVert) {
            time4Two.start();
    		this.tiles = Iterators.asList(new SingleFaceTiles(face, minVert));
    		i = j = 0;
            time4Two.stop();
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected Object findNext() throws NoSuchElementException {
            time4Two.start();
            while (true) {
                time4Final.start();
                boolean moreTilings = this.tilings.hasNext();
                time4Final.stop();
                if (moreTilings) {
                    time4Final.start();
                    final DSymbol ds = (DSymbol) this.tilings.next();
                    time4Final.stop();
                    if (isGood(ds)) {
                        time4Two.stop();
                        ++countTWO;
                        return ds;
                    }
                } else if (this.preTilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTilings.next();
                    if (!hasTrivialVertices(ds)) {
                        time4Final.start();
                        this.tilings = new DefineBranching3d(ds);
                        time4Final.stop();
                    }
                } else if (this.i < this.tiles.size()) {
                	final DSymbol t1 = (DSymbol) this.tiles.get(i);
                	final DSymbol t2 = (DSymbol) this.tiles.get(j);
                	++j;
                	if (j >= this.tiles.size()) {
                		j = ++i;
                	}
                	final DynamicDSymbol ds = new DynamicDSymbol(t1);
                	ds.append(t2);
                	this.preTilings = new Glue(new DSymbol(ds));
                } else {
                    time4Two.stop();
                    throw new NoSuchElementException("at end");
                }
            }
		}
    }
    
    /**
	 * Generates all minimal, euclidean, tile- and face-transitive 3-dimensional
	 * symbols made from two copies of a given 1-dimensional symbol.
	 */
    private class DOUBLE extends IteratorAdapter {
    	Iterator tiles = Iterators.empty();
        Iterator preTilings = Iterators.empty();
        Iterator tilings = Iterators.empty();
        
        final Set testTiles; //@@@ for test purposes only
        
    	public DOUBLE(final DSymbol face, final int minVert) {
    	    if (TEST) {
				final Iterator tiles = new SimpleDoubleFaceTiles(face, minVert);
				testTiles = new HashSet();
				while (tiles.hasNext()) {
					testTiles.add(((DSymbol) tiles.next()));
				}
			} else {
				testTiles = null;
			}
            
            time4Double.start();
    		this.tiles = new DoubleFaceTiles(face, minVert);
            time4Double.stop();
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected Object findNext() throws NoSuchElementException {
            time4Double.start();
            while (true) {
                time4Final.start();
                boolean moreTilings = this.tilings.hasNext();
                time4Final.stop();
                if (moreTilings) {
                    time4Final.start();
                    final DSymbol ds = (DSymbol) this.tilings.next();
                    time4Final.stop();
                    if (isGood(ds)) {
                        time4Double.stop();
                        ++countDOUBLE;
                        return ds;
                    }
                } else if (this.preTilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTilings.next();
                    if (ds.numberOfOrbits(idcsFace3d) == 1
							&& !hasTrivialVertices(ds)) {
                        time4Final.start();
                        this.tilings = new DefineBranching3d(ds);
                        time4Final.stop();
                    }
                } else if (this.tiles.hasNext()) {
                	final DSymbol ds = (DSymbol) this.tiles.next();
                    if (TEST) {
                    	this.testTiles.remove(ds);
                    }
                	this.preTilings = new Glue(ds);
                } else {
                    time4Double.stop();
                    if (TEST) {
						for (final Iterator iter = this.testTiles.iterator(); iter
								.hasNext();) {
							final DSymbol ds = (DSymbol) iter.next();
							System.out.println("#!!! Missed: " + ds);
							final DSymbol base = baseTile(ds);
							final DynamicDSymbol key = new DynamicDSymbol(1,
									base);
							if (((List) doubleBases.get(key)).contains(base)) {
								System.out.println("#!!!   (but base " + base
										+ " found.)");
							}
						}
					}
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
    private int type;
    private Iterator faces = Iterators.empty();
    private Iterator tilings = Iterators.empty();

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
    private Stopwatch time4One = new Stopwatch();
    private Stopwatch time4Two = new Stopwatch();
    private Stopwatch time4Double = new Stopwatch();
    private Stopwatch time4SingleFaced = new Stopwatch();
    private Stopwatch time4DoubleFaced = new Stopwatch();
    private Stopwatch time4BaseSingleFaced = new Stopwatch();
    private Stopwatch time4BaseDoubleFaced = new Stopwatch();
    private Stopwatch time4Final = new Stopwatch();

    public FaceTransitive(final int minSize, final int maxSize,
            final int minVertexDegree) {
    	if (minVertexDegree >= 3 && (maxSize > 20 || maxSize < 1)) {
    		this.maxSize = 20;
    	} else {
    		this.maxSize = maxSize;
    	}
        this.minVert = minVertexDegree;
        
        this.size = minSize - 1;
        this.type = 3;
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.box.collections.IteratorAdapter#findNext()
     */
    protected Object findNext() throws NoSuchElementException {
        while (true) {
            if (this.tilings.hasNext()) {
                return this.tilings.next();
            } else if (this.faces.hasNext()) {
                final DSymbol face = (DSymbol) faces.next();
                switch (this.type) {
                case 0:
                    this.tilings = new ONE(face, this.minVert);
                    break;
                case 1:
                    this.tilings = new TWO(face, this.minVert);
                    break;
                case 2:
                    this.tilings = new DOUBLE(face, this.minVert);
                    break;
                }
            } else if ((this.type < 2 && this.size % 2 == 0) || this.type < 0) {
            	++this.type;
                if (this.type == 0) {
                    this.faces = new Faces(this.size, 3);
                } else {
                    this.faces = new Faces(this.size / 2, 3);
                }
            } else if (this.maxSize < 1 || this.size < this.maxSize) {
                ++this.size;
                this.type = -1;
            } else {
                throw new NoSuchElementException("at end");
            }
        }
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
    private static List baseFaces(final DSymbol face) {
        final List results = new LinkedList();
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
            for (final Iterator iter = new Faces(size, 2); iter.hasNext();) {
                final DSymbol ds = (DSymbol) iter.next();
                if (ds.v(0, 1, ds.elements().next()) == v
                        && ds.isLoopless() == loopless) {
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
    
    public String timeForCaseOne() {
        return this.time4One.format();
    }
    
    public String timeForCaseTwo() {
        return this.time4Two.format();
    }
    
    public String timeForCaseDouble() {
        return this.time4Double.format();
    }
    
    public String timeForSingleFacedTiles() {
        return this.time4SingleFaced.format();
    }
    
    public String timeForDoubleFacedTiles() {
        return this.time4DoubleFaced.format();
    }
    
    public String timeForBaseSingleFacedTiles() {
        return this.time4BaseSingleFaced.format();
    }
    
    public String timeForBaseDoubleFacedTiles() {
        return this.time4BaseDoubleFaced.format();
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
        System.out.println("# Class: FaceTransitive");
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
        final FaceTransitive symbols = new FaceTransitive(minSize, maxSize,
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
        System.out.println("#     Used " + symbols.timeForCaseOne()
                + " for case ONE producing " + symbols.getCountONE()
                + " symbols.");
        System.out.println("#     Used " + symbols.timeForCaseTwo()
                + " for case TWO producing " + symbols.getCountTWO()
                + " symbols.");
        System.out.println("#     Used " + symbols.timeForCaseDouble()
                + " for case DOUBLE producing " + symbols.getCountDOUBLE()
                + " symbols.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForBaseSingleFacedTiles()
				+ " to generate " + symbols.getCountBaseSingleFaced()
				+ " single-faced base tiles.");
        System.out.println("#     Used " + symbols.timeForBaseDoubleFacedTiles()
                + " to generate " + symbols.getCountBaseDoubleFaced()
                + " double-faced base tiles.");
        System.out.println("#     Used " + symbols.timeForSingleFacedTiles()
				+ " to generate " + symbols.getCountSingleFaced()
				+ " single-faced tiles.");
        System.out.println("#     Used " + symbols.timeForDoubleFacedTiles()
                + " to generate " + symbols.getCountDoubleFaced()
                + " double-faced tiles.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForFinalBranching()
                + " to generate final branching.");
        System.out.println("#");
        System.out.println("# Program finished on "
				+ Calendar.getInstance().getTime());
    }
}
