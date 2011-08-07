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

/**
 * An infinite alphabet in which letters names consist of a common prefix and a
 * number.
 * @author Olaf Delgado
 * @version $Id: PrefixAlphabet.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */
public class PrefixAlphabet implements Alphabet {
    
    final String prefix;
    
    /**
     * Constructs a PrefixAlphabet instance.
     * @param prefix the prefix for all names.
     */
    public PrefixAlphabet(final String prefix) {
        this.prefix = prefix;
    }

    public Object letterToName(final int i) {
        if (i > 0) {
            return prefix + i;
        } else {
            return null;
        }
    }

    public int nameToLetter(final Object name) {
        if (name instanceof String) {
            final String s = (String) name;
            if (s.startsWith(this.prefix)) {
                final int i = Integer.parseInt(s.substring(this.prefix.length()));
                if (i > 0) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException(name + " is not a letter name");
    }
}
