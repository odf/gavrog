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

package org.gavrog.box.collections;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A collection of methods that return special iterators.
 */
final public class Iterators {
    /**
     * No instances for this class.
     */
    private Iterators() {}

    /**
     * Constructs an iterator over the empty set.
     * 
     * @return the newly constructed iterator.
     */
    public static <E> IteratorAdapter<E> empty() {
        return new IteratorAdapter<E>() {
            protected E findNext() throws NoSuchElementException {
                throw new NoSuchElementException("at end");
            }
        };
    }
    
    /**
     * Constructs an iterator over a single object.
     * @param x the object.
     * @return the newly constructed iterator.
     */
    public static <E> IteratorAdapter<E> singleton(final E x) {
        return new IteratorAdapter<E>() {
            private boolean done = false;
            
            protected E findNext() throws NoSuchElementException {
                if (done) {
                    throw new NoSuchElementException("at end");
                } else {
                    done = true;
                    return x;
                }
            }
            
        };
    }
    
    /**
     * Constructs an iterator over a range of integers.
     * @param start the smallest element in the range.
     * @param end the smallest element above the range.
     * @return the newly constructed iterator.
     */
    public static IteratorAdapter<Integer> range(final int start, final int end) {
        return new IteratorAdapter<Integer>() {
            private int next = start;

            protected Integer findNext() throws NoSuchElementException {
                if (next >= end) {
                    throw new NoSuchElementException("at end");
                } else {
                    return next++;
                }
            }           
        };
    }
    
