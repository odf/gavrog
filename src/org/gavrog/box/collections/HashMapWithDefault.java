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

import java.util.HashMap;

/**
 * Realizes a hash map in which the {@link #get(Object)}method installs a default
 * value whenever it is passed a key with no associated value yet. The default value
 * is specified by overriding the method {@link #makeDefault()}. This approach is
 * used rather than copying a fixed object so that for example an empty container can
 * be used as the default value without the risk of aliasing.
 *
 * @author Olaf Delgado
 * @version $Id: HashMapWithDefault.java,v 1.1 2005/09/22 05:34:36 odf Exp $
 */
public abstract class HashMapWithDefault extends HashMap {
    /**
     * This method must be overriden to produce a default value.
     * 
     * @return the default value.
     */
    public abstract Object makeDefault();
    
    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(final Object key) {
        if (!containsKey(key)) {
            try {
                put(key, makeDefault());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return super.get(key);
    }
}