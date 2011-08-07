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

package org.gavrog.box.simple;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Olaf Delgado
 * @version $Id: Misc.java,v 1.2 2006/05/24 22:44:30 odf Exp $
 */
public class Misc {
    /**
     * Returns the stack trace of a throwable as a string.
     * 
     * @param throwable the throwable.
     * @return the string representation.
     */
    public static String stackTrace(final Throwable throwable, final String prefix) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.close();
        String s = sw.toString();
        if (prefix != null) {
        	s = s.replaceAll("(?m)^", prefix);
        }
        return s;
    }

    /**
     * Returns the stack trace of a throwable as a string.
     * 
     * @param throwable the throwable.
     * @return the string representation.
     */
    public static String stackTrace(final Throwable throwable) {
    	return stackTrace(throwable, null);
    }
}
