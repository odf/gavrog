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



public abstract class Rational extends Real {

    public abstract Whole numerator();
    public abstract Whole denominator();

    private static Rational makeFast(Whole num, Whole den) {
        if (den.isZero()) {
            throw new ArithmeticException("zero denominator");
        } else if (den.isOne() || num.isZero()) {
            return num;
        } else {
            return new Fraction(num, den, false);
        }
    }

    public static Rational make(Whole num, Whole den) {
        Whole m = num.gcd(den);
        if (den.isNegative()) {
            m = (Whole) m.negative();
        }
        if (! m.isOne()) {
            num = num.div(m);
            den = den.div(m);
        }

        return makeFast(num, den);
    }

    public static Rational make(int num, int den) {
    	return make(new Whole(num), new Whole(den));
    }

    public boolean isExact() {
        return true;
    }

    public boolean isInteger() {
    	return this.denominator().equals(Whole.ONE);
    }
    
    public IArithmetic floor() {
        return numerator().div(denominator());
    }
    
    public int sign() {
        return numerator().sign();
    }

    public IArithmetic inverse() {
        Whole num = numerator();
        Whole den = denominator();

        if (den.isNegative()) {
            den = (Whole) den.negative();
            num = (Whole) num.negative();
        }

        return makeFast(den, num);
    }

    public boolean isZero() {
        return numerator().isZero();
    }

    public boolean isOne() {
        return numerator().isOne() && denominator().isOne();
    }

    public double doubleValue() {
        return numerator().doubleValue() / denominator().doubleValue();
    }

    public long longValue() {
    	return numerator().div(denominator()).longValue();
    }

    public String toString() {
        return numerator().toString() + "/" + denominator().toString();
    }

    public IArithmetic negative() {
        return new Fraction((Whole) numerator().negative(),
                           denominator(), false);
    }

    public IArithmetic abs() {
        return new Fraction((Whole) numerator().abs(),
                           denominator(), false);
    }

    public int hashCode() {
        return 31 * numerator().hashCode() + denominator().hashCode();
    }
    
    /* --- Addition. */
    
    public IArithmetic plus(Object other) {
    	if (other instanceof Rational) {
            Whole x_num = this.numerator();
            Whole x_den = this.denominator();
            Whole y_num = ((Rational) other).numerator();
            Whole y_den = ((Rational) other).denominator();

            Whole m = x_den.gcd(y_den);
            Whole n1 = (Whole) y_den.div(m).times(x_num);
            Whole n2 = (Whole) x_den.div(m).times(y_num);
            Whole num = (Whole) n1.plus(n2);
            Whole den = (Whole) x_den.div(m).times(y_den);

            return make(num, den);
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rplus(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }

    /* --- Subtraction */
    
    public IArithmetic minus(Object other) {
    	if (other instanceof Rational) {
            Whole x_num = this.numerator();
            Whole x_den = this.denominator();
            Whole y_num = ((Rational) other).numerator();
            Whole y_den = ((Rational) other).denominator();

            Whole m = x_den.gcd(y_den);
            Whole n1 = (Whole) y_den.div(m).times(x_num);
            Whole n2 = (Whole) x_den.div(m).times(y_num);
            Whole num = (Whole) n1.minus(n2);
            Whole den = (Whole) x_den.div(m).times(y_den);

            return make(num, den);
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rminus(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }
    
    /* --- Multiplication */
    
    public IArithmetic times(Object other) {
    	if (other instanceof Rational) {
            Whole x_num = this.numerator();
            Whole x_den = this.denominator();
            Whole y_num = ((Rational) other).numerator();
            Whole y_den = ((Rational) other).denominator();

            Whole a = x_num.gcd(y_den);
            if (! a.isOne()) {
                x_num = x_num.div(a);
                y_den = y_den.div(a);
            }
            Whole b = y_num.gcd(x_den);
            if (! b.isOne()) {
                y_num = y_num.div(b);
                x_den = x_den.div(b);
            }

            Whole num = (Whole) x_num.times(y_num);
            Whole den = (Whole) x_den.times(y_den);
        	if (den.isNegative()) {
        		den = (Whole) den.negative();
        		num = (Whole) num.negative();
        	}
            return makeFast(num, den);
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rtimes(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }
    
    /* --- Exponentiation */
    
    public IArithmetic raisedTo(Whole e) {
        if (e.isNegative()) {
            return ((Rational) this.inverse()).raisedTo((Whole) e.abs());
        } else {
            return makeFast((Whole) numerator().raisedTo(e),
            		(Whole) denominator().raisedTo(e));
        }
    }
}
