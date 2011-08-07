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


package org.gavrog.box.collections;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Delgado
 * @version $Id: Cache.java,v 1.3 2008/02/27 08:14:27 odf Exp $
 */
public class Cache {
    final private Map content;

    public class NotFoundException extends RuntimeException {
    }
    
    /**
     * Constructs an instance.
     */
    public Cache() {
        this.content = new HashMap();
    }

    /**
     * 
     */
    public void clear() {
        this.content.clear();
    }

    /**
     * @param key
     */
    public Object get(final Object key) {
        final Object entry = this.content.get(key);
        if (entry != null) {
            final Object result = ((SoftReference) entry).get();
            if (result != null) {
                return result;
            }
        }
        throw new NotFoundException();
    }

    /**
     * @param key
     */
    public boolean getBoolean(final Object key) {
        return ((Boolean) this.get(key)).booleanValue();
    }

    /**
     * @param key
     * @param value
     */
    public Object put(final Object key, final Object value) {
        this.content.put(key, new SoftReference(value));
        return value;
    }

    /**
     * @param key
     * @param value
     */
    public boolean put(final Object key, final boolean value) {
        return ((Boolean) this.put(key, new Boolean(value))).booleanValue();
    }

    /**
     * @param key
     */
    public Object remove(final Object key) {
        Object value = null;
        try {
            value = this.get(key);
        } catch (NotFoundException ex) {
        }
        this.content.remove(key);
        return value;
    }
}
