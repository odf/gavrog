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

public abstract class Real extends Complex implements Comparable {
    public abstract int sign();

    public abstract double doubleValue();
    public abstract long longValue();

    public Real realPart() {
        return this;
    }
    
    public Real imaginaryPart() {
        return Whole.ZERO;
    }
    
    public float floatValue() {
        return (float) doubleValue();
    }

    public double __float__() {
        return doubleValue();
    }
    
    public int __int__() {
        return intValue();
    }
    
   public int intValue() {
        return (int) longValue();
    }

    public short shortValue() {
        return (short) longValue();
    }

    public byte byteValue() {
        return (byte) longValue();
    }

    public int compareTo(Object other) {
        return (this.minus(other)).sign();
    }
    
    public Real sqrt() {
        return new FloatingPoint(Math.sqrt(this.doubleValue()));
    }
   
    public Real acos() {
        return new FloatingPoint(Math.acos(this.doubleValue()));
    }
}
