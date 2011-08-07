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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Olaf Delgado
 * @version $Id: PrintSystemProperties.java,v 1.1 2007/05/19 04:45:35 odf Exp $
 */
public class PrintSystemProperties {
	public static void main(String[] args) {
    	final Properties props = System.getProperties();
    	final List<String> pkeys = new ArrayList<String>();
    	for (final Object key: props.keySet()) {
    		pkeys.add(String.valueOf(key));
    	}
    	Collections.sort(pkeys);
    	for (final String key: pkeys) {
    		System.out.format("%-32s    %s\n", key, props.getProperty(key));
    	}
	}
}
