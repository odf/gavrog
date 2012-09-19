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

package org.gavrog.jane.fpgroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.collections.Partition;


/**
 * Implements coset tables for finitely generated subgroups of finitely
 * presented groups. A coset table describes how the group generators act on the
 * cosets defined by the subgroup. Each row corresponds to coset and each column
 * to a group generator or its inverse.
 * 
 * A coset of a subgroup consists of the set of all products of subgroup
 * elements with the same arbitrary group element. For example, the coset S*g
 * consist of all products of the form s*g, where S is a subgroup, s is an
 * element of S and g is an element of the original group G. If g' is another
 * element of G, we define (S*g)*g' := S*(g*g'), i.e., multiplying a coset by
 * some group element is equivalent to multiplying all its members by that
 * element. This operation may or may not produce a different coset.
 * 
 * The set of cosets with respect a subgroup always forms a partition of the set
 * of group elements.
 * 
 * @author Olaf Delgado
 * @version $Id: CosetAction.java,v 1.2 2005/07/18 23:33:29 odf Exp $
 */
public class CosetAction<E, D> implements GroupAction<E, Coset<E, D>> {
    // --- set to true to enable logging
    final private static boolean LOGGING = false;

    // --- maximal number of table rows if not specified by caller
    final public static int DEFAULT_SiZE_LIMIT = 100000;

    // --- the parameters given to the constructor
    final private FpGroup<E> group;
    final private List<FreeWord<E>> subgroupGenerators;
    final private int sizeLimit;
    
    // --- temporary fields used only in the construction process
    final private List<FreeWord<E>> idx2gen;
    final private int idx2invidx[];
    final private int ngens;
    
    // --- results of the construction process
    final private Map<FreeWord<E>, Integer> gen2idx;
    final private List<int[]> table;
    final private int numberOfCosets;
    final FreeWord<E> cosetRepresentatives[];
    
    /**
     * Constructs a CosetTable instance which reflects the action of a group on
     * the cosets of its trivial subgroup, i.e., on the group elements.
     * 
     * @param group the group.
     */
    public CosetAction(final FpGroup<E> group) {
        this(group, new ArrayList<FreeWord<E>>(), DEFAULT_SiZE_LIMIT);
    }

    /**
     * Constructs a CosetTable instance which reflects the action of a group on
     * the cosets of its trivial subgroup, i.e., on the group elements.
     * 
     * @param group the group.
     * @param sizeLimit the limit on the number of coset rows.
     */
    public CosetAction(final FpGroup<E> group, final int sizeLimit) {
        this(group, new ArrayList<FreeWord<E>>(), sizeLimit);
    }

    /**
     * Constructs a CosetTable instance for a subgroup.
     * 
     * @param group the group.
     * @param subgroupGenerators generators of the subgroup.
     */
    public CosetAction(final FpGroup<E> group,
            final List<FreeWord<E>> subgroupGenerators) {
        this(group, subgroupGenerators, DEFAULT_SiZE_LIMIT);
    }
        
