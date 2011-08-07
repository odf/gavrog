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

package org.gavrog.box.simple;


/**
 * Simple base class for object-oriented constants.
 * 
 * @author Olaf Delgado
 * @version $Id: NamedConstant.java,v 1.1 2006/07/05 22:01:51 odf Exp $
 */
public class NamedConstant {
    private final String name;

    /**
     * Making the constructor private makes sure that no other instances than the above
     * are created.
     * 
     * @param name the name associated to the constant.
     */
    protected NamedConstant(final String name) {
        this.name = name;
    }
    
    public String toString() {
        return this.name;
    }
}