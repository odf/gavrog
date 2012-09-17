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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents a partition of some finite set into equivalence classes.
 */
public class Partition<E> {
	private HashMap<E, Integer> index = new HashMap<E, Integer>();
	private ArrayList<E> value = new ArrayList<E>();
	private ArrayList<Integer> dad = new ArrayList<Integer>();
	private ArrayList<Integer> rnk = new ArrayList<Integer>();
	
    public Object clone() {
		Partition<E> result = new Partition<E>();
		result.index = new HashMap<E, Integer>(this.index);
		result.value = new ArrayList<E>(this.value);
		result.dad = new ArrayList<Integer>(this.dad);
		result.rnk = new ArrayList<Integer>(this.rnk);
		return result;
	}
	
	private int repIndex(E a) {
		int i, x;
		if (index.containsKey(a)) {
			i = x = index.get(a);
			while (dad.get(i) != null) {
				i = dad.get(i);
			}
			while (dad.get(x) != null) {
				int t = dad.get(x);
				dad.set(x, i);
				x = t;
			}
		} else {
			x = value.size();
			index.put(a, x);
			value.add(a);
			dad.add(null);
			rnk.add(0);
		}
		return x;
	}
	
	public E find(E a) {
		if (index.containsKey(a)) {
			return value.get(repIndex(a));
		} else {
			return a;
		}
	}
	
	public boolean areEquivalent(E a, E b) {
		return find(a) == find(b);
	}
	
	public void unite(E a, E b) {
		int i = repIndex(a);
		int j = repIndex(b);
		
		if (i != j) {
			if (rnk.get(j) > rnk.get(i)) {
				int t = i; i = j; j = t;
			}
			dad.set(j, i);
			rnk.set(i, rnk.get(i) + rnk.get(j) + 1);
			rnk.set(j, 0);
		}
	}

	public Map<E, E> representativeMap() {
		Map<E, E> result = new HashMap<E, E>();
		for (final E a: value) {
		    result.put(a, find(a));
		}
		return result;
	}
    
    public Iterator<Set<E>> classes() {
        final Map<E, Set<E>> rep2class = new HashMap<E, Set<E>>();
        for (final E a: value) {
            final E rep = find(a);
            if (!rep2class.containsKey(rep)) {
                rep2class.put(rep, new HashSet<E>());
            }
            rep2class.get(rep).add(a);
        }
        return rep2class.values().iterator();
    }
    
    public String toString() {
    	final StringBuffer tmp = new StringBuffer(100);
    	tmp.append("{\n");
    	for (final Iterator<Set<E>> sets = this.classes(); sets.hasNext();) {
    		final Set<E> cl = sets.next();
    		tmp.append("  {");
    		boolean first = true;
    		for (final E x: cl) {
    			if (first) {
    				first = false;
    			} else {
    				tmp.append(", ");
    			}
    			tmp.append(x);
    		}
    		tmp.append("},\n");
    	}
    	tmp.append("}\n");
    	return tmp.toString();
    }
}
