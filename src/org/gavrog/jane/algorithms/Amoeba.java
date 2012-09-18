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

package org.gavrog.jane.algorithms;

import java.util.Arrays;

/**
 * Implements the downhill simplex (a.k.a. amoeba) method for multidimensional
 * function minimization as described in Numerical Recipes.
 */
public class Amoeba {
    final static double TINY = 1e-10;
    final static boolean DEBUG = false;
    
    public static interface Function {
        public double evaluate(final double p[]);
        public int dim();
    }
    
    private class Point implements Comparable<Point> {
        public final double arg[];
        public final double value;
        
        public Point(final double arg[]) {
            this.arg = (double[]) arg.clone();
            double val = Amoeba.this.f.evaluate(arg);
            if (Double.isNaN(val)) {
                this.value = Double.MAX_VALUE;
            } else {
                this.value = val;
            }
            ++Amoeba.this.steps;
        }

        public int compareTo(final Point other) {
            return Double.compare(this.value, other.value);
        }
    }
    
    final private Function f;
    private double tolerance;
    private int maxSteps;
    private int maxRestarts;
    private double scale;
    
    final private int dim;
    final private Point simplex[];
    int steps;
    
    /**
     * Constructs an instance, setting all the options.
     * 
     * @param f the function to minimize.
     * @param tolerance when to consider function values too close to continue.
     * @param maxSteps maximal number of function evaluations.
     * @param maxRestarts maximal number of restarts.
     * @param scale the scale for the initial trial simplex.
     */
    public Amoeba(final Function f, final double tolerance, final int maxSteps,
            final int maxRestarts, final double scale) {
        this.f = f;
        this.tolerance = tolerance;
        this.maxSteps = maxSteps;
        this.maxRestarts = maxRestarts;
        this.scale = scale;
        
        this.dim = f.dim();
        this.simplex = new Point[this.dim + 1];
    }
    
    /**
     * Constructs an instance, using defaults for most options.
     * 
     * @param f the function to minimize.
     */
    public Amoeba(final Function f) {
        this(f, 1e-6,  1000, 10, 1.0);
    }
    
    /**
     * Performs the minimization.
     * 
     * @param p the starting point to evaluate function at.
     * @return the point at which a minimum was found.
     */
    public double[] go(final double p[]) {
        Point bestSoFar = new Point(p);
        Point bestOfLastPass = bestSoFar;
        this.steps = 0;
        
        for (int pass = 0; pass < maxRestarts; ++pass) {
            // --- initialize the simplex
            final double s = 1 - 2 * this.scale * (pass % 2);
            for (int k = 0; k <= this.dim; ++k) {
                final double q[] = (double[]) p.clone();
                if (k > 0) {
                    q[k-1] += s;
                }
                this.simplex[k] = new Point(q);
            }
            
            // --- sort it by function values
            Arrays.sort(this.simplex);
            
            // --- perform the amoeba algorithm
            Point bestInPass = null;
            
            while (this.steps < this.maxSteps) {
                // --- record the best point so far
                if (bestInPass == null || this.simplex[0].value < bestInPass.value) {
                    bestInPass = this.simplex[0];
                }
                
                // --- check the current value range
                final double vlo = this.simplex[0].value;
                final double vhi = this.simplex[this.dim].value;
                final double relativeRange = 2 * Math.abs(vhi - vlo)
                                     / (Math.abs(vlo) + Math.abs(vhi) + TINY);
                if (relativeRange < this.tolerance) {
                    break;
                }
                
                // --- modify the simplex
                Point q = amotry(-1.0);
                if (q.value <= this.simplex[0].value) {
                    q = amotry(2.0);
                } else if (q.value >= this.simplex[this.dim-1].value) {
                    q = amotry(0.5);
                    if (q.value >= this.simplex[dim].value) {
                        final double s0[] = this.simplex[0].arg;
                        for (int k = 1; k <= dim; ++k) {
                            final double sk[] = this.simplex[k].arg;
                            final double a[] = new double[this.dim];
                            for (int i = 0; i < dim; ++i) {
                                a[i] = (s0[i] + sk[i]) / 2.0;
                            }
                            this.simplex[k] = new Point(a);
                        }
                        Arrays.sort(this.simplex);
                        if (DEBUG) {
                            System.out.println("After scaling: E = "
                                    + this.simplex[0].value);
                        }
                    }
                }
            }
            
            // --- check the results
            final Point q = this.simplex[0];
            if (q.value < bestSoFar.value) {
                bestSoFar = q;
            }
            double maxD = 0.0;
            for (int i = 0; i < dim; ++i) {
                maxD = Math.max(maxD, Math.abs(bestOfLastPass.arg[i] - q.arg[i]));
            }
            if (maxD < this.tolerance) {
                break;
            } else {
                bestOfLastPass = q;
            }
        }
        
        return bestSoFar.arg;
    }
    
