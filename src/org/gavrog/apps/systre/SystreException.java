/*
Copyright 2006 Olaf Delgado-Friedrichs

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

package org.gavrog.apps.systre;

/**
 * Exceptions thrown by Systre's methods.
 * 
 * @author Olaf Delgado
 * @version $Id: SystreException.java,v 1.1 2007/05/12 01:32:24 odf Exp $
 */
public class SystreException extends RuntimeException {
    public static class Type {
        final private String text;
        
        /**
         * Constructor is private, so only the instances below will exist.
         * @param text
         */
        private Type(final String text) {
            this.text = text;
        }
        public String toString() {
            return text;
        }
    }
    
    final public static Type FATAL = new Type("FATAL");
    final public static Type INTERNAL = new Type("INTERNAL");
    final public static Type MISC = new Type("MISC");
    final public static Type FILE = new Type("FILE");
    final public static Type INPUT = new Type("INPUT");
    final public static Type STRUCTURE = new Type("STRUCTURE");
    final public static Type CANCELLED = new Type("CANCELLED");
    
    final private Type type;
    
    public SystreException() {
        super();
        type = MISC;
    }

    public SystreException(final String message) {
        super(message);
        type = MISC;
    }

    public SystreException(final Type type, final String message) {
        super(message);
        this.type = type;
    }

    public SystreException(final Throwable cause) {
        super(cause);
        type = MISC;
    }

    public SystreException(final String message, final Throwable cause) {
        super(message, cause);
        type = MISC;
    }

    public SystreException(final Type type, final String message, final Throwable cause) {
        super(message, cause);
        this.type = type;
    }
    
    public Type getType() {
        return this.type;
    }
}