    /**
     * Constructs a CosetTable instance with a limit on the number of table rows
     * that may be present at any stage of the construction process. Note that
     * this number may be much higher than the actual number of cosets, because
     * multiple rows may be found correspond to a single coset long after their
     * construction.
     * 
     * @param group the group.
     * @param subgroupGenerators generators of the subgroup.
     * @param sizeLimit the limit on the number of coset rows.
     */
    public CosetAction(final FpGroup<E> group,
            final List<FreeWord<E>> subgroupGenerators,
            final int sizeLimit) {
        
        // --- copy parameters to fields
        this.group = group;
        this.subgroupGenerators = Collections
                .unmodifiableList(subgroupGenerators);
        this.sizeLimit = sizeLimit;
        
        // --- extract generators and relators for group
        final List<FreeWord<E>> groupGenerators = group.getGenerators();
        final List<FreeWord<E>> groupRelators = group.getRelators();
        
        // --- set up translation tables
        this.ngens = 2 * groupGenerators.size();
        this.idx2gen = new ArrayList<FreeWord<E>>();
        this.gen2idx = new HashMap<FreeWord<E>, Integer>();
        this.idx2invidx = new int[this.ngens];

        int nu = 0;
        for (final FreeWord<E> g: groupGenerators) {
            this.idx2gen.add(g);
            this.gen2idx.put(g, nu);
            this.idx2gen.add(g.inverse());
            this.gen2idx.put(g.inverse(), nu+1);
            this.idx2invidx[nu] = nu+1;
            this.idx2invidx[nu+1] = nu;
            nu += 2;
        }

        // --- translate relators
        final List<int[]> rels = new ArrayList<int[]>();
        for (final FreeWord<E> r: groupRelators) {
            final int n = r.length();
            for (int i = 0; i < n; ++i) {
                final FreeWord<E> w = r.subword(i, n).times(r.subword(0, i));
                rels.add(translateWord(w));
                rels.add(translateWord(w.inverse()));
            }
        }
        
        // --- translate subgroup generators
        final List<int[]> subgens = new ArrayList<int[]>();
        for (final FreeWord<E> generator: subgroupGenerators) {
            subgens.add(translateWord(generator));
            subgens.add(translateWord(generator.inverse()));
        }
        
        // --- set up a coset table with one dummy row and one row for the trivial coset
        this.table = new ArrayList<int[]>();
        this.table.add(new int[this.ngens]);
        this.table.add(new int[this.ngens]);
        
        // --- holds classes of rows known to correspond to the same coset
        Partition<Integer> equivalences = new Partition<Integer>();
        
        // --- number of rows made invalid but not yet deleted
        int invalidRows = 0;
        
        // --- scan the table row by row, creating and deleting rows on the fly
        int i = 1;
        while (i < this.table.size()) {
            // --- proceed only if the current row is valid
            if (equivalences.find(i) != i) {
                ++i;
                continue;
            }
            
            // --- scan the current row
            final int row[] = (int[]) this.table.get(i);
            for (int j = 0; j < this.ngens; ++j) {
                // --- proceed only if the current column is empty
                if (row[j] != 0) {
                    continue;
                }
                
                // --- log the result of this construction step
                if (LOGGING) {
                    System.out.println("\ni = " + i + ", j = " + j);
                }
                // --- make a new row for the product with g
                final int newRow[] = new int[this.ngens];
                this.table.add(newRow);
                
                // --- set the correspondences
                final int n = this.table.size();
                row[j] = n-1;
                newRow[this.idx2invidx[j]] = i;
                
                if (LOGGING) {
                    System.out.println("after setting item:");
                    dumpTable();
                }

                // --- scan relations to identify equivalent rows
                final LinkedList<Pair<Integer, Integer>> identify =
                        new LinkedList<Pair<Integer, Integer>>();
                for (final int rel[]: rels) {
                    scanRelation(rel, n-1, identify);
                }
                
                // --- scan subgroup generators
                final int one = equivalences.find(1);
                for (final int gen[]: subgens) {
                    scanRelation(gen, one, identify);
                }

                if (LOGGING) {
                    System.out.println("after scanning:");
                    dumpTable();
                }

                // --- perform pending identifications
                invalidRows += performIdentifications(identify, equivalences);

                if (LOGGING) {
                    System.out.println("after identifying:");
                    dumpTable();
                }

                // --- quit if the limit on the number of rows is reached
                if (this.sizeLimit > 0 && n - invalidRows > this.sizeLimit) {
                    throw new RuntimeException("table limit reached");
                }
                
                // --- compress the table if necessary
                if (invalidRows > n / 2) {
                    final int old2new[] = compressTable(equivalences);
                    equivalences = new Partition<Integer>();
                    i = old2new[i];
                    invalidRows = 0;
                    if (LOGGING) {
                        System.out.println("after compressing:");
                        dumpTable();
                    }

                }
            }
            
            // --- look at next row
            ++i;
        }
        
        // --- make a final cleanup
        compressTable(equivalences);
        if (LOGGING) {
            System.out.println("\nAfter final compression:");
            dumpTable();
        }

        // --- some finishing touches
        this.numberOfCosets = this.table.size() - 1;
        this.cosetRepresentatives = computeCosetRepresentatives();
    }

    /**
     * Writes the current table to standard output. Used for logging.
     */
    private void dumpTable() {
        final StringBuffer buf = new StringBuffer(500);
        for (int i = 1; i < this.table.size(); ++i) {
            final int row[] = (int[]) this.table.get(i);
            for (int j = 0; j < this.ngens; ++j) {
                buf.append(" ");
                buf.append(row[j]);
            }
            buf.append("\n");
        }
        System.out.println(buf);
    }

