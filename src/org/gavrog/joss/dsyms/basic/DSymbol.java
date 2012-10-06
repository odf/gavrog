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

package org.gavrog.joss.dsyms.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;


/**
 */
public class DSymbol extends DelaneySymbol<Integer> implements Cloneable {
    
    private int dim;
    private int size;
    private int[][] op;
    private int[][] v;
    private String name = null;

    /**
     * Dummy constructor for internal purposes.
     */
    private DSymbol() {
    }
    
    /**
     * Runs consistency checks on the data.
     * 
     * @throws RuntimeException if inconsistencies are found.
     */
    private void assertConsistency() {
        StringBuffer buf = new StringBuffer(100);

        /* --- check sizes */
        if (dim < 0) {
            buf.append("\n : the dimension is negative.");
        }
        if (size < 0) {
            buf.append("\n : the size is negative.");
        }
        if (dim >= 0 && op.length < dim + 1) {
            buf.append("\n : found " + op.length + " neighbors per element,");
            buf.append(" required " + (dim + 1) + ".");
        }
        if (dim >= 0 && v.length < dim) {
            buf.append("\n : found " + v.length + " branching limits per");
            buf.append(" element, required " + (dim + 1) + ".");
        }
        for (int i = 0; i <= dim; ++i) {
            if (op[i].length < size + 1) {
                buf.append("\n : found " + (op[i].length-1) + " neighbors");
                buf.append(" for index " + i + ", required " + size + ".");
            }
            if (i < dim && v[i].length < size + 1) {
                buf.append("\n : found " + (v[i].length-1) + " branching");
                buf.append(" limits for index " + i + ", required " + size);
                buf.append(".");
            }
        }

        /* --- check neighbor operations and explicit braching limits */
        if (buf.length() == 0) {
            for (int i = 0; i <= dim; ++i) {
                for (int D = 1; D <= size; ++D) {
                    int Di = op[i][D];
                    if (Di != 0) {
                        if (Di < 0 || Di > size) {
                            buf.append("\n : op[" + i + "][" + D + "] = ");
                            buf.append(Di + " is outside the allowed range");
                            buf.append(" 1.." + size + ".");
                            Di = 0;
                        } else if (op[i][Di] != D) {
                            int Dx = op[i][Di];
                            buf.append("\n : op[" + i + "][" + D + "] = " + Di);
                            buf.append(", but op[" + i + "][" + Di + "] = ");
                            buf.append(Dx + ".");
                        }
                    }
                    if (i < dim) {
                        int v1 = v[i][D];
                        if (v1 < 0) {
                            buf.append("\n : v[" + i + "][" + D + "] = " + v1);
                            buf.append(" is negative.");
                        } else if (Di >= D) {
                            int v2 = v[i][Di];
                            if (v1 != v2) {
                                buf.append("\n : v[" + i + "][" + D + "] = ");
                                buf.append(v1 + ", but v[" + i + "][" + Di);
                                buf.append("] = " + v2 + ".");
                            }
                        }
                    }
                    if (i > 0 && Di >= D) {
                        int j = i - 1;
                        int v1 = v[j][D];
                        int v2 = v[j][Di];
                        if (v1 != v2) {
                            buf.append("\n : v[" + j + "][" + D + "] = " + v1);
                            buf.append(", but v[" + j + "][" + Di + "] = ");
                            buf.append(v2 + ".");

                        }
                    }
                }
            }
        }
        
        /* --- Check special orbits. */
        if (buf.length() == 0) {
            for (int i = 0; i <= dim; ++i) {
                for (int j = i + 2; j <= dim; ++j) {
                    for (final int D: orbitReps(new IndexList(i, j))) {
                        int E1 = op[j][op[i][D]];
                        int E2 = op[i][op[j][D]];
                        // TODO correct this test
                        if (E1 != E2 && E1 != 0 && E2 != 0) {
                            buf.append("\n : the " + i + "," + j + "-orbit");
                            buf.append(" at " + D + " is too large.");
                        }
                    }
                }
            }
        }

        if (buf.length() > 0) {
            buf.insert(0, "Consistency check failed for " + toString());
            throw new RuntimeException(buf.toString());
        }
    }
    
    /**
     * Constructs a DSymbol from two arrays giving operations and v values.
     * @param op the operation array.
     * @param v the branching array.
     */
    public DSymbol(final int[][] op, final int[][] v) {
    	this.dim = op.length - 1;
    	this.size = op[0].length - 1;
    	this.op = (int[][]) op.clone();
    	this.v = (int[][]) v.clone();
    	assertConsistency();
    }
    
