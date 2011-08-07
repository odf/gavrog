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

/**
 * @author Olaf Delgado
 * @version $Id: Imaginary.java,v 1.2 2005/07/20 20:34:35 odf Exp $
 */
public class Imaginary extends Complex {
	
	private Real real_part;
	private Real imaginary_part;

	public Imaginary(Real real_part, Real imaginary_part) {
		this.real_part = real_part;
		this.imaginary_part = imaginary_part;
	}
	
	public Imaginary(Real real_part, double imaginary_part) {
		this(real_part, new FloatingPoint(imaginary_part));
	}
	
	public Imaginary(double real_part, Real imaginary_part) {
		this(new FloatingPoint(real_part), imaginary_part);
	}
	
	public Imaginary(double real_part, double imaginary_part) {
		this(new FloatingPoint(real_part), new FloatingPoint(imaginary_part));
	}
	
	public Imaginary(Real real_part) {
		this(real_part, Whole.ZERO);
	}
	
	public Imaginary(double real_part) {
		this(new FloatingPoint(real_part), Whole.ZERO);
	}
	
	public Real realPart() {
		return this.real_part;
	}
	
	public Real imaginaryPart() {
		return this.imaginary_part;
	}
	
	public boolean equals(Object other) {
	    if (other instanceof Complex) {
	        final Complex c = (Complex) other;
	        return realPart().equals(c.realPart())
                   && imaginaryPart().equals(c.imaginaryPart());
	    } else {
	        return false;
	    }
	}
	
	public int compareTo(Object other) {
	    throw new ArithmeticException("imaginary numbers are not ordered");
	}
    
    public IArithmetic floor() {
        throw new ArithmeticException("imaginary numbers are not ordered");
    }
}
