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

package org.gavrog.joss.dsyms.basic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;


/**
 * This class implements a morphism from a connected, finite Delaney symbol to
 * another symbol. A Delaney symbol morphism is a map which respects the
 * neighbor relations and m-values.
 * 
 * @author Olaf Delgado
 * @version $Id: DSMorphism.java,v 1.1 2007/04/23 20:57:06 odf Exp $
 */

public class DSMorphism implements Map {
    final private Map src2img;
    final Map img2src;
    final private boolean bijective;

    /**
     * Creates an instance with a specified first point mapping.
     * 
     * @param src the source symbol.
     * @param img the image symbol.
     * @param srcBase a base element in the source symbol.
     * @param imgBase what to map the source base element to.
     */
    public DSMorphism(final DelaneySymbol src, final DelaneySymbol img,
            final Object srcBase, final Object imgBase) {
        try {
            src.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("source must be finite");
        }
        if (!src.isConnected()) {
            throw new UnsupportedOperationException("source must be connected");
        }
        if (!Iterators.equal(src.indices(), img.indices())) {
            throw new UnsupportedOperationException("index lists must be equal");
        }
        if (srcBase == null || imgBase == null) {
        	throw new IllegalArgumentException("elements must not be null");
        }
        
        final List indices = new IndexList(src);
        boolean bijective = img.isConnected();
        src2img = new HashMap();
        img2src = new HashMap();
        src2img.put(srcBase, imgBase);
        img2src.put(imgBase, srcBase);
        final LinkedList queue = new LinkedList();
        queue.addLast(new Pair(srcBase, imgBase));
        
        while (queue.size() > 0) {
            final Pair entry = (Pair) queue.removeFirst();
            final Object D = entry.getFirst();
            final Object E = entry.getSecond();
            
            for (final Iterator idcs = src.indices(); idcs.hasNext();) {
                final int i = ((Integer) idcs.next()).intValue();
                final Object Di = src.op(i, D);
                final Object Ei = img.op(i, E);
                
                if (correspond(Di, Ei)) {
                    continue;
                }
                if (src2img.containsKey(Di)) {
                    throw new IllegalArgumentException("no such morphism");
                }
                if (img2src.containsKey(Ei)) {
                    bijective = false;
                }
                for (int k = 0; k < src.dim(); ++k) {
    				int r = ((Integer) indices.get(k)).intValue();
    				int s = ((Integer) indices.get(k + 1)).intValue();
    				if (src.m(r, s, Di) != img.m(r, s, Ei)) {
                        throw new IllegalArgumentException("no such morphism");
    				}
                }
                src2img.put(Di, Ei);
                img2src.put(Ei, Di);
                queue.addLast(new Pair(Di, Ei));
            }
        }
        
        this.bijective = bijective;
    }
    
    /**
     * Creates an instance with a default initial point mapping.
     * 
     * @param src the source symbol.
     * @param img the image symbol.
     */
    public DSMorphism(final DelaneySymbol src, final DelaneySymbol img) {
        this(src, img, src.elements().next(), img.elements().next());
    }
    
    /**
     * Creates an instance modelled after a given one or its inverse.
     * 
     * @param morphism the model morphism.
     * @param inverse if true, try to construct the inverse.
     */
    private DSMorphism(final DSMorphism morphism, final boolean inverse) {
        if (inverse) {
            if (!morphism.isIsomorphism()) {
                throw new IllegalArgumentException("not invertible");
            } else {
                this.src2img = morphism.img2src;
                this.img2src = morphism.src2img;
                this.bijective = true;
            }
        } else {
            this.src2img = morphism.src2img;
            this.img2src = morphism.img2src;
            this.bijective = morphism.bijective;
        }
    }
    
    /**
     * Creates an instance after a given one.
     * @param model the model morphism.
     */
    public DSMorphism(final DSMorphism model) {
        this(model, false);
    }
    
    /**
     * Returns the inverse of a given morphism.
     * @return the morphism to invert.
     */
    public DSMorphism inverse() {
        return new DSMorphism(this, true);
    }
    
    /**
     * Checks if the given elements are mapped to each other.
     * 
     * @param src the source element.
     * @param img the possible image element.
     * @return true if img is the image of src or both are null.
     */
    private boolean correspond(Object src, Object img) {
        if (img == null) {
            return src == null;
        } else {
            return img.equals(src2img.get(src));
        }
    }

    /**
     * Checks if this morphism is bijective.
     * @return true if this morphism is bijective.
     */
    public boolean isIsomorphism() {
        return this.bijective;
    }
    
    /**
     * Returns an object that is mapped onto the given one.
     * 
     * @param x the image.
     * @return a source for that image.
     */
    public Object getASource(final Object x) {
        return img2src.get(x);
    }
    
    // --- Implementation of map interface starts here.
    
    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        return src2img.size();
    }

    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException("morphisms are immutable");
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return src2img.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(final Object arg0) {
        return src2img.containsKey(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(final Object arg0) {
        return src2img.containsValue(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection values() {
        return src2img.values();
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(final Map arg0) {
        throw new UnsupportedOperationException("morphisms are immutable");
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        return src2img.entrySet();
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        return src2img.keySet();
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(final Object arg0) {
        return src2img.get(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(final Object arg0) {
        throw new UnsupportedOperationException("morphisms are immutable");
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(final Object arg0, final Object arg1) {
        throw new UnsupportedOperationException("morphisms are immutable");
    }
    
    /**
     * Computes all the automorphisms of a given Delaney Symbol.
     * 
     * @param ds the input symbol.
     * @return the list of all automorphisms of ds.
     */
    public static List automorphisms(final DelaneySymbol ds) {
        final List result = new LinkedList();
        final Iterator elms = ds.elements();
        
        if (elms.hasNext()) {
            final Object first = elms.next();
            result.add(new DSMorphism(ds, ds, first, first));
            
            while (elms.hasNext()) {
                final Object D = elms.next();
                try {
                    result.add(new DSMorphism(ds, ds, first, D));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
            }
        }
        
        return result;
    }
}
