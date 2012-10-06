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

package org.gavrog.joss.dsyms.basic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;


/**
 * This class implements a morphism from a connected, finite Delaney symbol to
 * another symbol. A Delaney symbol morphism is a map which respects the
 * neighbor relations and m-values.
 */

public class DSMorphism<S, T> {
    final private Map<S, T> src2img;
    final Map<T, S> img2src;
    final private boolean bijective;

    /**
     * Creates an instance with a specified first point mapping.
     * 
     * @param src the source symbol.
     * @param img the image symbol.
     * @param srcBase a base element in the source symbol.
     * @param imgBase what to map the source base element to.
     */
    public DSMorphism(final DelaneySymbol<S> src, final DelaneySymbol<T> img,
            final S srcBase, final T imgBase) {
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
        
        final List<Integer> indices = new IndexList(src);
        boolean bijective = img.isConnected();
        src2img = new HashMap<S, T>();
        img2src = new HashMap<T, S>();
        src2img.put(srcBase, imgBase);
        img2src.put(imgBase, srcBase);
        final LinkedList<Pair<S, T>> queue = new LinkedList<Pair<S, T>>();
        queue.addLast(new Pair<S, T>(srcBase, imgBase));
        
        while (queue.size() > 0) {
            final Pair<S, T> entry = queue.removeFirst();
            final S D = entry.getFirst();
            final T E = entry.getSecond();
            
            for (final int i: src.indices()) {
                final S Di = src.op(i, D);
                final T Ei = img.op(i, E);
                
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
    				int r = indices.get(k);
    				int s = indices.get(k + 1);
    				if (src.m(r, s, Di) != img.m(r, s, Ei)) {
                        throw new IllegalArgumentException("no such morphism");
    				}
                }
                src2img.put(Di, Ei);
                img2src.put(Ei, Di);
                queue.addLast(new Pair<S, T>(Di, Ei));
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
    public DSMorphism(final DelaneySymbol<S> src, final DelaneySymbol<T> img) {
        this(src, img, src.elements().next(), img.elements().next());
    }
    
    /**
     * Creates an instance after a given one.
     * @param model the model morphism.
     */
    public DSMorphism(final DSMorphism<S, T> model) {
        this.src2img = model.src2img;
        this.img2src = model.img2src;
        this.bijective = model.bijective;
    }
    
    /**
     * Creates an instance as the inverse of a given one.
     * 
     * @param morphism the model morphism.
     * @param dummy value does not matter.
     */
    private DSMorphism(final DSMorphism<T, S> model, final boolean dummy) {
    	if (!model.isIsomorphism()) {
    		throw new IllegalArgumentException("not invertible");
    	} else {
    		this.src2img = model.img2src;
    		this.img2src = model.src2img;
    		this.bijective = true;
    	}
    }
    
    /**
     * Returns the inverse of a given morphism.
     * @return the morphism to invert.
     */
    public DSMorphism<T, S> inverse() {
        return new DSMorphism<T, S>(this, true);
    }
    
    /**
     * Checks if the given elements are mapped to each other.
     * 
     * @param src the source element.
     * @param img the possible image element.
     * @return true if img is the image of src or both are null.
     */
    private boolean correspond(final S src, final T img) {
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
    public S getASource(final T x) {
        return img2src.get(x);
    }
    
    public int size() {
        return src2img.size();
    }

    public T get(final S arg0) {
        return src2img.get(arg0);
    }

    /**
     * Computes all the automorphisms of a given Delaney Symbol.
     * 
     * @param ds the input symbol.
     * @return the list of all automorphisms of ds.
     */
    public static <T> List<DSMorphism<T, T>> automorphisms(
    		final DelaneySymbol<T> ds)
    {
        final List<DSMorphism<T, T>> result =
        		new LinkedList<DSMorphism<T, T>>();

        final Iterator<T> elms = ds.elements();
        if (elms.hasNext()) {
            final T first = elms.next();
            result.add(new DSMorphism<T, T>(ds, ds, first, first));
            
            while (elms.hasNext()) {
                final T D = elms.next();
                try {
                    result.add(new DSMorphism<T, T>(ds, ds, first, D));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
            }
        }
        
        return result;
    }
}
