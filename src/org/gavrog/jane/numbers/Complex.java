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

/*
 * Created on Jun 18, 2004 by delgado.
 */
package org.gavrog.jane.numbers;

/**
 * Marker interface for complex numbers.
 * 
 * @author Olaf Delgado
 * @version $Id: Complex.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */

public abstract class Complex extends ArithmeticBase {

	public abstract Real realPart();
	public abstract Real imaginaryPart();
	
	public static final Complex I = new Imaginary(Whole.ZERO, Whole.ONE);
	
    public Complex make(Real re, Real im) {
		if (im.isZero()) {
			return re;
		} else {
			return new Imaginary(re, im);
		}
	}	
	
	public boolean isExact() {
		return realPart().isExact() && imaginaryPart().isExact();
	}

	public IArithmetic zero() {
		return Whole.ZERO;
	}

	public IArithmetic one() {
		return Whole.ONE;
	}

	public IArithmetic i() {
	    return I;
	}
	
	public IArithmetic negative() {
		return make((Real) realPart().negative(), (Real) imaginaryPart()
				.negative());
	}

	public IArithmetic inverse() {
		Real re = realPart();
		Real im = imaginaryPart();
		IArithmetic d = re.times(re).plus(im.times(im));
		re = (Real) realPart().dividedBy(d);
		im = (Real) imaginaryPart().negative().dividedBy(d);
		return make(re, im);
	}

	public IArithmetic plus(Object other) {
		if (other instanceof Imaginary) {
			Real re = (Real) realPart().plus(((Imaginary) other).realPart());
			Real im = (Real) imaginaryPart().plus(
					((Imaginary) other).imaginaryPart());
			return make(re, im);
		} else if (other instanceof Real) {
			Real re = (Real) realPart().plus(other);
			return make(re, this.imaginaryPart());
		} else if (other instanceof IArithmetic) {
			return ((IArithmetic) other).rplus(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
	}

	public IArithmetic times(Object other) {
		if (other instanceof Imaginary) {
			Real re1 = this.realPart();
			Real im1 = this.imaginaryPart();
			Real re2 = ((Imaginary) other).realPart();
			Real im2 = ((Imaginary) other).imaginaryPart();
			Real re = (Real) re1.times(re2).minus(im1.times(im2));
			Real im = (Real) re1.times(im2).plus(im1.times(re2));
			return make(re, im);
		} else if (other instanceof Real) {
			Real re = (Real) realPart().times(other);
			Real im = (Real) imaginaryPart().times(other);
			return make(re, im);
		} else if (other instanceof IArithmetic) {
			return ((IArithmetic) other).rtimes(this);
    	} else {
    		throw new IllegalArgumentException();
    	}
	}

	public IArithmetic norm() {
		Real re = realPart();
		Real im = imaginaryPart();
		Real d = (Real) re.times(re).plus(im.times(im));
		return new FloatingPoint(Math.sqrt(d.doubleValue()));
	}

	public String toString() {
		if (imaginaryPart().isPositive()) {
			return realPart().toString() + "+" + imaginaryPart().toString() + "i";
		} else {
			return realPart().toString() + imaginaryPart().toString() + "i";
		}
	}

	public int hashCode() {
        return 31 * realPart().hashCode() + imaginaryPart().hashCode();
	}
}
