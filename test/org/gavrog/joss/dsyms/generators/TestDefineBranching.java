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

package org.gavrog.joss.dsyms.generators;

import java.util.Iterator;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.generators.DefineBranching3d;

import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestDefineBranching.java,v 1.2 2006/09/04 00:09:27 odf Exp $
 */
public class TestDefineBranching extends TestCase {
    public void test1() {
        doTest(new DSymbol("1 3:1,1,1,1:0,0,0"), 14);
    }
    
    public void test2() {
        doTest(new DSymbol("3 3:1 3,2 3,1 3,1 2 3:3,3,0 0"), 6);
    }
    
    public void test3() {
        doTest(new DSymbol("1 3:1,1,1,1:4,3,4"), 1);
    }
    
    public void test4() {
        doTest(new DSymbol("1 3:1,1,1,1:6,0,3"), 1);
    }
    
    public void test5() {
        doTest(new DSymbol("15 3:1 3 4 6 8 10 12 14 15,2 3 5 6 12 9 11 13 15,"
                           + "1 3 15 9 10 13 14 11 12,4 5 6 12 11 10 13 14 15:"
                           + "3 3 3 3,3 3 3 3,0 0 0 0"), 10);
    }
    
    public void doTest(final DSymbol ds, final int xCount) {
        final Iterator iter = new DefineBranching3d(ds);
        int count = 0;
        while (iter.hasNext()) {
            final DSymbol out = (DSymbol) iter.next();
            if (xCount < 0) { 
                System.out.println(out);
            }
            ++count;
        }
        if (xCount < 0) {
            System.err.println("Found " + count + " symbols.");
        } else {
            assertEquals(xCount, count);
        }
    }
}
