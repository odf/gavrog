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

public class Fraction extends Rational {
    private Whole num;
    private Whole den;

    public Fraction(Whole num, Whole den, boolean normalize) {
    	if (normalize) {
			Whole m = num.gcd(den);
			if (den.isNegative()) {
				m = (Whole) m.negative();
			}
			if (!m.isOne()) {
				num = num.div(m);
				den = den.div(m);
			}
            if (den.isZero()) {
                throw new ArithmeticException("zero denominator");
            }
            if (num.isZero()) {
                den = Whole.ONE;
            }
		}
        this.num = num;
        this.den = den;
    }

    public Fraction(Whole num, Whole den) {
    	this(num, den, true);
    }
    
    public Fraction(long num, Whole den) {
        this(new Whole(num), den);
    }

    public Fraction(Whole num, long den) {
        this(num, new Whole(den));
    }

    public Fraction(long num, long den) {
        this(new Whole(num), new Whole(den));
    }

    public Whole numerator() {
        return this.num;
    }

    public Whole denominator() {
        return this.den;
    }
}
