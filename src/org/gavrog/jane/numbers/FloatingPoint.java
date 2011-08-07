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


public class FloatingPoint extends Real {
    private double val;

    public FloatingPoint(double val) {
        this.val = val;
    }

    public FloatingPoint(Rational val) {
        this.val = val.doubleValue();
    }

    public static final FloatingPoint ZERO = new FloatingPoint(0.0);
    public static final FloatingPoint ONE = new FloatingPoint(1.0);

    public IArithmetic zero() {
        return ZERO;
    }

    public IArithmetic one() {
        return ONE;
    }

    public IArithmetic floor() {
        return new Whole((long) Math.floor(this.val));
    }
    
    public int sign() {
        return (this.val == 0) ? 0 : ((this.val > 0) ? 1 : -1);
    }

    public boolean isExact() {
        return false;
    }

    public boolean isPositive() {
        return this.val > 0;
    }

    public boolean isNegative() {
        return this.val < 0;
    }

    public boolean isZero() {
        return this.val == 0;
    }

    public boolean isOne() {
        return this.val == 1;
    }

    public double doubleValue() {
        return this.val;
    }

    public long longValue() {
        return (long) this.val;
    }

    public String toString() {
        return Double.toString(this.val);
    }

    public IArithmetic negative() {
        return new FloatingPoint(-this.val);
    }

    public IArithmetic inverse() {
        return new FloatingPoint(1.0 / this.val);
    }

    public IArithmetic abs() {
        return new FloatingPoint(Math.abs(this.val));
    }

    public int hashCode() {
        return (new Double(this.val)).hashCode();
    }

    /* --- Addition */
    
    public IArithmetic plus(Object other) {
    	if (other instanceof FloatingPoint) {
            return new FloatingPoint(this.val + ((FloatingPoint) other).val);
    	} else if (other instanceof Rational) {
            return new FloatingPoint(this.val + ((Rational) other).doubleValue());
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rplus(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }

    /* --- Subtraction */
    
    public IArithmetic minus(Object other) {
    	if (other instanceof FloatingPoint) {
            return new FloatingPoint(this.val - ((FloatingPoint) other).val);
    	} else if (other instanceof Rational) {
            return new FloatingPoint(this.val - ((Rational) other).doubleValue());
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rminus(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }
    
    /* --- Multiplication */
    
    public IArithmetic times(Object other) {
    	if (other instanceof FloatingPoint) {
            return new FloatingPoint(this.val * ((FloatingPoint) other).val);
    	} else if (other instanceof Rational) {
            return new FloatingPoint(this.val * ((Rational) other).doubleValue());
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rtimes(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }
    
    /* --- Division */

    public IArithmetic dividedBy(Object other) {
    	if (other instanceof FloatingPoint) {
            return new FloatingPoint(this.val / ((FloatingPoint) other).val);
    	} else if (other instanceof Rational) {
            return new FloatingPoint(this.val / ((Rational) other).doubleValue());
    	} else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rdividedBy(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
    }
}