    /**
     * Constructs a new DSymbol from a textual representation.
     * @param code the text specifying the symbol.
     */
    public DSymbol(final String input) {
    	final String code = input.trim();
        int start = 0;
        int end = code.length();
        if (code.startsWith("<")) {
            start++;
        }
        if (code.endsWith(">")) {
            end--;
        }
        String parts[] = code.substring(start, end).split(":");
        final int offset;
        if (parts.length >= 4) {
            offset = parts.length - 3;
        } else {
            offset = 0;
        }
        
        String subparts[] = parts[offset].trim().split("\\s+");
        size = Integer.parseInt(subparts[0]);
        if (subparts.length > 1) {
            dim = Integer.parseInt(subparts[1]);
        } else {
            dim = 2;
        }
        op = new int[dim+1][size+1];
        v = new int[dim][size+1];
        
        if (size == 0 || dim == 0) {
            return;
        }
        
        subparts = parts[offset+1].split(",");
        for (int i = 0; i <= dim; ++i) {
            String entries[] = subparts[i].trim().split("\\s+");
            int k = 0;
            for (int D = 1; D <= size; ++D) {
                if (op[i][D] == 0) {
                    int Di = Integer.parseInt(entries[k++]);
                    if (Di < 0 || Di > size) {
                        final String msg = "illegal " + i + "-image: " + Di;
                        throw new IllegalArgumentException(msg);
                    } else if (op[i][Di] != 0) {
                    	final String msg = "element " + Di + " already has a "
								+ i + "-image";
                        throw new IllegalArgumentException(msg);
                    }
                    op[i][D] = Di;
                    if (Di != 0) {
                    	op[i][Di] = D;
                    }
                }
            }
        }

        subparts = parts[offset+2].split(",");
        for (int i = 0; i < dim; ++i) {
            String entries[] = subparts[i].trim().split("\\s+");
            int k = 0;
            boolean seen[] = new boolean[size+1];
            for (int D = 1; D <= size; ++D) {
                if (seen[D] == false) {
                    int m = Integer.parseInt(entries[k++]);
                    if (m < 0) {
                        String msg = "illegal degree: " + m;
                        throw new IllegalArgumentException(msg);
                    }
                    int r = r(i, i+1, new Integer(D));
                    if (m % r != 0) {
						String msg = "degree " + m + " not a multiple of " + r
								+ " at i=" + i + ", D = " + D;
						throw new IllegalArgumentException(msg);
					}
                    int v = m / r;
                    int Di = D;
                    while (true) {
                        if (op[i][Di] != 0) {
                            Di = op[i][Di];
                        }
                        this.v[i][Di] = v;
                        seen[Di] = true;
                        if (op[i+1][Di] != 0) {
                            Di = op[i+1][Di];
                        }
                        this.v[i][Di] = v;
                        seen[Di] = true;
                        if (Di == D) {
                            break;
                        }
                    }
                }
            }
        }
    	assertConsistency();
    }

