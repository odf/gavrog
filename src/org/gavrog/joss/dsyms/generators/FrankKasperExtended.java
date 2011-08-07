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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;

/**
 * Generates all tile-k-transitive tetrahedra tilings with edge degrees 4, 5 and
 * 6 only.
 * 
 * @author Olaf Delgado
 * @version $Id: FrankKasperExtended.java,v 1.9 2008/04/12 08:07:14 odf Exp $
 */

public class FrankKasperExtended extends TileKTransitive {
	final private static Set<String> interesting_stabilizers =
		new HashSet<String>();
	final private static Set<String> allowed_stabilizer_sets =
		new HashSet<String>();
	static {
		interesting_stabilizers.addAll(Arrays.asList(new String[] {
				"*332", "2*2", "222", "2x", "332"
		}));
		allowed_stabilizer_sets.addAll(Arrays.asList(new String[] {
				"", "*332", "*332/*332", "*332/*332/*332", "*332/*332/*332/*332",
				"*332/*332/2*2", "*332/*332/2*2/2*2", "*332/2*2",
				"*332/2*2/2*2", "*332/2*2/222", "*332/2*2/2x", "*332/222",
				"*332/2x", "2*2", "2*2/2*2", "2*2/2*2/2*2", "2*2/2*2/2*2/2*2",
				"2*2/2*2/2*2/2*2/222", "2*2/2*2/2*2/2*2/222/222",
				"2*2/2*2/2*2/222", "2*2/2*2/2*2/222/222", "2*2/2*2/222",
				"2*2/2*2/222/222", "2*2/2*2/222/2x", "2*2/2*2/2x", "2*2/222",
				"2*2/222/222", "2*2/222/2x", "2*2/2x", "222", "222/222",
				"222/222/222", "222/222/222/222", "222/222/222/222/222",
				"222/222/222/222/222/222", "222/222/222/222/222/222/222",
				"222/222/222/222/222/222/222/222", "222/222/222/222/2x",
				"222/222/222/222/2x/2x", "222/222/222/2x", "222/222/222/2x/2x",
				"222/222/222/332", "222/222/2x", "222/222/2x/2x",
				"222/222/332", "222/222/332/332", "222/2x", "222/2x/2x",
				"222/2x/2x/332", "222/2x/332", "222/332", "222/332/332", "2x",
				"2x/2x", "2x/2x/2x", "2x/2x/2x/2x", "2x/2x/332",
				"2x/2x/332/332", "2x/332", "2x/332/332", "332", "332/332",
				"332/332/332", "332/332/332/332"
		}));
	}

	final private boolean testParts;
	private boolean testVertexFigures = false;
	final private Stopwatch vertexFigureTestingTimer = new Stopwatch();
	final private Stopwatch extraDeductionsTimer = new Stopwatch();
	final private Stopwatch cutsFindingTimer = new Stopwatch();

	public FrankKasperExtended(
			final int k, final boolean verbose, final boolean testParts) {
		super(new DSymbol("1:1,1,1:3,3"), k, verbose);
		this.testParts = testParts;
	}

	protected boolean partsListOkay(final List parts) {
		if (!this.testParts) {
			return true;
		}
		
		final List<String> stabs = new ArrayList<String>();
		
		for (Iterator iter = parts.iterator(); iter.hasNext();) {
            final String type = guessOrbifoldSymbol((DSymbol) iter.next());
            if (interesting_stabilizers.contains(type)) {
            	stabs.add(type);
            }
		}
		Collections.sort(stabs);
		final StringBuffer buf = new StringBuffer(100);
		for (Iterator iter = stabs.iterator(); iter.hasNext();) {
			final String type = (String) iter.next();
			if (buf.length() > 0) {
				buf.append('/');
			}
			buf.append(type);
		}
		return allowed_stabilizer_sets.contains(buf.toString());
	}
	
	private static String guessOrbifoldSymbol(final DSymbol ds) {
		switch (ds.size()) {
		case 1:
			return "*332";
		case 2:
			return "332";
		case 3:
			return "2*2";
		case 4:
			return "*33";
		case 6:
			if (ds.isOriented()) {
				return "222";
			} else if (ds.isLoopless()) {
				return "2x";
			} else {
				return "*22";
			}
		case 8:
			return "33";
		case 12:
			if (ds.isOriented()) {
				return "22";
			} else {
				return "1*";
			}
		case 24:
			return "1";
		default:
			return null;
		}
	}
	
