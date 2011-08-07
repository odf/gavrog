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

package org.gavrog.jane.numbers;


public abstract class ArithmeticBase implements IArithmetic {

    /* A minimal set of methods that a dervied concrete class must implement. */

    public abstract boolean isExact();

    public abstract IArithmetic zero();
    public abstract IArithmetic one();

    public abstract IArithmetic negative();
    public abstract IArithmetic inverse();

    public abstract IArithmetic plus(Object other);
    public abstract IArithmetic times(Object other);

    public abstract int compareTo(Object other);
    public abstract IArithmetic floor();

    public abstract String toString();
    public abstract int hashCode();

    /* Comparison with constants. */
    
    public boolean isZero() {
        return this.equals(this.zero());
    }

    public boolean isOne() {
        return this.equals(this.one());
    }

    /* Nonstatic arithmetic methods with default implementations. */

    public IArithmetic plus(long n) {
        return this.plus(new Whole(n));
    }

    public IArithmetic plus(double n) {
        return this.plus(new FloatingPoint(n));
    }

    public IArithmetic rplus(IArithmetic other) {
        return this.plus(other);
    }

    public IArithmetic minus(Object other) {
        return this.plus(((IArithmetic) other).negative());
    }

    public IArithmetic minus(long n) {
        return this.minus(new Whole(n));
    }

    public IArithmetic minus(double n) {
        return this.minus(new FloatingPoint(n));
    }

    public IArithmetic rminus(IArithmetic other) {
        return this.negative().plus(other);
    }

    public IArithmetic times(long n) {
        return this.times(new Whole(n));
    }

    public IArithmetic times(double n) {
        return this.times(new FloatingPoint(n));
    }

    public IArithmetic rtimes(IArithmetic other) {
        return this.times(other);
    }

    public IArithmetic dividedBy(long n) {
        return this.dividedBy(new Whole(n));
    }

    public IArithmetic dividedBy(double n) {
        return this.dividedBy(new FloatingPoint(n));
    }

    public IArithmetic dividedBy(Object other) {
        return this.times(((IArithmetic) other).inverse());
    }

    public IArithmetic rdividedBy(IArithmetic other) {
        return this.inverse().times(other);
    }

    public IArithmetic div(long n) {
        return this.div(new Whole(n));
    }

    public IArithmetic div(double n) {
        return this.div(new FloatingPoint(n));
    }

    public IArithmetic div(Object other) {
        return this.dividedBy(other).floor();
    }
    
    public IArithmetic mod(long n) {
        return this.mod(new Whole(n));
    }

    public IArithmetic mod(double n) {
        return this.mod(new FloatingPoint(n));
    }

    public IArithmetic mod(Object other) {
        return this.minus(this.div(other).times(other));
    }
    
    public IArithmetic raisedTo(Object other) {
        return power(this, (Real) other);
    }

    public IArithmetic raisedTo(long n) {
        return power(this, new Whole(n));
    }

    public IArithmetic round() {
        final IArithmetic n = floor();
        final IArithmetic m = n.plus(Whole.ONE);
        if (this.minus(n).isLessThan(m.minus(this))) {
            return n;
        } else {
            return m;
        }
    }
    
    /* Static arithmetic methods. */

    public static IArithmetic power(IArithmetic arg, Real exp) {
        IArithmetic zero = exp.zero();
        Real one = (Real) exp.one();
        IArithmetic two = one.plus(one);

        if (exp.isNegative()) {
            arg = arg.inverse();
            exp = (Real) exp.negative();
        }

        Real mask = one;
        while (mask.isLessOrEqual(exp)) {
            mask = (Real) mask.plus(mask);
        }

        IArithmetic a = arg.one();

        while (mask.isGreaterThan(one)) {
            mask = (Real) mask.dividedBy(two);
            a = a.times(a);
            if (exp.isGreaterOrEqual(mask)) {
                a = a.times(arg);
                exp = (Real) exp.minus(mask);
            }
        }

        if (! exp.equals(zero)) {
            throw new IllegalArgumentException("exponent must be integral");
        }

        return a;
    }

    /* Sign and absolute value. */
    
    public int sign() {
        int tmp = this.compareTo(this.zero());
        if (tmp < 0) {
            return -1;
        } else if (tmp == 0) {
            return 0;
        } else {
            return 1;
        }
    }
    
    public boolean isPositive() {
        return this.sign() > 0;
    }
    
    public boolean isNonNegative() {
        return this.sign() >= 0;
    }
    
    public boolean isNegative() {
        return this.sign() < 0;
    }
    
    public IArithmetic abs() {
        if (this.sign() < 0) {
            return this.negative();
        } else {
            return this;
        }
    }
    
    public IArithmetic norm() {
        return abs();
    }
    
