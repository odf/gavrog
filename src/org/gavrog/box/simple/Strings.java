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

/**
 * Contains some simple utilities for strings.
 * 
 * @author Olaf Delgado
 * @version $Id: Strings.java,v 1.2 2006/05/22 22:27:47 odf Exp $
 */

public class Strings {
    /**
     * Turns a string's first letter to upper case.
     * 
     * @param s the source string.
     * @return the capitalized version.
     */
    public static String capitalized(final String s) {
        if (s.length() > 1) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        } else {
            return s.toUpperCase();
        }
    }
    
    /**
	 * Converts the given string to a format that can be safely parsed. If
	 * double quotes or white space is present or the parameter
	 * <code>forceQuotes</code> is true, the string is enclosed in double
	 * quotes and problematic characters (currently double quotes and
	 * backslashes) are preceded by backslashes. Any stretch of white space is
	 * converted to a single blank and leading or trailing white space is
	 * removed. Also, any control characters are removes.
	 * 
	 * @param s
	 *            the input string.
	 * @param forceQuotes
	 *            if true, enclose output in double quotes.
	 * @return the modified string to print out.
	 */
	public static String parsable(final String s, final boolean forceQuotes) {
		boolean needsQuoting = forceQuotes;
		if (!needsQuoting) {
			for (int i = 0; i < s.length(); ++i) {
				final char c = s.charAt(i);
				if (c == '"' || Character.isWhitespace(c)) {
					needsQuoting = true;
					break;
				}
			}
		}
		final StringBuffer buf = new StringBuffer(2 * s.length());
		if (needsQuoting) {
			buf.append('"');
		}
		boolean inWhite = false;
		for (int i = 0; i < s.length(); ++i) {
			final char c = s.charAt(i);
			if (Character.isWhitespace(c)) {
				if (!inWhite) {
					buf.append(' ');
					inWhite = true;
				}
				continue;
			} else if (!Character.isIdentifierIgnorable(c)) {
				inWhite = false;
				if (c == '"' || (needsQuoting && c == '\\')) {
					buf.append('\\');
				}
				buf.append(c);
			}
		}
		if (needsQuoting) {
			buf.append('"');
		}
		if (buf.length() > 1 && buf.charAt(1) == ' ') {
			buf.deleteCharAt(1);
		}
		if (buf.length() > 1 && buf.charAt(buf.length() - 2) == ' ') {
			buf.deleteCharAt(buf.length() - 2);
		}
		if (buf.length() == 0) {
			buf.append("\"\"");
		}
		
		return buf.toString();
	}
}
