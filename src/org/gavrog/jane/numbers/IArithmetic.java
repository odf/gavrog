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

package org.gavrog.jane.numbers;

/**
 * Common interface for arithmetic types. Not all methods have to be supported
 * by all derived classes. As a general rule, the result of an operation lies in
 * the least general common superclass of all arguments, if any, in which the
 * operation is defined. If the operation is not defined in any common
 * superclass, an exception is thrown.
 */
public interface IArithmetic extends Comparable<Object> {
    
    // --- Testing special properties.
    
    /**
     * Checks if this arithmetic object is exact.
     * @return true if the object is exact.
     */
    public boolean isExact();

    // --- Basic arithmetic.
    
    /**
     * Constructs an object that acts as an additive zero with respect to this
     * object.
     * 
     * @return the constructed zero object.
     */
    public IArithmetic zero();
    
    /**
     * Constructs an object that acts as a multiplicative one with respect to
     * this object.
     * 
     * @return the constructed one object.
     */
    public IArithmetic one();

    /**
     * Constructs an additive inverse to this object.
     * @return the constructed inverse.
     */
    public IArithmetic negative();
    
    /**
     * Constructs a multiplicative inverse to this object.
     * @return the constructed inverse.
     */
    public IArithmetic inverse();

    /**
     * Adds an object to this number.
     * @param other the object to add.
     * @return the result of the addition.
     */
    public IArithmetic plus(Object other);
    
    /**
     * Adds a long integer to this number.
     * @param other the number to add.
     * @return the result of the addition.
     */
    public IArithmetic plus(long other);
    
    /**
     * Adds a double to this number.
     * @param other the number to add.
     * @return the result of the addition.
     */
    public IArithmetic plus(double other);
    
    /**
     * Subtracts an object from this one.
     * @param other the object to subtract.
     * @return the result of the subtraction.
     */
    public IArithmetic minus(Object other);
    
    /**
     * Subtracts a long integer from this number.
     * @param other the number to subtract.
     * @return the result of the subtraction.
     */
    public IArithmetic minus(long other);
    
    /**
     * Subtracts a double from this number.
     * @param other the number to subtract.
     * @return the result of the subtraction.
     */
    public IArithmetic minus(double other);
    
    /**
     * Multiplies an object to this one (from the right).
     * @param other the object to multiply.
     * @return the result of the multiplication.
     */
    public IArithmetic times(Object other);
    
    /**
     * Multiplies this number with a long integer.
     * @param other the number to multiply with.
     * @return the result of the multiplication.
     */
    public IArithmetic times(long other);
    
    /**
     * Multiplies this number with a double.
     * @param other the number to multiply with.
     * @return the result of the multiplication.
     */
    public IArithmetic times(double other);
    
    /**
     * Divides this arithmetic object by another object.
     * @param other the object to divide by.
     * @return the result of the division.
     */
    public IArithmetic dividedBy(Object other);

    /**
     * Divides this number by a long integer.
     * @param other the number to divide by.
     * @return the result of the division.
     */
    public IArithmetic dividedBy(long other);
    
    /**
     * Divides this number by a double.
     * @param other the number to divide by.
     * @return the result of the division.
     */
    public IArithmetic dividedBy(double other);
    
    /**
     * Truncated division: returns the largest whole number not smaller than the quotient.
     * 
     * @param other the object to divide by.
     * @return the result of the truncated division.
     */
    public IArithmetic div(Object other);

    /**
     * Returns the remainder of a truncated division operation.
     * 
     * @param other the object to divide by.
     * @return the remainder.
     */
    public IArithmetic mod(Object other);

    /**
     * Raises this arithmetic object to a certain power.
     * @param other the exponent to raise to.
     * @return the result of the exponentiation.
     */
    public IArithmetic raisedTo(Object other);

    /**
     * Raises this arithmetic object to a certain power.
     * @param other the exponent to raise to.
     * @return the result of the exponentiation.
     */
    public IArithmetic raisedTo(long other);

    // --- Reverse arithmetic for delegating operations to argument objects.
    
    /**
     * Add this object to another.
     * @param other the object to add to.
     * @return the result of the addition.
     */
    public IArithmetic rplus(IArithmetic other);
    
    /**
     * Subtract this object from another.
     * @param other the object to subtract from.
     * @return the result of the subtraction.
     */
    public IArithmetic rminus(IArithmetic other);
    
    /**
     * Multiply this object to another.
     * @param other the object to multiply by this one.
     * @return the result of the multiplication.
     */
    public IArithmetic rtimes(IArithmetic other);
    
    /**
     * Divides another object by this one.
     * @param other the object to divide by this one.
     * @return the result of the division.
     */
    public IArithmetic rdividedBy(IArithmetic other);

    // --- Tests for equality.
    
    /**
     * Tests if this object acts as an additive zero in its domain.
     * @return true if this object is a zero.
     */
    public boolean isZero();
    
    /**
     * Tests if this object acts as a multiplicative one in its domain.
     * @return true if the object is a one.
     */
    public boolean isOne();

    // --- Comparison methods for ordered domains.
    
    /**
     * Compares this object with another. The result is a negative number if
     * this object is smaller, a positive number if it is larger, and 0 if both
     * objects are comparable, but indistinguishable in their common domain. If
     * the objects are incomparable, an exception is thrown.
     * 
     * @param other the object to compare with.
     * @return a number indicating the relative order of the compared objects.
     */
    public int compareTo(Object other);
    
    /**
     * Constructs the largest whole number not larger than this number.
     * @return the largest whole number not larger than this number.
     */
    public IArithmetic floor();
    
    /**
     * Constructs the whole number closest to this number.
     * @return the whole number closest to this number.
     */
    public IArithmetic round();
    
    /**
     * Tests if this object is smaller than another.
     * @param other the object to compare with.
     * @return true if this object is smaller.
     */
    public boolean isLessThan(Object other);
    
    /**
     * Tests if this object is smaller than or indistinguishable from another.
     * @param other the object to compare with.
     * @return true if this object is smaller or equal.
     */
    public boolean isLessOrEqual(Object other);
    
    /**
     * Tests if this object is larger than another.
     * @param other the object to compare with.
     * @return true if this object is larger.
     */
    public boolean isGreaterThan(Object other);
    
    /**
     * Tests if this object is larger than or indistinguishable from another.
     * @param other the object to compare with.
     * @return true if this object is larger or equal.
     */
    public boolean isGreaterOrEqual(Object other);
    
    // --- Methods related to the sign of an object.
    
    /**
     * Determines the sign of this object, if defined.
     * @return one of the numbers -1, 0 or 1.
     */
    public int sign();

    /**
     * Computes the absolute value of this object.
     * @return the absolute value.
     */
    public IArithmetic abs();
    
    /**
     * Tests if this object is positive (larger than zero).
     * @return true if this object is positive.
     */
    public boolean isPositive();
    
    /**
     * Tests if this is object is non-negative (not smaller than zero).
     * @return true if this object is non-negative.
     */
    public boolean isNonNegative();
    
    /**
     * Tests if this object is positive (larger than zero).
     * @return true if this object is positive.
     */
    public boolean isNegative();

    // --- The norm.
    
    /**
     * Computes the norm of this object.
     * @return the norm.
     */
    public IArithmetic norm();
}
