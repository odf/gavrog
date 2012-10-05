/*
   Copyright 2009 Olaf Delgado-Friedrichs

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

package org.gavrog.joss.dsyms.derived;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.jane.fpgroups.ChoiceLimitExceededException;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.GroupAction;
import org.gavrog.jane.fpgroups.SmallActionsIterator;
import org.gavrog.jane.fpgroups.Stabilizer;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;


/**
 * Tests if a 3-dimensional Delaney symbol encodes a tiling of ordinary space.
 * 
 * @author Olaf Delgado
 * @version $Id: EuclidicityTester.java,v 1.10 2007/04/26 20:21:58 odf Exp $
 */
public class EuclidicityTester {
    final private static boolean LOGGING = false;
    
    final private static DelaneySymbol goodlist[] = new DelaneySymbol[] {
            new DSymbol("48 3:"
                    + "2 4 6 8 10 12 14 16 18 20 22 24 "
                    + "26 28 30 32 34 36 38 40 42 44 46 48,"
                    + "8 3 5 7 16 11 13 15 24 19 21 23 "
                    + "32 27 29 31 40 35 37 39 48 43 45 47,"
                    + "9 10 17 18 25 26 33 34 24 23 41 "
                    + "42 36 35 32 31 47 48 40 39 45 46 43 44,"
                    + "42 41 48 47 46 45 44 43 26 25 32 "
                    + "31 30 29 28 27 34 33 40 39 38 37 36 35:"
                    + "4 4 4 4 4 4,3 3 3 3 3 3 3 3,4 4 4 4 4 4"),
            new DSymbol("64 3:"
                    + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 "
                    + "34 36 38 40 42 44 46 48 50 52 54 56 58 60 62 64,"
                    + "6 3 5 12 9 11 18 15 17 26 21 23 25 32 29 31 "
                    + "38 35 37 46 41 43 45 52 49 51 58 55 57 64 61 63,"
                    + "7 8 13 14 19 20 27 28 22 21 33 34 39 40 47 48 "
                    + "53 54 59 60 42 41 46 45 56 55 61 62 58 57 63 64,"
                    + "59 60 61 62 63 64 33 34 35 36 37 38 47 48 49 "
                    + "50 51 52 39 40 41 42 43 44 45 46 54 53 58 57 56 55:"
                    + "3 3 3 4 3 3 4 3 3 3,3 5 5 5 3 3 3 5,4 4 3 3 3 3 3 3 3 3")
    };
    final private static List Z3 = Arrays.asList(new Whole[] {
            Whole.ZERO, Whole.ZERO, Whole.ZERO });
    final private static List empty = new LinkedList();

