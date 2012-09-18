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

package org.gavrog.jane.fpgroups;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.gavrog.jane.fpgroups.CosetAction;
import org.gavrog.jane.fpgroups.FiniteAlphabet;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.jane.fpgroups.Stabilizer;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Simple unit test for Stabilizer.
 * @author Olaf Delgado
 * @version $Id: TestStabilizer.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestStabilizer extends TestCase {

	private FiniteAlphabet A;
	private FpGroup G;
	private CosetAction TG;
	private Stabilizer S;

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
        A = new FiniteAlphabet(new String[] { "a", "b" });
        G = new FpGroup(A, new String[] { "a*b*a^-1*b^-1" });
        final List sgens = new LinkedList();
        sgens.add(FreeWord.parsedWord(A, "a^2"));
        sgens.add(FreeWord.parsedWord(A, "b^3"));
        TG = new CosetAction(G, sgens);
        S = new Stabilizer(TG, TG.getTrivialCoset());
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		A = null;
		G = null;
		TG = null;
		S = null;
	}

	public void testGetAction() {
		Assert.assertEquals(TG, S.getAction());
	}

	public void testGetBasepoint() {
		Assert.assertEquals(TG.getTrivialCoset(), S.getBasepoint());
	}
	
	public void testGetEdgeLabelling() {
		Assert.assertNotNull(S.getEdgeLabelling());
	}

	public void testGetGenerators() {
		Assert.assertNotNull(S.getGenerators());
		Object x = S.getBasepoint();
		for (Iterator gens = S.getGenerators().iterator(); gens.hasNext();) {
			final FreeWord w = (FreeWord) gens.next();
			Assert.assertEquals(x, TG.apply(x, w));
		}
	}

	public void testGetPresentation() {
		final FpGroup pres = S.getPresentation();
		final FpGroup test = new FpGroup(pres.getAlphabet(),
				new String[] {"s_1*s_2*s_1^-1*s_2^-1"});
		Assert.assertEquals(test.toString(), pres.toString());
	}
}