    /**
     * Constructs an iterator over the set of all pairs of elements in the
     * domains of two given iterators. One or both of the individual iterators
     * may be over an infinite domain as, e.g., the natural numbers.
     * Accordingly, a Cantor diagonalization scheme is used to ensure that each
     * pair is reached after a finite number of steps. As an example, the
     * product of two iterators over the natural numbers would look as follows:
     * (1,1), (1,2), (2,1), (1,3), (2,2), (3,1), ...
     * 
     * Be aware that the constructed iterator uses an internal cache which may,
     * in the worst case, grow linearly with the number of steps taken.
     * 
     * @param a the first source iterator.
     * @param b the second source iterator.
     * @return the newly constructed iterator.
     */
    public static <A, B> IteratorAdapter<Pair<A, B>>
    cantorProduct(final Iterator<A> a, final Iterator<B> b)
    {
        if (a == b) {
            throw new UnsupportedOperationException("identical iterators");
        }
        
        return new IteratorAdapter<Pair<A, B>>() {
            final LinkedList<A> cacheA = new LinkedList<A>();
            final LinkedList<B> cacheB = new LinkedList<B>();
            Iterator<A> iterA = empty();
            Iterator<B> iterB = empty();

            protected Pair<A, B> findNext() throws NoSuchElementException {
                if (!iterA.hasNext()) {
                    if (a.hasNext()) {
                        cacheA.addLast(a.next());
                    } else {
                        cacheB.removeLast();
                    }
                    if (b.hasNext()) {
                        cacheB.addFirst(b.next());
                    } else {
                        cacheA.removeFirst();
                    }
                    iterA = cacheA.iterator();
                    iterB = cacheB.iterator();
                }
                if (iterA.hasNext() && iterB.hasNext()) {
                    return new Pair<A, B>(iterA.next(), iterB.next());
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
        };
    }
    
    /**
     * Constructs an iterator over all permutations of an array.
     * 
     * @param things the things to permute.
     * @return the newly constructed iterator.
     */
    public static <E> IteratorAdapter<List<E>> permutations(final E things[]) {
        final int n = things.length;
        
        return new IteratorAdapter<List<E>>() {
            private int a[] = null;
            
            protected List<E> findNext() throws NoSuchElementException {
                if (a == null) {
                    // --- construct the first (trivial) permutation of indices
                    a = new int[n];
                    for (int i = 0; i < n; ++i) {
                        a[i] = i;
                    }
                } else {
                    // --- find the next permutation of indices
                    int i, j, t;
                    for (i = n-2; i >= 0 && a[i] >= a[i+1]; --i) {}
                    if (i < 0) {
                        throw new NoSuchElementException("at end");
                    }
                    for (j = n-1; a[j] <= a[i]; --j) {}
                    t = a[i]; a[i] = a[j]; a[j] = t;
                    for (++i, j = n-1; i < j; ++i, --j) {
                        t = a[i]; a[i] = a[j]; a[j] = t;
                    }
                }
                
                // --- construct the result
                final List<E> result = new ArrayList<E>(n);
                for (int i = 0; i < n; ++i) {
                    result.add(things[a[i]]);
                }
                return result;
            }
        };
    }
    
    /**
     * Constructs an iterator over all ordered m-tuples of elements picked from
     * an array without repetitions.
     * 
     * @param things the set of objects to pick from.
     * @return the newly constructed iterator.
     */
    public static <E> IteratorAdapter<List<E>> combinations(final E things[],
                                                            final int m)
    {
        final int n = things.length;
        if (n < m) {
            throw new IllegalArgumentException("not enough objects to pick from");
        }

        return new IteratorAdapter<List<E>>() {
            private Iterator<List<Object>> perms = Iterators.empty();
            private final int a[] = new int[m];

            protected List<E> findNext() throws NoSuchElementException {
                if (!perms.hasNext()) {
                    if (a[1] == 0) {
                        for (int i = 0; i < m; ++i) {
                            a[i] = i;
                        }
                    } else {
                        int k = m - 1;
                        while (k >= 0 && n - a[k] <= m - k) {
                            --k;
                        }
                        if (k < 0) {
                            throw new NoSuchElementException("at end");
                        } else {
                            ++a[k];
                            for (int i = k + 1; i < m; ++i) {
                                a[i] = a[k] + i - k;
                            }
                        }
                    }
                    final Object picks[] = new Object[m];
                    for (int i = 0; i < m; ++i) {
                        picks[i] = things[a[i]];
                    }
                    perms = permutations(picks);
                }
                @SuppressWarnings("unchecked")
                List<E> result = (List<E>) perms.next();
                return result;
            }
        };
    }
    
    /**
     * Constructs an iterator over selections of k elements from an n-element
     * array, where the same element may appear more than once in a selection.
     * The algorithm assumes that the elements of the input array are unique.
     * 
     * @param things the input list.
     * @param k the size of selections to produce.
     * @return the newly constructed iterator.
     */
    public static <E> IteratorAdapter<List<E>> selections(final E things[],
                                                          final int k)
    {
        final int n = things.length;
        
        return new IteratorAdapter<List<E>>() {
            int a[] = null;
            
            protected List<E> findNext() throws NoSuchElementException {
                if (a == null) {
                    // --- the first selection uses the first available object k times
                    a = new int[k];
                    for (int i = 0; i < k; ++i) {
                        a[i] = 0;
                    }
                } else {
                    // --- find the next selection
                    int i = k-1;
                    while (i >= 0 && a[i] >= n-1) {
                        --i;
                    }
                    if (i < 0) {
                        throw new NoSuchElementException("at end");
                    } else {
                        ++a[i];
                        while (i < k-1) {
                            a[i+1] = a[i];
                            ++i;
                        }
                    }
                }
                
                // --- construct the result
                final List<E> result = new ArrayList<E>();
                
                for (int i = 0; i < k; ++i) {
                    result.add(things[a[i]]);
                }
                return result;
            }
        };
    }
    
    /**
     * Checks if two iterators deliver equal sequences of objects. This may be
     * useful for such things as unit testing. As there is no way to reset an
     * iterator, the arguments will not be useable after the test.
     * 
     * @param a the first iterator.
     * @param b the second iterator.
     * @return true if the iterators produce equal sequences.
     */
    public static <E> boolean equal(final Iterator<E> a, final Iterator<?> b) {
		while (true) {
			if (a.hasNext() != b.hasNext()) {
			    return false;
			}
			if (a.hasNext()) {
			    if (!(a.next().equals(b.next()))) {
			        return false;
			    }
			} else {
				return true;
			}
		}
    }
    
    /**
     * Returns the number of objects an iterator iterates over. Notice that the
     * iterator will be exhausted after applying this method.
     * 
     * @param iter the iterator.
     * @return the number of objects.
     */
    public static <E> int size(final Iterator<E> iter) {
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            ++count;
        }
        return count;
    }
    
    /**
     * Checks if an object equal (but not necessarily identical) to a given one
     * is produced by a given iterator.
     * 
     * @param iter the iterator.
     * @param x the object to look for.
     * @return true if the given object is generated by the iterator.
     */
    public static <E> boolean contains(final Iterator<E> iter, final E x) {
    	if (x == null) {
    		while (iter.hasNext()) {
    			if (iter.next() == null) {
    				return true;
    			}
    		}
    	} else {
    		while (iter.hasNext()) {
    			if (x.equals(iter.next())) {
    				return true;
    			}
    		}
    	}
		return false;
    }
    
    /**
     * Returns a list containing the set of elements covered by an iterator.
     * Notice that the iterator will be exhausted after applying this method.
     * 
     * @param iter the iterator.
     * @return the list of objects covered.
     */
    public static <E> List<E> asList(final Iterator<E> iter) {
        final List<E> result = new LinkedList<E>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }
    
    /**
     * Adds the elements covered by an iterator to a list in order.
     * Notice that the iterator will be exhausted after applying this method.
     * 
     * @param list the list to add to.
     * @param iter the iterator.
     */
    public static <E> void addAll(final List<E> list,
            final Iterator<? extends E> iter) {
        while (iter.hasNext()) {
            list.add(iter.next());
        }
    }
    
    /**
     * Writes the elements covered by an iterator to an output stream in order.
     * 
     * @param out represents the output stream.
     * @param iter the iterator to write.
     * @param separator written in between entries.
     * @return the number of item written.
     * @throws IOException if writing to the output stream does not work.
     */
    public static <E> int write(final BufferedWriter out,
            final Iterator<E> iter, final String separator)
                    throws IOException {
        int count = 0;
        if (iter.hasNext()) {
            out.write(String.valueOf(iter.next()));
            out.write(separator);
            out.flush();
            ++count;
        }
        while (iter.hasNext()) {
            out.write(String.valueOf(iter.next()));
            out.write(separator);
            out.flush();
            ++count;
        }
        return count;
    }
    
    /**
     * Writes the elements covered by an iterator to a print stream in order.
     * 
     * @param out represents the output stream.
     * @param iter the iterator to write.
     * @param separator written after each entry.
     * @return the number of items written.
     */
    public static <E> int print(final PrintStream out, final Iterator<E> iter,
            final String separator) {
        int count = 0;
        while (iter.hasNext()) {
            out.print(String.valueOf(iter.next()));
            out.print(separator);
            out.flush();
            ++count;
        }
        return count;
    }
}
