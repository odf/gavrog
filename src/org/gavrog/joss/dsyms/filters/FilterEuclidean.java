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

package org.gavrog.joss.dsyms.filters;

import java.util.LinkedList;
import java.util.List;

import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;
import org.gavrog.joss.dsyms.generators.InputIterator;


/**
 */
public class FilterEuclidean {

    public static void main(String[] args) {
        final String filename = args[0];
        final int f = (args.length > 1 ? Integer.parseInt(args[1]) : 10000);

        final List<Integer> good = new LinkedList<Integer>();
        final List<Pair<Integer, DelaneySymbol<Integer>>> ambiguous =
                new LinkedList<Pair<Integer, DelaneySymbol<Integer>>>();
        int count = 0;
        final Stopwatch timer = new Stopwatch();
        timer.start();

        for (final DSymbol ds: new InputIterator(filename)) {
            ++count;

            final EuclidicityTester tester = new EuclidicityTester(ds, true, f);
            if (tester.isGood()) {
                System.out.println("#Symbol " + count + " is good: "
                        + tester.getCause());
                good.add(count);
                System.out.println(ds);
            } else if (tester.isBad()) {
                System.out.println("#Symbol " + count + " is bad: "
                        + tester.getCause());
            } else {
                System.out.println("#Symbol " + count + " is ambiguous: "
                                   + tester.getCause());
                ambiguous.add(new Pair<Integer, DelaneySymbol<Integer>>(
                        count, tester.getOutcome()));
                System.out.println("#??? " + ds);
            }
            System.out.flush();
        }
        timer.stop();

        System.out.print("### " + good.size() + " good symbols:");
        for (final Integer i: good) {
            System.out.print(" " + i);
        }
        System.out.println();

        System.out.println("### " + ambiguous.size() + " ambiguous symbols:");
        for (final Pair<Integer, DelaneySymbol<Integer>> pair: ambiguous) {
            final int n = pair.getFirst();
            final DelaneySymbol<Integer> ds = pair.getSecond();
            final FpGroup<String> G =
                    new FundamentalGroup<Integer>(ds).getPresentation();
            System.out.println("#   " + n + ":  " + ds.canonical());
            System.out.println("#       " + G);
            System.out.println("#       (abelian invariants: "
                + G.abelianInvariants() + ")" );
        }
        System.out.println("### Running time was " + timer.format() + ".");
    }
}
