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

/**
 * Defines a translation between abstract letters and their names.
 */
public interface Alphabet<E> {
    /**
     * Retrieves a specific letter name.
     * @param i the numeric letter.
     * @return the name of the ith letter or null if no name was defined.
     */
    public abstract E letterToName(int i);

    /**
     * Retrieves the index of a letter name.
     * @param name the name.
     * @return the index or abstract letter for this name.
     */
    public abstract int nameToLetter(E name);
}