    /* Comparison methods. */

    public boolean isLessThan(Object other) {
        return this.compareTo(other) < 0;
    }

    public boolean isLessOrEqual(Object other) {
        return this.compareTo(other) <= 0;
    }

    public boolean isGreaterThan(Object other) {
        return this.compareTo(other) > 0;
    }

    public boolean isGreaterOrEqual(Object other) {
        return this.compareTo(other) >= 0;
    }

    public boolean equals(Object other) {
        return this.compareTo(other) == 0;
    }


    /* Convenience methods for use with Jython. */

    public IArithmetic __neg__() {
        return this.negative();
    }
    
    public IArithmetic __add__(Object other) {
        return this.plus(other);
    }

    public IArithmetic __add__(long other) {
        return this.plus(new Whole(other));
    }

    public IArithmetic __add__(double other) {
        return this.plus(new FloatingPoint(other));
    }

    public IArithmetic __radd__(long other) {
        return new Whole(other).plus(this);
    }

    public IArithmetic __radd__(double other) {
        return (new FloatingPoint(other)).plus(this);
    }

    public IArithmetic __sub__(Object other) {
        return this.minus(other);
    }

    public IArithmetic __sub__(long other) {
        return this.minus(new Whole(other));
    }

    public IArithmetic __sub__(double other) {
        return this.minus(new FloatingPoint(other));
    }

    public IArithmetic __rsub__(long other) {
        return new Whole(other).minus(this);
    }

    public IArithmetic __rsub__(double other) {
        return (new FloatingPoint(other)).minus(this);
    }

    public IArithmetic __mul__(Object other) {
        return this.times(other);
    }

    public IArithmetic __mul__(long other) {
        return this.times(new Whole(other));
    }

    public IArithmetic __mul__(double other) {
        return this.times(new FloatingPoint(other));
    }

    public IArithmetic __rmul__(long other) {
        return new Whole(other).times(this);
    }

    public IArithmetic __rmul__(double other) {
        return (new FloatingPoint(other)).times(this);
    }

    public IArithmetic __div__(Object other) {
        return this.dividedBy(other);
    }

    public IArithmetic __div__(long other) {
        return this.dividedBy(new Whole(other));
    }

    public IArithmetic __div__(double other) {
        return this.dividedBy(new FloatingPoint(other));
    }

    public IArithmetic __rdiv__(long other) {
        return new Whole(other).dividedBy(this);
    }

    public IArithmetic __rdiv__(double other) {
        return (new FloatingPoint(other)).dividedBy(this);
    }

    public IArithmetic __pow__(Object other) {
        return this.raisedTo(other);
    }

    public IArithmetic __pow__(long other) {
        return this.raisedTo(new Whole(other));
    }

    public IArithmetic __rpow__(long other) {
        return (new Whole(other)).raisedTo((Whole) this);
    }

    public IArithmetic __rpow__(double other) {
        return (new FloatingPoint(other)).raisedTo(this);
    }

    public boolean __eq__(Object other) {
        return this.equals(other);
    }

    public boolean __eq__(long other) {
        return this.equals(new Whole(other));
    }

    public boolean __eq__(double other) {
        return this.equals(new FloatingPoint(other));
    }

    public boolean __ne__(Object other) {
        return !this.equals(other);
    }

    public boolean __ne__(long other) {
        return !this.equals(new Whole(other));
    }

    public boolean __ne__(double other) {
        return !this.equals(new FloatingPoint(other));
    }

    public int __hash__() {
        return this.hashCode();
    }

    public boolean __lt__(Object other) {
        return this.compareTo(other) < 0;
    }

    public boolean __lt__(long other) {
        return this.compareTo(new Whole(other)) < 0;
    }

    public boolean __lt__(double other) {
        return this.compareTo(new FloatingPoint(other)) < 0;
    }

    public boolean __le__(Object other) {
        return this.compareTo(other) <= 0;
    }

    public boolean __le__(long other) {
        return this.compareTo(new Whole(other)) <= 0;
    }

    public boolean __le__(double other) {
        return this.compareTo(new FloatingPoint(other)) <= 0;
    }

    public boolean __gt__(Object other) {
        return this.compareTo(other) > 0;
    }

    public boolean __gt__(long other) {
        return this.compareTo(new Whole(other)) > 0;
    }

    public boolean __gt__(double other) {
        return this.compareTo(new FloatingPoint(other)) > 0;
    }

    public boolean __ge__(Object other) {
        return this.compareTo(other) >= 0;
    }

    public boolean __ge__(long other) {
        return this.compareTo(new Whole(other)) >= 0;
    }

    public boolean __ge__(double other) {
        return this.compareTo(new FloatingPoint(other)) >= 0;
    }
}
