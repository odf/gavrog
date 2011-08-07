/*
   Copyright 2006 Olaf Delgado-Friedrichs

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;

/**
 * @author Olaf Delgado
 */
public class Azulenoids extends IteratorAdapter {

	private Iterator sets;
	private DefineBranching2d syms;
	private int pos;
	private DSymbol ds;
	private Set seenInvariants;

	private int nrOctaSets = 0;
	private int nrOctaSyms = 0;
	private int nrAzulSyms = 0;
	
	private boolean trace = false;
	
	private static DSymbol template = new DSymbol("1.1:60:"
			+ "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48 "
			+ "50 52 54 56 58 60,"
			+ "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 33 35 42 39 41 48 45 47 "
			+ "54 51 53 60 57 59,"
			+ "0 0 12 11 28 27 0 0 18 17 36 35 24 23 58 57 30 29 0 0 0 0 42 41 "
			+ "0 0 48 47 0 0 54 53 0 0 60 59 0 0:"
			+ "3 3 3 3 3 3 3 3 3 3,0 0 5 0 7 0 0 0 0 0");
	
	/**
	 * Constructs an instance.
	 */
	public Azulenoids() {
		super();
    	final int n = 8;
    	final DynamicDSymbol ds = new DynamicDSymbol(1);
    	ds.grow(2 * n);
    	for (int D = 1; D < 2*n + 1; D += 2) {
    		ds.redefineOp(0, new Integer(D), new Integer(D+1));
    		if (D > 1) {
    			ds.redefineOp(1, new Integer(D), new Integer(D-1));
    		}
    	}
    	ds.redefineOp(1, new Integer(1), new Integer(2*n));
    	ds.redefineV(0, 1, new Integer(1), 1);
    	
        this.sets = new CombineTiles(ds);
        this.syms = null;
        this.pos = 0;
        this.seenInvariants = new HashSet();
	}

	/* (non-Javadoc)
	 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
	 */
	protected Object findNext() throws NoSuchElementException {
		while (true) {
			// --- if necessary, find the next octagon tiling to subdivide
			if (this.pos < 1 || this.pos > 16) {
				while (this.syms == null || !this.syms.hasNext()) {
	                if (this.sets.hasNext()) {
	                    final DSymbol ds = (DSymbol) this.sets.next();
	                    ds.setVDefaultToOne(true);
	                    final Rational curv = ds.curvature2D();
	                    ds.setVDefaultToOne(false);
	                    if (curv.isNegative()) {
	                    	continue;
	                    }
	                    ++this.nrOctaSets;
	                    this.syms = new DefineBranching2d(ds, 3, 2, Whole.ZERO);
	                } else {
	                    throw new NoSuchElementException("At end");
	                }
				}
				this.ds = (DSymbol) syms.next();
				if (!this.ds.curvature2D().isZero()) {
					continue;
				}
				++this.nrOctaSyms;
				if (trace) {
					System.out.println("# Using octagon tiling " + this.ds);
				}
				this.pos = 1;
			}
			final int p = this.pos;
			this.pos += 2;
			
			// --- perform the subdivision if it is legal
			final DynamicDSymbol tmp = new DynamicDSymbol(template);

			// --- map template chambers to octagon chambers
			final Map tmp2oct = new HashMap();
			final Map oct2tmp = new HashMap();
			final Object E0 = new Integer(1);
			Object E = E0;
			int k = (3 - p + 16) % 16 + 1;
			do {
				tmp2oct.put(E, new Integer(k));
				oct2tmp.put(new Integer(k), E);
				E = tmp.op(0, E);
				k = k % 16 + 1;
				tmp2oct.put(E, new Integer(k));
				oct2tmp.put(new Integer(k), E);
				E = tmp.op(1, tmp.op(2, tmp.op(1, E)));
				if (tmp.definesOp(2, E)) {
					E = tmp.op(1, tmp.op(2, E));
				}
				k = k % 16 + 1;
			} while (!E0.equals(E));

			// --- complete the template based on the octagon tiling
			for (final Iterator iter = tmp.elements(); iter.hasNext();) {
				final Object D = iter.next();
				if (!tmp.definesOp(2, D)) {
					tmp.redefineOp(2, D, oct2tmp.get(this.ds.op(2, tmp2oct.get(D))));
				}
			}
			for (final Iterator iter = tmp2oct.keySet().iterator(); iter.hasNext();) {
				final Object D = iter.next();
				if (!tmp.definesV(1, 2, D)) {
					tmp.redefineV(1, 2, D, this.ds.v(1, 2, tmp2oct.get(D)));
				}
			}

			final DSymbol result = new DSymbol(tmp);
			final List key = result.minimal().invariant();
			if (!this.seenInvariants.contains(key)) {
				this.seenInvariants.add(key);
				++nrAzulSyms;
				return result.dual().minimal().canonical();
			}
		}
	}


	public int getNrAzulSyms() {
		return this.nrAzulSyms;
	}

	public int getNrOctaSets() {
		return this.nrOctaSets;
	}

	public int getNrOctaSyms() {
		return this.nrOctaSyms;
	}
	
	public boolean getTrace() {
		return this.trace;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Azulenoids azul = new Azulenoids();
		azul.setTrace(true);
		while (azul.hasNext()) {
			System.out.println(azul.next());
		}
		System.out.println("#Generated:");
		System.out.println("#    " +
				azul.getNrOctaSets() + " octagonal D-sets.");
		System.out.println("#    " +
				azul.getNrOctaSyms() + " octagonal D-symbols.");
		System.out.println("#    " +
				azul.getNrAzulSyms() + " azulenoid D-symbols.");
	}
}