    /**
     * Create an instance isomorphic to a given symbol and with the same order
     * of elements and indices.
     * 
     * @param source the symbol to use as a model.
     */
    public <T> DSymbol(final DelaneySymbol<T> source) {
        this.dim = source.dim();
        this.size = source.size();
        
        if (source instanceof DSymbol) {
            this.op = ((DSymbol) source).op;
            this.v = ((DSymbol) source).v;
            return;
        }
        
    	this.op = new int[dim() + 1][size() + 1];
    	this.v = new int[dim()][size() + 1];
    	final List<T> num2elm = new ArrayList<T>();
    	final Map<T, Integer> elm2num = new HashMap<T, Integer>();
    	final int num2idx[] = new int[dim() + 1];
    	
    	num2elm.add(null);
    	for (final T D: source.elements()) {
    	    elm2num.put(D, num2elm.size());
    	    num2elm.add(D);
    	}
    	
    	int k = 0;
    	for (final int i: source.indices()) {
    	    num2idx[k] = i;
    	    ++k;
    	}
        
        for (k = 1; k <= size(); ++k) {
            final T D = num2elm.get(k);
            for (int m = 0; m <= dim(); ++m) {
                final int i = num2idx[m];
                if (source.definesOp(i, D)) {
                    final T E = source.op(i, D);
                    op[m][k] = elm2num.get(E);
                }
            }
        }
    	
        for (int m = 0; m < dim(); ++m) {
            final int i = num2idx[m];
            final int j = num2idx[m+1];
            final IndexList idcs = new IndexList(i, j);
            for (final T D: source.orbitReps(idcs)) {
                if (source.definesV(i, j, D)) {
                    final int vD = source.v(i, j, D);
                    for (final T E: source.orbit(idcs, D)) {
                        v[m][elm2num.get(E)] = vD;
                    }
                }
            }
        }
    	
    	assertConsistency();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(100);
    	if (getName() != null) {
    		buf.append("#@ name ");
    		buf.append(getName());
    		buf.append('\n');
    	}
        buf.append("<1.1:");
        buf.append(size);
        if (dim() != 2) {
            buf.append(" ");
            buf.append(dim);
        }
        buf.append(":");
        
        for (int i = 0; i <= dim; ++i) {
            for (int D = 1; D <= size; ++D) {
                int Di = op[i][D];
                if (Di == 0 || Di >= D) {
                    if (D == 1) {
                        if (i > 0) {
                            buf.append(",");
                        }
                    } else {
                        buf.append(" ");
                    }
                    buf.append(Di);
                }
            }
        }
        buf.append(":");
        
        for (int i = 0; i < dim; ++i) {
            boolean seen[] = new boolean[size+1];
            for (int D = 1; D <= size; ++D) {
                if (!seen[D]) {
                    int m = m(i, i+1, new Integer(D));
                    if (D == 1) {
                        if (i > 0) {
                            buf.append(",");
                        }
                    } else {
                        buf.append(" ");
                    }
                    buf.append(m);
                    int Di = D;
                    while (true) {
                        if (op[i][Di] != 0) {
                            Di = op[i][Di];
                        }
                        seen[Di] = true;
                        if (op[i+1][Di] != 0) {
                            Di = op[i+1][Di];
                        }
                        seen[Di] = true;
                        if (Di == D) {
                            break;
                        }
                    }
                }
            }
        }
        buf.append(">");
        
        return buf.toString();
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#dim()
     */
    public int dim() {
        return dim;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#size()
     */
    public int size() {
        return size;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#elements()
     */
    public IteratorAdapter<Integer> elements() {
        return Iterators.range(1, size() + 1);
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isElement(java.lang.Object)
     */
    public boolean hasElement(final Integer D) {
    	return D >= 1 && D <= size();
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#indices()
     */
    public IteratorAdapter<Integer> indices() {
        return Iterators.range(0, dim() + 1);
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isIndex(int)
     */
    public boolean hasIndex(int i) {
        return i >= 0 && i <= dim();
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#op(int, java.lang.Object)
     */
    public boolean definesOp(final int i, final Integer D) {
		return hasElement(D) && hasIndex(i) && op[i][D] != 0;
	}
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#op(int, java.lang.Object)
     */
    public Integer op(final int i, final Integer D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        return op[i][D];
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#v(int, int, java.lang.Object)
     */
    public boolean definesV(final int i, final int j, final Integer D) {
		return hasElement(D)
				&& hasIndex(i)
				&& hasIndex(j)
				&& (Math.abs(i - j) != 1 || v[Math.min(i, j)][D] != 0);
	}
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#v(int, int, java.lang.Object)
     */
    public int v(final int i, final int j, final Integer D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (!hasIndex(j)) {
            throw new IllegalArgumentException("invalid index: " + j);
        }
        final int val;
        if (j == i+1) {
            val = v[i][D];
        } else if (j == i-1) {
            val = v[j][D];
        } else if (i != j && op(i, D) == op(j, D)) {
            val = 2;
        } else {
            val = 1;
        }
        return normalizedV(val);
    }
    
    /**
     * Produces an identical copy of this symbol.
     * 
     * Since objects of this class are immutable, this is not really needed,
     * but the method may serve as a template to be used in derived classes.
     */
    public Object clone() {
        final DSymbol ds = new DSymbol();
        ds.dim = this.dim;
        ds.size = this.size;
        ds.op = new int[dim+1][size+1];
        ds.v = new int[dim][size+1];
        for (int D = 1; D <= size; ++D) {
            for (int i = 0; i < dim; ++i) {
                ds.op[i][D] = this.op[i][D];
                ds.v[i][D] = this.v[i][D];
            }
            ds.op[dim][D] = this.op[dim][D];
        }
        return ds;
    }
    
    /**
     * Constructs the dual of this symbol.
     * @return the dual symbol.
     */
    public DSymbol dual() {
        final DSymbol ds = new DSymbol();
        ds.dim = this.dim;
        ds.size = this.size;
        ds.op = new int[dim+1][size+1];
        ds.v = new int[dim][size+1];
        for (int D = 1; D <= size; ++D) {
            for (int i = 0; i < dim; ++i) {
                ds.op[i][D] = this.op[dim-i][D];
                ds.v[i][D] = this.v[dim-i-1][D];
            }
            ds.op[dim][D] = this.op[0][D];
        }
        return ds;
    }

	/**
	 * @return the name of this symbol.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Changes the symbol's name.
	 * @param name the new name.
	 */
	public void setName(final String name) {
		this.name = name;
	}
}
