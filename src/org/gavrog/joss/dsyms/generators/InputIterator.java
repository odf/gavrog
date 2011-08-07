/*
   Copyright 2008 Olaf Delgado-Friedrichs

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

package org.gavrog.joss.dsyms.generators;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.joss.dsyms.basic.DSymbol;


/**
 * An iterator that reads Delaney symbols from an input stream.
 * 
 * @author Olaf Delgado
 * @version $Id: InputIterator.java,v 1.5 2008/01/16 00:50:44 odf Exp $
 */
public class InputIterator extends IteratorAdapter {
    final private BufferedReader reader;
    final StringBuffer buffer = new StringBuffer(200);

    /**
     * Constructs a new instance.
     * @param reader represents the input stream.
     */
    public InputIterator(final Reader reader) {
        this.reader = new BufferedReader(reader);
    }
    
    /**
     * Constructs a new instance.
     * @param filename name of an input file.
     */
    public InputIterator(final String filename) {
        try {
            this.reader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /* (non-Javadoc)
     * @see javaDSym.util.IteratorAdapter#findNext()
     */
    protected Object findNext() throws NoSuchElementException {
    	String name = null;
        while (true) {
			String line;
			try {
				line = reader.readLine();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			if (line == null) {
              throw new NoSuchElementException("at end");
			}
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			if (line.charAt(0) == '#') {
				if (line.charAt(1) == '@') {
					line = line.substring(2).trim();
					if (line.startsWith("name ")) {
						name = line.substring(5);
					}
				}
			} else {
				int i = line.indexOf('#');
				if (i >= 0) {
					line = line.substring(0, i);
				}
				buffer.append(' ');
				buffer.append(line);
				if (buffer.toString().trim().endsWith(">")) {
					final DSymbol ds = new DSymbol(buffer.toString().trim());
					buffer.delete(0, buffer.length());
					if (name != null && !name.equals("")) {
						ds.setName(name);
					}
					name = null;
					return ds;
				}
			}
        }
    }
}