    /**
     * Takes a word and translates it into the internally used form, an array of
     * integers.
     * @param word the input word.
     * @return the translated word.
     */
    private int[] translateWord(FreeWord<E> word) {
        final int res[] = new int[word.size()];
        for (int i = 0; i < word.size(); ++i) {
            final FreeWord<E> g = word.subword(i, i+1);
            final Integer idx = (Integer) this.gen2idx.get(g);
            res[i] = idx.intValue();
        }
        return res;
    }

    /**
     * Scans a given relation to identify pairs of rows which represent the
     * same coset or make deductions about table entries.
     * 
     * @param rel the relation to scan for (in internal form).
     * @param start the start row to scan from.
     * @param identify a list to add equivalent row pairs to.
     */
    private void scanRelation(final int rel[], final int start,
            final LinkedList<Pair<Integer, Integer>> identify) {

        int head = start;
        int headPos;
        
        // --- forward scan
        for (headPos = 0; headPos < rel.length; ++headPos) {
            final int g = rel[headPos];
            final int next = ((int[]) this.table.get(head))[g];
            if (next == 0) {
                break;
            } else {
                head = next;
            }
        }
        
        int tail = start;
        int tailPos;
        
        // --- backward scan
        for (tailPos = rel.length - 1; tailPos >= headPos; --tailPos) {
            final int g = this.idx2invidx[rel[tailPos]];
            final int next = ((int[]) this.table.get(tail))[g];
            if (next == 0) {
                break;
            } else {
                tail = next;
            }
        }

        if (tailPos == headPos) {
            // --- we can make a deduction
            final int g = rel[headPos];
            final int tailRow[] = (int[]) this.table.get(tail);
            final int headRow[] = (int[]) this.table.get(head);
            headRow[g] = tail;
            tailRow[this.idx2invidx[g]] = head;
        } else if (tailPos < headPos && head != tail) {
            // --- identify start and end rows
            final Pair<Integer, Integer> pair =
                    new Pair<Integer, Integer>(head, tail);
            identify.addLast(pair);
        }
    }
    
    /**
     * Merges pairs of rows by making them identical and marking them as
     * equivalent. For each merged pair, also merges each pair of not yet merged
     * images by the same generator, and so on recursively, until no more pairs
     * need to be merged.
     * 
     * @param Q a list of row pairs to merge.
     * @param P the row equivalence classes (modified by this method).
     * @return the number of individual merges performed.
     */
    private int performIdentifications(
            final LinkedList<Pair<Integer, Integer>> Q,
            final Partition<Integer> P) {
        int count = 0;
        while (Q.size() > 0) {
            final Pair<Integer, Integer> pair = Q.removeFirst();
            final int a = P.find(pair.getFirst());
            final int b = P.find(pair.getSecond());
            if (a == b) {
                continue;
            }
            P.unite(a, b);
            ++count;
            
            final int row_a[] = (int[]) this.table.get(a);
            final int row_b[] = (int[]) this.table.get(b);
            for (int g = 0; g < this.ngens; ++g) {
                final int ag = row_a[g];
                final int bg = row_b[g];
                if (ag == 0) {
                    row_a[g] = bg;
                } else if (bg == 0) {
                    row_b[g] = ag;
                } else if (!P.areEquivalent(ag, bg)) {
                    Q.addLast(new Pair<Integer, Integer>(ag, bg));
                }
            }
            this.table.set(b, row_a);
        }
        
        return count;
    }

