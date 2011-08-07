/*
   Copyright 2005 Olaf Delgado-Friedrichs

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
 * @author Olaf Delgado
 * @version $Id: Partition.java,v 1.2 2006/09/20 22:36:12 odf Exp $
 */
public class Partition {
	private class IntList extends ArrayList implements Cloneable {
		public int getInt(int i) {
			return ((Integer) get(i)).intValue();
		}
		public void addInt(int x) {
			add(new Integer(x));
		}
		public void setInt(int i, int x) {
			set(i, new Integer(x));
		}
	}
	
	private HashMap index = new HashMap();
	private ArrayList value = new ArrayList();
	private IntList dad = new IntList();
	private IntList rnk = new IntList();
	
	public Object clone() {
		Partition result = new Partition();
		result.index = (HashMap) this.index.clone();
		result.value = (ArrayList) this.value.clone();
		result.dad = (IntList) this.dad.clone();
		result.rnk = (IntList) this.rnk.clone();
		return result;
	}
	
	private int repIndex(Object a) {
		int i, x;
		if (index.containsKey(a)) {
			i = x = ((Integer) index.get(a)).intValue();
			while (dad.get(i) != null) {
				i = dad.getInt(i);
			}
			while (dad.get(x) != null) {
				int t = dad.getInt(x);
				dad.setInt(x, i);
				x = t;
			}
		} else {
			x = value.size();
			index.put(a, new Integer(x));
			value.add(a);
			dad.add(null);
			rnk.addInt(0);
		}
		return x;
	}
	
	public Object find(Object a) {
		if (index.containsKey(a)) {
			return value.get(repIndex(a));
		} else {
			return a;
		}
	}
	
	public boolean areEquivalent(Object a, Object b) {
		return find(a) == find(b);
	}
	
	public void unite(Object a, Object b) {
		int i = repIndex(a);
		int j = repIndex(b);
		
		if (i != j) {
			if (rnk.getInt(j) > rnk.getInt(i)) {
				int t = i; i = j; j = t;
			}
			dad.setInt(j, i);
			rnk.setInt(i, rnk.getInt(i) + rnk.getInt(j) + 1);
			rnk.setInt(j, 0);
		}
	}

	public Map representativeMap() {
		HashMap result = new HashMap();
		Iterator iter = value.iterator();
		while (iter.hasNext()) {
			Object a = iter.next();
			result.put(a, find(a));
		}
		return result;
	}
    
    public Iterator classes() {
        final Map rep2class = new HashMap();
        for (final Iterator iter = value.iterator(); iter.hasNext();) {
            final Object a = iter.next();
            final Object rep = find(a);
            if (!rep2class.containsKey(rep)) {
                rep2class.put(rep, new HashSet());
            }
            ((Set) rep2class.get(rep)).add(a);
        }
        return rep2class.values().iterator();
    }
    
    public String toString() {
    	final StringBuffer tmp = new StringBuffer(100);
    	tmp.append("{\n");
    	for (final Iterator sets = this.classes(); sets.hasNext();) {
    		final Set cl = (Set) sets.next();
    		tmp.append("  {");
    		boolean first = true;
    		for (final Iterator elms = cl.iterator(); elms.hasNext();) {
    			final Object x = elms.next();
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
