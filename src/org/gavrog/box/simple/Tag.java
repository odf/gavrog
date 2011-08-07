/*
   Copyright 2007 Olaf Delgado-Friedrichs

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


package org.gavrog.box.simple;


/**
 * Each instance of this class is a unique object with no special properties.
 * These can essentially be used as symbolic constants.
 * 
 * @author Olaf Delgado
 * @version $Id: Tag.java,v 1.1 2007/04/25 22:51:33 odf Exp $
 */
public class Tag {
    // --- the unique id assigned to the next instance
    private static int nextId = 1;
    
    // --- the id for this instance
    private int id;
    
    /**
     * Constructs an instance.
     */
    public Tag() {
        this.id = nextId++;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(T)
     */
    public int compareTo(final Object other) {
        if (other instanceof Tag) {
            return this.id - ((Tag) other).id;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