    /**
     * Compresses the table by collapsing each set of rows tagged as equivalent,
     * i.e., representing the same coset, into a single row. At this stage,
     * equivalent rows are expected to have equal contents, as should have been
     * established by {@link #performIdentifications(LinkedList, Partition<Integer>)}.
     * 
     * @param P a partition of the row set into equivalence classes.
     * @return a mapping of old to new row numbers.
     */
    private int[] compressTable(final Partition<Integer> P) {
        // --- initialize the mapping from old to new row numbers
        final int old2new[] = new int[this.table.size()];
        // --- initialize the new coset table
        final LinkedList<int[]> newTable = new LinkedList<int[]>();
        // --- the row with number 0 is not used
        newTable.add(this.table.get(0));

        // --- collect the rows for the new table and establish the mapping
        for (int i = 1; i < this.table.size(); ++i) {
            // --- get the representative for i's equivalence class
            final int ri = P.find(i);
            // --- use only the first row in each equivalence class
            if (old2new[ri] == 0) {
                old2new[ri] = newTable.size();
                newTable.add(this.table.get(ri));
            }
            // --- map i to the same row number as its representative
            old2new[i] = old2new[ri];
        }
        
        // --- copy the new table into the old one
        this.table.clear();
        while (newTable.size() > 0) {
            this.table.add(newTable.removeFirst());
        }
        
        // --- finally, translate all entries into the new numbering scheme
        for (int i = 1; i < this.table.size(); ++i) {
            final int row[] = (int[]) this.table.get(i);
            for (int j = 0; j < this.ngens; ++j) {
                row[j] = old2new[row[j]];
            }
        }
        
        // --- return the mapping from old to new row numbers
        return old2new;
    }

    /**
     * Computes a shortest representative for each coset.
     * @return the array of representatives.
     */
    private FreeWord<E>[] computeCosetRepresentatives() {
        final int n = size();
        @SuppressWarnings("unchecked")
        final FreeWord<E> reps[] = new FreeWord[n+1];
        final LinkedList<Integer> Q = new LinkedList<Integer>();
        reps[1] = new FreeWord<E>(getGroup().getAlphabet());
        Q.addLast(new Integer(1));
        while (Q.size() > 0) {
            final int i = ((Integer) Q.removeFirst()).intValue();
            final int[] row = (int[]) this.table.get(i);
            for (int column = 0; column < ngens; ++column) {
                final int next = row[column];
                if (reps[next] == null) {
                    reps[next] = reps[i].times(this.idx2gen.get(column));
                    Q.addLast(new Integer(next));
                }
            }
        }
        
        return reps;
    }
    
    /**
     * Returns the value of sizeLimit.
     * @return the current value of sizeLimit.
     */
    public int getSizeLimit() {
        return this.sizeLimit;
    }
    
    /**
     * Returns the value of subgroupGenerators.
     * @return the current value of subgroupGenerators.
     */
    public List<FreeWord<E>> getSubgroupGenerators() {
        return this.subgroupGenerators;
    }
    
    /**
     * Retrieves the coset containing the trivial word.
     * @return the coset containing the trivial word.
     */
    public Coset<E, D> getTrivialCoset() {
        return new Coset<E, D>(this, 1);
    }
    
    /**
     * Retrieves the coset containing a specific word.
     * @param word the word.
     * @return the coset containing the word.
     */
    public Coset<E, D> getCoset(final FreeWord<E> word) {
        return apply(getTrivialCoset(), word);
    }
    
    /**
     * Retrieves the coset containing a specific word.
     * @param word the word.
     * @return the coset containing the word.
     */
    public Coset<E, D> getCoset(final String word) {
        return getCoset(FreeWord.parsedWord(getGroup().getAlphabet(), word));
    }
    
    // --- implementation of the GroupAction interface starts here.
    
    public FpGroup<E> getGroup() {
        return this.group;
    }
    
    public Iterator<Coset<E, D>> domain() {
        return new FilteredIterator<Coset<E, D>, Integer>(
                Iterators.range(1, size() + 1)) {
            public Coset<E, D> filter(final Integer x) {
                return new Coset<E, D>(CosetAction.this, x);
            }
        };
    }

    public Coset<E, D> apply(final Coset<E, D> x, final FreeWord<E> w) {
        if (x.getAction() != this) {
            return null;
        }
        int current = x.getIndex();
        if (current < 1 || current > size()) {
            return null;
        }
        for (int i = 0; i < w.length(); ++i) {
            final Integer j = (Integer) this.gen2idx.get(w.subword(i, i+1));
            if (j == null) {
                return null;
            } else {
                final int row[] = (int[]) this.table.get(current);
                current = row[j.intValue()];
            }
        }
        return new Coset<E, D>(this, current);
    }

    public boolean isDefinedOn(final Coset<E, D> x) {
        return x.getAction() == this;
    }
    
    public int size() {
        return this.numberOfCosets;
    }
}
