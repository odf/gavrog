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


/**
 * Enumerates all non-isomorphic transitive actions of a given group on small
 * sets up to a a given size. By computing the stabilizers of the resulting
 * actions, one obtains all conjugacy classes of subgroups of the group.
 */
public class SmallActionsIterator<E>
extends IteratorAdapter<GroupAction<E, Integer>> {
    // --- set to true to enable logging
    final private static boolean LOGGING = false;

    // --- input parameters
    final private FpGroup<E> group;
    final private int maxSize;
    final private boolean normalOnly;
    
    // --- convenience data
    final private int ngens;
    final private Map<FreeWord<E>, Integer> gen2idx;
    final private int[] idx2invidx;
    final private List<Set<int[]>> relatorsByStartGen;
    
    // --- the action table and trial stack
    final private int[][] table;
    final private LinkedList<Move> stack;
    
    // --- info on the current state
    private int currentNumberOfRows;
    
    // --- time limit for this iterator
    private long startTime;
    
    // --- choices made so far and choice limit
    private long choicesSoFar = 0;
    private long choiceLimit = Long.MAX_VALUE;
    
    /**
     * The instances of this class represent individual moves of setting
     * table entries. These become the entries of the trial stack.
     */
    private class Move {
        final public int row;
        final public int column;
        final public int value;
        final public boolean startsRow;
        final public boolean isChoice;
        
        public Move(final int row, final int column, final int value,
                final boolean newRow, final boolean isChoice) {
            this.row = row;
            this.column = column;
            this.value = value;
            this.startsRow = newRow;
            this.isChoice = isChoice;
        }
        
        public String toString() {
            return "Move(" + row + ", " + column + ", " + value + ", "
                   + startsRow + ", " + isChoice + ")";
        }
    }
    
    /**
     * Constructs and initializes a new instance.
     * 
     * @param group the group to construct actions of.
     * @param maxSize the maximal size of the set on which to act.
     * @param normalOnly if true, require that each set element has the same
     *            stabilizer. The stabilizer is then a normal subgroup.
     */
    public SmallActionsIterator(final FpGroup<E> group, final int maxSize,
            final boolean normalOnly) {
        // --- copy the input parameters to fields
        this.group = group;
        this.maxSize = maxSize;
        this.normalOnly = normalOnly;
        
        // --- extract some information from the group
        final List<FreeWord<E>> groupGenerators = group.getGenerators();
        final List<FreeWord<E>> groupRelators = group.getRelators();
        this.ngens = 2 * groupGenerators.size();

        // --- set up translation tables
        this.gen2idx = new HashMap<FreeWord<E>, Integer>();
        this.idx2invidx = new int[this.ngens];

        int nu = 0;
        for (final FreeWord<E> g: groupGenerators) {
            this.gen2idx.put(g, new Integer(nu));
            this.gen2idx.put(g.inverse(), new Integer(nu+1));
            this.idx2invidx[nu] = nu+1;
            this.idx2invidx[nu+1] = nu;
            nu += 2;
        }

        // --- translate relators
        this.relatorsByStartGen = new LinkedList<Set<int[]>>();
        for (int k = 0; k <= this.ngens; ++k) {
            this.relatorsByStartGen.add(new HashSet<int[]>());
        }
        for (final FreeWord<E> rel: groupRelators) {
            for (int exp = -1; exp <= 1; exp += 2) {
                final FreeWord<E> r = rel.raisedTo(exp);
                final int n = r.length();
                for (int i = 0; i < n; ++i) {
                    final FreeWord<E> w = r.subword(i, n).times(r.subword(0, i));
                    final FreeWord<E> g = w.subword(0, 1);
                    final int k = this.gen2idx.get(g);
                    this.relatorsByStartGen.get(k).add(translateWord(w));
                }
            }
        }
        
        // --- initialize the action table and trial stack
        this.table = new int[maxSize+1][ngens];
        this.stack = new LinkedList<Move>();
        this.currentNumberOfRows = 1;
        
        // --- push a dummy move (see documentation for findNext() below)
        this.stack.addLast(new Move(1, 0, 0, false, true));
        
        // --- initialize counts and timers
        this.startTime = System.currentTimeMillis();
        this.choicesSoFar = 0;
    }

    /**
     * Sets the maximum number of calls to performMove() this instance may make.
     * @param choiceLimit the new limit.
     */
    public void setMaximalNumberOfChoices(final long choiceLimit) {
        this.choiceLimit = choiceLimit;
    }
    
    /**
     * Repeatedly finds the next legal choice in the enumeration tree and
     * executes it, together with all its implications, until the table is
     * completely filled and in canonical form, in which case the corresponding
     * action is returned.
     * 
     * Does appropriate backtracking in order to find the respective next
     * choice. Also backtracks if the partial group action resulting from the
     * latest choice is not in canonical form.
     * 
     * To simplify the code, the algorithm makes use of "dummy moves", which are
     * put on the stack as fallback entries but do not have any effect on the
     * table. A dummy move is of the form
     * <code>Move(row, column, 0, false, false)</code> and effectively
     * indicates that the next entry to be set is at the given row and column.
     * 
     * @return the next action, if any.
     */
    protected GroupAction<E, Integer> findNext() throws NoSuchElementException {
        if (LOGGING) {
            System.out.println("findNext(): stack size = " + this.stack.size());
        }
        while (true) {
            final Move choice = undoLastChoice();
            if (LOGGING) {
                System.out.println("  last choice was " + choice);
                System.out.println("  table after undo:");
                dumpTable();
            }
            if (choice == null) {
                throw new NoSuchElementException("At end.");
            }
            final int invCol = this.idx2invidx[choice.column];
            final int nrows = this.currentNumberOfRows;
            int nextValue = choice.value + 1;
            while (nextValue <= nrows && this.table[nextValue][invCol] != 0) {
                ++nextValue;
            }
            if (nextValue > nrows) {
                if (choice.startsRow || nextValue > this.maxSize) {
                    continue;
                }
            }
            if (LOGGING) {
                System.out.println("  found free value " + nextValue);
            }
            if (performMove(choice.row, choice.column, nextValue)) {
                if (LOGGING) {
                    System.out.println("  new table after move:");
                    dumpTable();
                }
                if (tableIsCanonical()) {
                    if (!findNextChoice(choice.row, choice.column)) {
                        if (!this.normalOnly || isNormal()) {
                            return constructAction();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Performs a move with all its implications. This includes setting the
     * table entries described by the move, pushing the move on the stack
     * and as well performing all the deduced moves as dictated by the
     * relations of the underlying group.
     * 
     * @param row the row of the table entry to set.
     * @param column the column of the table entry to set.
     * @param value the new target row.
     * @return true if the move did not lead to a contradiction.
     */
    private boolean performMove(int row, int column, int value) {
        if (++this.choicesSoFar >= this.choiceLimit) {
            throw new ChoiceLimitExceededException("too many choices made");
        }

        final boolean startsRow = value > this.currentNumberOfRows;
        final LinkedList<Move> queue = new LinkedList<Move>();
        queue.addLast(new Move(row, column, value, startsRow, true));
        
        while (queue.size() > 0) {
            final Move move = (Move) queue.removeFirst();
            this.stack.addLast(move);
            this.table[move.row][move.column] = move.value;
            this.table[move.value][this.idx2invidx[move.column]] = move.row;
            if (move.startsRow) {
                ++this.currentNumberOfRows;
            }
            
            final Set<int[]> rels = this.relatorsByStartGen.get(move.column);
            for (final int relator[]: rels) {
                final Move next = scanRelation(relator, move.row);
                if (next == null) {
                    // --- nothing to do
                } else if (next.isChoice) {
                    // --- a contraction was found
                    if (LOGGING) {
                        System.out.println("  found contraction " + next
                                + " by relation " + dumpRelation(relator)
                                + " on table:");
                        dumpTable();
                    }
                    return false;
                } else {
                    // --- a valid deduction was found
                    if (LOGGING) {
                        System.out.println("  found deduction " + next
                                + " by relation " + dumpRelation(relator)
                                + " on table:");
                        dumpTable();
                    }
                    queue.addLast(next);
                }
            }
        }
        return true;
    }

    /**
     * Scans a given relation to make deductions about table entries. If a
     * reduction is found, a {@link SmallActionsIterator.Move}object encoding
     * it is returned. As a special case, a {@link SmallActionsIterator.Move}
     * object tagged as a choice is returned to indicate that a contradiction
     * was found during scanning.
     * 
     * @param rel the relation to scan for (in internal form).
     * @param start the start row to scan from.
     * 
     * @return a move object encoding the deduction found if any
     */
    private Move scanRelation(final int rel[], final int start) {
        int head = start;
        int headPos;
        
        // --- forward scan
        for (headPos = 0; headPos < rel.length; ++headPos) {
            final int next = table[head][rel[headPos]];
            if (next == 0) {
                break;
            } else {
                head = next;
            }
        }

        int tail = start;
        int tailPos;
        
        // --- backward scan
        for (tailPos = rel.length-1; tailPos >= headPos; --tailPos) {
            final int next = table[tail][this.idx2invidx[rel[tailPos]]];
            if (next == 0) {
                break;
            } else {
                tail = next;
            }
        }

        // --- evaluate the results
        if (tailPos == headPos) {
            // --- we can make a deduction
            return new Move(head, rel[headPos], tail, false, false);
        } else if (tailPos < headPos && head != tail) {
            // --- signal a contradiction
            return new Move(head, 0, tail, false, true);
        } else {
            // --- no conclusions
            return null;
        }
    }
    
    /**
     * Checks if the current table is in canonical form by comparing it with the
     * tables that would result from putting other rows in first place. For each
     * such choice, there is a unique resulting table which is then compared
     * lexicographically, row by row, with the present one. Undefined table
     * entries, as indicated by a 0, are considered larger than any other
     * entries. The current table is canonical only if it stays the smallest in
     * this comparison.
     * 
     * @return true if table is in canonical form.
     */
    private boolean tableIsCanonical() {
        final int n = this.currentNumberOfRows;
        for (int start = 2; start <= n; ++start) {
            if (compareStart(start) < 0) {
                if (LOGGING) {
                    System.out.println("better start " + start + " for table:");
                    dumpTable();
                }
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the stabilizer of the current action is a normal subgroup.
     * 
     * @return true if the stabilizer is normal.
     */
    private boolean isNormal() {
        final int n = this.currentNumberOfRows;
        for (int start = 2; start <= n; ++start) {
            if (compareStart(start) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares the current table with the one that would result from
     * renumbering the table rows such that the start row is as given and the
     * further numbering is as the algorithm would produce it, i.e., such that
     * row numbers larger than 1 appear in the table in consecutive order. The
     * result is negative if the renumbered table would be lexicographically
     * smaller, zero if it would be equal and positive if it would be larger
     * than the current table. An undefined entry, indicated by a 0, is
     * considered larger than any other entry.
     * 
     * @param start the new start row.
     * @return the result encoded as an integer.
     */
    private int compareStart(final int start) {
        final int n = this.currentNumberOfRows;
        final int old2new[] = new int[n + 1];
        final int new2old[] = new int[n + 1];
        int rowsSeen = 1;
        old2new[start] = 1;
        new2old[1] = start;
        for (int row = 1; row <= n; ++row) {
            assert row <= rowsSeen : "the current action is not transitive";
            for (int col = 0; col < this.ngens; ++col) {
                final int oldVal = this.table[row][col];
                int newVal = this.table[new2old[row]][col];
                if (newVal != 0 && old2new[newVal] == 0) {
                    old2new[newVal] = ++rowsSeen;
                    new2old[rowsSeen] = newVal;
                }
                newVal = old2new[newVal];
                if (newVal != oldVal) {
                    if (oldVal == 0) {
                        return -1;
                    } else if (newVal == 0){
                        return 1;
                    } else {
                        return newVal - oldVal;
                    }
                }
            }
        }

        return 0;
    }

    /**
     * Finds the next empty slot in the table, starting from the given row and
     * column and proceeding row by row. If an empty slot is found, a dummy move
     * (see {@link #findNext()}above) is generated and placed on the stack.
     * 
     * @param row the row to start searching at.
     * @param column the column to start searching at.
     * @return true if an empty slot was found.
     */
    private boolean findNextChoice(final int row, final int column) {
        int newRow = row;
        int newCol = column;
        
        do {
            if (++newCol >= this.ngens) {
                newCol = 0;
                if (++newRow > this.currentNumberOfRows) {
                    return false;
                }
            }
        } while (this.table[newRow][newCol] != 0);
        
        this.stack.addLast(new Move(newRow, newCol, 0, false, true));
        return true;
    }

    /**
     * Undoes the last choice and all its implications by popping moves from the
     * stack until one is found which is a choice. The corresponding table
     * entries are cleared and the last choice is returned. If there was no
     * choice left on the stack, a <code>null</code> result is returned.
     * 
     * @return the last choice or null.
     */
    private Move undoLastChoice() {
        Move last;
        do {
            if (stack.size() == 0) {
                return null;
            }
            last = (Move) this.stack.removeLast();
            this.table[last.row][last.column] = 0;
            this.table[last.value][this.idx2invidx[last.column]] = 0;
            if (last.startsRow) {
                --this.currentNumberOfRows;
            }
        } while (!last.isChoice);

        return last;
    }
    
    /**
     * Takes a word and translates it into the internally used form, an array of
     * integers.
     * 
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
     * Constructs and returns a {@link GroupAction} instance from the current
     * table, which must be complete.
     * 
     * @return the constructed instance.
     */
    private GroupAction<E, Integer> constructAction() {
        final int size = this.currentNumberOfRows;
        final int[][] table = new int[size+1][this.ngens];
        for (int i = 0; i <= size; ++i) {
            table[i] = (int[]) this.table[i].clone();
        }
        
        return new GroupAction<E, Integer>() {
            public FpGroup<E> getGroup() {
                return SmallActionsIterator.this.group;
            }
            
            public Iterator<Integer> domain() {
                return Iterators.range(1, size + 1);
            }

            public Integer apply(final Integer x, final FreeWord<E> w) {
                int current = x;
                if (current < 1 || current > size) {
                    return null;
                }
                for (int i = 0; i < w.length(); ++i) {
                    final FreeWord<E> g = w.subword(i, i+1);
                    final Integer k = SmallActionsIterator.this.gen2idx.get(g);
                    if (k == null) {
                        return null;
                    } else {
                        current = table[current][k];
                    }
                }
                return current;
            }

            public boolean isDefinedOn(final Integer x) {
            	return x >= 1 && x <= size;
            }
            
            public int size() {
                return size;
            }
        };
    }
    
    
    /**
     * Writes the current table to standard output. Used for logging.
     */
    private void dumpTable() {
        final StringBuffer buf = new StringBuffer(500);
        for (int i = 1; i < this.table.length; ++i) {
            final int row[] = this.table[i];
            for (int j = 0; j < this.ngens; ++j) {
                buf.append(" ");
                buf.append(row[j]);
            }
            buf.append("\n");
        }
        System.out.println(buf);
    }

    /**
     * Returns a string representation of an int array. Used for logging.
     * @param rel the int array.
     * @return the string representation.
     */
    private String dumpRelation(final int[] rel) {
        final StringBuffer buf = new StringBuffer(500);
        buf.append("[");
        for (int i = 0; i < rel.length; ++i) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(rel[i]);
        }
        buf.append("]");
        return buf.toString();
    }
    
    /**
     * Returns the value of countChoices.
     * @return the current value of countChoices.
     */
    public long getChoicesSoFar() {
        return this.choicesSoFar;
    }
    
    /**
     * Returns the time elapsed since this instance was created.
     * @return the elapsed time in milliseconds.
     */
    public long getTimeElapsed() {
        return System.currentTimeMillis() - this.startTime;
    }
    
    public static void main(final String args[]) {
        final int index = Integer.parseInt(args[0]);
        final FiniteAlphabet<String> A =
        		new FiniteAlphabet<String>(new String[] { "a", "b", "c" });
        final FpGroup<String> G = new FpGroup<String>(A,
        		new String[] { "[a,b]", "[a,c]", "[b,c]" });
        final SmallActionsIterator<String> iter =
        		new SmallActionsIterator<String>(G, index, false);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            ++count;
        }
        System.out.println(count + " subgroup classes found in "
        		+ iter.getTimeElapsed() / 1000.0 + " second.");
    }
}