	protected Iterator defineBranching(final DelaneySymbol ds) {
		final DynamicDSymbol out = new DynamicDSymbol(new DSymbol(ds));
		final IndexList idx = new IndexList(0, 2, 3);
		final List<Object> choices = new LinkedList<Object>();
		for (final Iterator reps = out.orbitReps(idx); reps.hasNext();) {
			final Object D = reps.next();
			final Object D0 = out.op(0, D);
			final int r = out.r(2, 3, D);
			if (r == 4 || r == 5) {
				out.redefineV(2, 3, D, 1);
				out.redefineV(2, 3, D0, 1);
			} else if (r == 3 || r == 6) {
				out.redefineV(2, 3, D, 6 / r);
				out.redefineV(2, 3, D0, 6 / r);
			} else if (r == 1 || r == 2) {
				choices.add(D);
			} else {
				throw new RuntimeException("this should not happen: r = " + r
						+ " at D = " + D);
			}
		}
		
		return new IteratorAdapter() {
			final int n = choices.size();
			int count = 0;
			final Set<DSymbol> seen = new HashSet<DSymbol>();
			int a[] = null;

			protected Object findNext() throws NoSuchElementException {
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
					++count;
					final DSymbol res = new DSymbol(out);
					if (res.isLocallyEuclidean3D() && !seen.contains(res)) {
						seen.add(res);
						return res;
					}
				}
			}

			private void choose(final int i, final int m) {
				final Object D = choices.get(i);
				final Object D0 = out.op(0, D);
				final int r = out.r(2, 3, D);
				out.redefineV(2, 3, D, m / r);
				out.redefineV(2, 3, D0, m / r);
				a[i] = m;
			}
		};
	}

    public boolean isComplete(final DelaneySymbol ds) {
        for (final Iterator elms = ds.elements(); elms.hasNext();) {
        	if (!ds.definesOp(3, elms.next())) {
        		return false;
        	}
        }
        for (final Iterator elms = ds.elements(); elms.hasNext();) {
        	final Object D = elms.next();
        	if (!ds.op(1, ds.op(3, D)).equals(ds.op(3, ds.op(1, D)))) {
        		return false;
        	}
        }
        return true;
    }
    
    // override this to introduce extra tests
    protected boolean vertexFigureOkay(final DSymbol ds) {
    	return Utils.mayBecomeLocallyEuclidean3D(ds);
    }
    
	protected Iterator extendTo3d(final DSymbol ds) {
		final List idcs = new IndexList(1, 2, 3);
		
		return new CombineTiles(ds) {
			protected List<Move> getExtraDeductions(final DelaneySymbol ds,
					final Move move) {
				extraDeductionsTimer.start();
				List<Move> out = new ArrayList<Move>();
				final Object D = move.element;
				Object E = D;
				int r = 0;
				cutsFindingTimer.start();
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
				cutsFindingTimer.stop();

				switch (cuts.size()) {
				case 0:
					if (r > 6) {
						out = null;
					} else if (testVertexFigures) {
						vertexFigureTestingTimer.start();
						final Subsymbol sub = new Subsymbol(ds, idcs, D);
						final boolean bad = isComplete(sub)
								&& !vertexFigureOkay(new DSymbol(sub));
						vertexFigureTestingTimer.stop();
						if (bad) {
							out = null;
						}
					}
					break;
				case 1:
					if (r > 6) {
						out = null;
					} else if (r == 6) {
						final Object A = cuts.get(0);
						out.add(new Move(A, A, -1, -1, false, 0));
					}
					break;
				case 2:
					if (r > 12) {
						out = null;
					} else if (r == 12) {
						final Object A = cuts.get(0);
						final Object B = cuts.get(1);
						out.add(new Move(A, B, -1, -1, false, 0));
					}
					break;
				default:
					throw new RuntimeException("this should not happen");
				}
				extraDeductionsTimer.stop();

				return out;
			}
		};
	}

	public boolean getTestVertexFigures() {
		return this.testVertexFigures;
	}

	public void setTestVertexFigures(final boolean extraTests) {
		this.testVertexFigures = extraTests;
	}
	
	public String getTimeForVertexFigureTests() {
		return vertexFigureTestingTimer.format();
	}
	
	public String getTimeForComputingDeductions() {
		return extraDeductionsTimer.format();
	}
	
	public String getTimeForFindingCuts() {
		return cutsFindingTimer.format();
	}
}