    final private static Set<String> goodInvariants = new HashSet<String>();
    static {
        final Package pkg = EuclidicityTester.class.getPackage();
        final String packagePath = pkg.getName().replaceAll("\\.", "/");
        final String filePath = packagePath + "/euclideanInvariants.data";
        final InputStream inStream = ClassLoader
                .getSystemResourceAsStream(filePath);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
                inStream));
        while (true) {
            final String line;
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (line == null) {
                break;
            }
            if (line.length() == 0 || line.trim().charAt(0) == '#') {
                continue;
            }
            goodInvariants.add(line.trim());
        }
    }
    
    final private DelaneySymbol ds;
    final private boolean useInvariant;
    final private int choicesFactor;
    
    private boolean done = false;
    private boolean good = false;
    private boolean bad = false;
    
    private DelaneySymbol outcome = null;
    private String cause = null;
    
    /**
     * Constructs an instance.
     * 
     * @param ds the Delaney symbol to test.
     * @param useInvars if true, orbifold invariants are compared first
     */
    public EuclidicityTester(final DelaneySymbol ds, final boolean useInvars,
            final int factor) {
        if (ds.dim() != 3) {
            final String s = "symbol must be 3-dimensional";
            throw new UnsupportedOperationException(s);
        }
        this.ds = ds;
        this.useInvariant = useInvars;
        this.choicesFactor = factor;
    }
    
    /**
     * Constructs an instance that does not use orbifold invariants.
     * 
     * @param ds the Delaney symbol to test.
     */
    public EuclidicityTester(final DelaneySymbol ds) {
    	this(ds, true, 10000);
    }
    
    /**
     * @return true if symbol is found to be Euclidean.
     */
    public boolean isGood() {
        compute();
        return this.good;
    }

    /**
     * @return true if symbol is found to be non-Euclidean.
     */
    public boolean isBad() {
        compute();
        return this.bad;
    }

    /**
     * @return true if symbol could not be determined as either Euclidean or
     * non-Euclidean.
     */
    public boolean isAmbiguous() {
        compute();
        return !(this.good || this.bad);
    }

    /**
     * Returns the cause for the decision made.
     * @return Returns the cause.
     */
    public String getCause() {
        return cause;
    }
    
    /**
     * If no decision was made, returns the last result of manipulating the
     * input symbol in order to find one on which euclidicity can be determined
     * easier. In the current implementation, this is derived from a
     * pseudo-toroidal cover of the input symbol by applying a
     * {@link Simplifier}.
     * 
     * @return Returns the outcome.
     */
    public DelaneySymbol getOutcome() {
        return outcome;
    }
    
    public static boolean invariantOkay(final DelaneySymbol ds) {
    	return goodInvariants.contains(new OrbifoldInvariant(ds).toString());
    }
    
    /**
     * Do the actual computation here.
     */
    private void compute() {
        if (this.done) {
            return;
        }

        if (LOGGING) {
            System.err.println("\nStarting tests for symbol " + ds);
        }
        if (this.useInvariant) {
			if (LOGGING) {
				System.err.print("Computing orbifold invariant ...");
				System.err.flush();
			}
			final String invar = new OrbifoldInvariant(ds).toString();
			if (LOGGING) {
				System.err.println(" done.");
				System.err.flush();
			}
			if (!goodInvariants.contains(invar)) {
				decide(false, "orbifold invariants do not match");
				return;
			}
		}
        if (LOGGING) {
            System.err.print("Computing pseudo-toroidal cover ...");
            System.err.flush();
        }
        final DelaneySymbol cover = Covers.pseudoToroidalCover3D(ds);
        if (LOGGING) {
            System.err.println(" done.");
            System.err.flush();
        }
        if (cover == null) {
            decide(false, "no pseudo-toroidal cover");
            return;
        }
        
        if (LOGGING) {
            System.err.print("Simplifying cover ...");
            System.err.flush();
        }
        final DelaneySymbol simpl = new Simplifier(cover).getSimplifiedSymbol();
        if (LOGGING) {
            System.err.println(" done.");
            System.err.flush();
        }
        if (simpl.size() == 0) {
            decide(false, "cover is a lens space");
            return;
        }
        
        if (!simpl.isConnected()) {
            // --- cover is a connected sum, but some parts may be trivial (spheres)
            if (LOGGING) {
                System.err.print("Cover is a sum - checking components ...");
                System.err.flush();
            }
            final boolean bad = badComponents(simpl);
            if (LOGGING) {
                System.err.println(" done.");
                System.err.flush();
            }
            if (bad) {
                decide(false, "cover is a non-trivial connected sum");
                return;
            } else {
                giveUp("cover is a (potentially trivial) connected sum", simpl);
                return;
            }
        }
        
        if (LOGGING) {
            System.err.print("Looking up the simplified cover ...");
            System.err.flush();
        }
        for (int i = 0; i < goodlist.length; ++i) {
            if (simpl.equals(goodlist[i])) {
                if (LOGGING) {
                    System.err.println(" found.");
                    System.err.flush();
                }
                decide(true, "simplified cover recognized");
                return;
            }
        }
        if (LOGGING) {
            System.err.println(" nothing found.");
            System.err.flush();
        }
        
        if (LOGGING) {
            System.err.print("Computing fundamental group of cover...");
            System.err.flush();
        }
        final FpGroup G = new FundamentalGroup(simpl).getPresentation();
        if (LOGGING) {
            System.err.println(" done.");
            System.err.flush();
        }
        
        if (LOGGING) {
            System.err.print("Checking for handle reductions ...");
            System.err.flush();
        }
        final List invariants = G.abelianInvariants();
        if (LOGGING) {
            System.err.println(" done.");
            System.err.flush();
        }
        if (!invariants.equals(Z3)) {
            // --- a handle reduction in the simplification process changes the homology
            decide(false, "cover has at least one handle");
            return;
        }
        
        if (G.getRelators().size() == 0) {
            if (LOGGING) {
                System.err.println("Pseudotoroidal cover has free fundamental group:");
                System.err.println("    " + cover);
                System.err.flush();
            }
            decide(false, "cover has free fundamental group");
            return;
        }
        
        if (LOGGING) {
            System.err.print("Checking abelianized index 2 subgroups ...");
            System.err.flush();
        }
        final boolean good = checkAbelianInvariantsSubgroups(G, 2, Z3);
        if (LOGGING) {
            System.err.println(" done.");
            System.err.flush();
        }
        if (!good) {
            decide(false, "bad subgroups for cover");
            return;
        }
        
        try {
            if (LOGGING) {
                System.err
                        .print("Counting subgroups of fundamental group up to index 3 ...");
                System.err.flush();
            }
            final boolean index3Ok = checkSubgroupCount(G, 3, 21);
            if (!index3Ok) {
                decide(false, "bad subgroup count for cover");
                return;
            }

            if (LOGGING) {
                System.err
                        .print("Counting subgroups of fundamental group up to index 4 ...");
                System.err.flush();
            }
            final boolean index4Ok = checkSubgroupCount(G, 4, 56);
            if (!index4Ok) {
                decide(false, "bad subgroup count for cover");
                return;
            }
        } catch (ChoiceLimitExceededException ex) {
            giveUp("runtime limits reached", simpl);
            return;
        }
        
        giveUp("no decision found", simpl);
    }

    /**
     * Tries to show that the connected components of the symbol are not exactly one
     * 3-torus and all other 3-spheres.
     * 
     * @return true if the above was successfully shown.
     */
    private static boolean badComponents(final DelaneySymbol ds) {
        final List idcs = new IndexList(ds);
        int countZ3 = 0;
        
        for (final Iterator reps = ds.orbitReps(idcs); reps.hasNext();) {
            final DSymbol component = new DSymbol(new Subsymbol(ds, idcs, reps.next()));
            final FpGroup G = new FundamentalGroup(component).getPresentation();
            final List invars = G.abelianInvariants();
            if (invars.equals(Z3)) {
                ++countZ3;
                if (countZ3 > 1 || !checkAbelianInvariantsSubgroups(G, 2, Z3)) {
                    return true;
                }
            } else if (invars.size() == 0) {
                if (!checkAbelianInvariantsSubgroups(G, 5, empty)) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies that all subgroups of the given group up to a given index have
     * the specified abelian invariants.
     * 
     * @param G the input group.
     * @param index the maximum index
     * @param expected the abelian invariants the subgroups should have.
     * 
     * @return true if all abelian invariants are okay.
     */
    private static boolean checkAbelianInvariantsSubgroups(final FpGroup G,
            final int index, final List expected) {
        final SmallActionsIterator actions = new SmallActionsIterator(G, index, false);
        while (actions.hasNext()) {
            final GroupAction action = (GroupAction) actions.next();
            final Stabilizer stab = new Stabilizer(action, action.domain().next());
            final List invar = stab.getPresentation().abelianInvariants();
            if (!invar.equals(expected)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifies a count on the number of conjugacy classes of subgroups.
     * 
     * @param G the original group.
     * @param index the maximal index of the subgroups.
     * @param expected the expected number of conjugacy classes.
     * @return true if the actual number matches the expected.
     */
    private boolean checkSubgroupCount(FpGroup G, int index, int expected) {
        int count = 0;
        final SmallActionsIterator actions = new SmallActionsIterator(G, index, false);

        // --- restrict the number of average choice moves per expected result
        actions.setMaximalNumberOfChoices(expected * this.choicesFactor);
        
        while (true) {
            try {
                actions.next();
            } catch (NoSuchElementException ex) {
                if (LOGGING) {
                    System.err.println("\n    ... done with " + stats(actions));
                    System.err.flush();
                }
                break;
            } catch (ChoiceLimitExceededException ex) {
                if (LOGGING) {
                    System.err.println("\n    ... choice limit exceeded after "
                            + stats(actions));
                    System.err.flush();
                }
                throw ex;
            }
            ++count;
            if (LOGGING) {
                System.err.print(" " + count);
                System.err.flush();
            }
            if (count > expected) {
                if (LOGGING) {
                    System.err.println("\n    ... found more than expected after "
                            + stats(actions));
                    System.err.flush();
                }
                return false;
            }
        }
        return (count == expected);
    }

    private String stats(final SmallActionsIterator actions) {
        return actions.getChoicesSoFar() + " choices performed in "
                + (actions.getTimeElapsed() / 1000.0) + " seconds.";
    }
    
    /**
     * States a decision as to wether the input symbol is Euclidean or not.
     * 
     * @param good true if Euclidean, false if non-Euclidean.
     * @param cause the cause for the decision.
     */
    private void decide(final boolean good, final String cause) {
        if (good) {
            this.good = true;
        } else {
            this.bad = true;
        }
        this.cause = cause;
        this.done = true;
    }
    
    /**
     * States that euclidicity could not be decided for the input symbol.
     * 
     * @param outcome the last derived symbol considered in the test.
     * @param cause the cause for the decision.
     */
    private void giveUp(final String cause, final DelaneySymbol outcome) {
        this.outcome = outcome;
        this.cause = cause;
        this.done = true;
    }
}
