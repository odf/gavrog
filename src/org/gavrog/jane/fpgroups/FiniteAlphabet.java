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

package org.gavrog.jane.fpgroups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A finite alphabet to be used in abstract words and groups.
 * @author Olaf Delgado
 * @version $Id: FiniteAlphabet.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */
public class FiniteAlphabet implements Alphabet {
	private final List nameList;
	private final Map nameSet;
	
	/**
	 * Constructs an Alphabet instance from an list of letter names, which may
	 * be arbitrary objects.
	 * @param names all the letter names for this alphabet.
	 */
	public FiniteAlphabet(final List names) {
	    final int n = names.size();
	    final Map tmp = new HashMap();
	    for (int i = 0; i < n; ++i) {
	        tmp.put(names.get(i), new Integer(i+1));
	    }
		this.nameList = Collections.unmodifiableList(names);
		this.nameSet = Collections.unmodifiableMap(tmp);
	}
	
	/**
	 * Constructs an Alphabet instance. Letter names start with a common prefix,
	 * followed by a running number. Example: "s1", "s2", "s3", etc.
	 * @param prefix the prefix for all letter names.
	 * @param n the number of letters.
	 */
	public FiniteAlphabet(final String prefix, final int n) {
	    this(lettersFromPrefix(prefix, n));
	}
	
	/**
	 * Constructs an Alphabet instance from an array of letter names, which may
	 * be arbitrary objects.
	 * @param names all the letter names for this alphabet.
	 */
	public FiniteAlphabet(final Object[] names) {
	    this(Arrays.asList(names));
	}

	/**
	 * Constructs letter names with a common prefix and indexed from 1.
	 * @param prefix the prefix for all letter names.
	 * @param n the number of letters.
	 * @return the list of letter names.
	 */
	protected static List lettersFromPrefix(final String prefix, final int n) {
	    final List tmp = new ArrayList();
		for (int i = 1; i <= n; ++i) {
			tmp.add(prefix + i);
		}
		return tmp;
	}

    /**
     * Returns the list of all letter-names in order.
     * @return the name list..
     */
    public List getNameList() {
        return this.nameList;
    }
    
	/**
	 * @return the size of this Alphabet.
	 */
	public int size() {
		return nameList.size();
	}

	/**
	 * Retrieves a specific letter name.
	 * @param i a number between 1 and the number of letters.
	 * @return the name of the ith letter.
	 */
	public Object letterToName(final int i) {
		return nameList.get(i-1);
	}
	
	/**
	 * Retrieves the index of a letter name.
	 * @param name the name.
	 * @return the index of this name.
	 */
	public int nameToLetter(final Object name) {
		return ((Integer) nameSet.get(name)).intValue();
	}
	
	/*
	 * Returns a string representation.
	 */
	public String toString() {
		final StringBuffer buf = new StringBuffer(100);
		buf.append("Alphabet({");
		for (int i = 0; i < size(); ++i) {
			if (i > 0) {
				buf.append(", ");
			}
			buf.append("\"");
			buf.append(nameList.get(i));
			buf.append("\"");
		}
		buf.append("})");
		return buf.toString();
	}
	
	public boolean equals(Object other) {
	    if (other instanceof FiniteAlphabet) {
	        final List ourNames = this.getNameList();
	        final List otherNames = ((FiniteAlphabet) other).getNameList();
	        return ourNames.equals(otherNames);
	    } else {
	        return false;
	    }
	}
	
	public int hashCode() {
	    return getNameList().hashCode();
	}
}