    /**
     * Scales or reflects the simplex w.r.t. its facet of lowest values.
     * 
     * @param factor the scaling factor (negative for reflection).
     * @return the new point generated.
     */
    private Point amotry(final double factor) {
        if (DEBUG) {
            System.out.println("Calling amotry(" + factor);
            System.out.println("    before: E = " + this.simplex[0].value + " .. "
                    + this.simplex[this.dim].value);
        }
        
        // --- compute the simplex centroid
        final double c[] = new double[this.dim];
        for (int k = 0; k <= dim; ++k) {
            final double s[] = this.simplex[k].arg;
            for (int i = 0; i < dim; ++i) {
                c[i] += s[i];
            }
        }
        
        // --- compute the new point
        final double f1 = (1.0 - factor) / this.dim;
        final double f2 = factor - f1;
        final double hi[] = this.simplex[this.dim].arg;
        final double a[] = new double[this.dim];
        for (int i = 0; i < dim; ++i) {
            a[i] = f1 * c[i] + f2 * hi[i];
        }
        final Point p = new Point(a);
        if (DEBUG) {
            System.out.println("    new point with E = " + p.value);
        }
        
        // --- keep the simplex sorted
        final double val = p.value;
        if (val < this.simplex[this.dim].value) {
            if (DEBUG) {
                System.out.println("    inserting new point");
            }
            for (int k = this.dim-1; k >= -1; --k) {
                if (k < 0 || val >= this.simplex[k].value) {
                    this.simplex[k+1] = p;
                    if (DEBUG) {
                        System.out.println("    inserted at position " + k+1);
                    }
                    break;
                } else {
                    this.simplex[k+1] = this.simplex[k];
                }
            }
        }
        
        if (DEBUG) {
            System.out.println("    after: E = " + this.simplex[0].value + " .. "
                    + this.simplex[this.dim].value);
            System.out.println("Leaving amotry(" + factor);
        }
        return p;
    }

    /**
     * @return the current value of maxRestarts.
     */
    public int getMaxRestarts() {
        return maxRestarts;
    }
    
    /**
     * Sets the value of maxRestarts.
     *
     * @param maxRestarts the new value of maxRestarts.
     */
    public void setMaxRestarts(int maxRestarts) {
        this.maxRestarts = maxRestarts;
    }
    
    /**
     * @return the current value of maxSteps.
     */
    public int getMaxSteps() {
        return maxSteps;
    }
    
    /**
     * Sets the value of maxSteps.
     *
     * @param maxSteps the new value of maxSteps.
     */
    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }
    
    /**
     * @return the current value of scale.
     */
    public double getScale() {
        return scale;
    }
    
    /**
     * Sets the value of scale.
     *
     * @param scale the new value of scale.
     */
    public void setScale(double scale) {
        this.scale = scale;
    }
    
    /**
     * @return the current value of tolerance.
     */
    public double getTolerance() {
        return tolerance;
    }
    
    /**
     * Sets the value of tolerance.
     *
     * @param tolerance the new value of tolerance.
     */
    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }
}
