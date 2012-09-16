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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class Cache<K, V> {
    final private Map<K, SoftReference<V>> content;

    /**
     * Constructs an instance.
     */
    public Cache() {
        this.content = new HashMap<K, SoftReference<V>>();
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
    public V get(final K key) {
        final SoftReference<V> entry = content.get(key);
        if (entry != null) {
            final V result = entry.get();
            if (result != null) {
                return result;
            }
        }
        throw new CacheMissException();
    }

    /**
     * @param key
     * @param value
     */
    public V put(final K key, final V value) {
        this.content.put(key, new SoftReference<V>(value));
        return value;
    }

    /**
     * @param key
     */
    public V remove(final K key) {
        V value = null;
        try {
            value = this.get(key);
        } catch (CacheMissException ex) {
        }
        this.content.remove(key);
        return value;
    }
}
