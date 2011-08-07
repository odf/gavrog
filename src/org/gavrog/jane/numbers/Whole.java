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

import java.math.BigInteger;

public class Whole extends Rational {
    private BigInteger val;

    public Whole(long val) {
        this.val = BigInteger.valueOf(val);
    }

    public Whole(BigInteger val) {
        this.val = val;
    }

    public static final Whole ZERO = new Whole(0);
    public static final Whole ONE = new Whole(1);

    public Whole numerator() {
        return this;
    }

    public Whole denominator() {
        return ONE;
    }

    public int sign() {
        return this.val.signum();
    }

    public boolean isZero() {
        return this.val.equals(BigInteger.ZERO);
    }

    public boolean isOne() {
        return this.val.equals(BigInteger.ONE);
    }

    public double doubleValue() {
        return this.val.doubleValue();
    }

    public long longValue() {
        return this.val.longValue();
    }

    public int intValue() {
        return this.val.intValue();
    }

    public String toString() {
        return this.val.toString();
    }

    public IArithmetic negative() {
        return new Whole(this.val.negate());
    }

    public IArithmetic abs() {
        return new Whole(this.val.abs());
    }

    public int hashCode() {
        return this.val.hashCode();
    }

    /* --- Addition */
    
    public IArithmetic plus(Object other) {
    	if (other instanceof Whole) {
            return new Whole(this.val.add(((Whole) other).val));
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rplus(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }

    /* --- Subtraction */
    
    public IArithmetic minus(Object other) {
    	if (other instanceof Whole) {
            return new Whole(this.val.subtract(((Whole) other).val));
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rminus(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }

    /* --- Multiplication */
    
    public IArithmetic times(Object other) {
    	if (other instanceof Whole) {
            return new Whole(this.val.multiply(((Whole) other).val));
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rtimes(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }
    
    /* --- Division */
    
    public IArithmetic dividedBy(Object other) {
    	if (other instanceof Whole) {
            return Rational.make(this, (Whole) other);
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rdividedBy(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }
    
    /* --- Truncated division */
    
    public Whole div(Whole other) {
        // IMPORTANT: We need to override the implementation in ArithmeticBase, because
        // Rational.div() (= ArithmeticBase.div()) calls Rational.floor() which calls
        // Whole.div().
        final Whole tmp = new Whole(this.val.divide(other.val));
        if (tmp.times(other).isGreaterThan(this)) {
            return (Whole) tmp.minus(Whole.ONE);
        } else {
            return tmp;
        }
    }

    /* --- Greatest common divisor */
    
    public Whole gcd(Whole other) {
    	return new Whole(this.val.gcd(other.val));
    }
    
    public Whole gcd(long other) {
    	return new Whole(this.val.gcd(BigInteger.valueOf(other)));
    }
}
