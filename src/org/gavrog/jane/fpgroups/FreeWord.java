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

package org.gavrog.jane.fpgroups;

/**
 * Represents a reduced free word of finite length over some alphabet. A reduced
 * free word is a string of signed letters - a negative sign denoting a formal
 * inverse - in which no pair of consecutive entries are just inverses of each
 * other.
 * @author Olaf Delgado
 * @version $Id: FreeWord.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */
public class FreeWord<E> implements Comparable<FreeWord<E>> {

	final private Alphabet<E> alphabet;
	final private int[] data;
	
	/**
	 * Makes an identical copy of a free word.
	 */
	public FreeWord(final FreeWord<? extends E> word) {
	    this(word.getAlphabet(), word.data, false);
	}
	
	/**
	 * Translates a free word into a new alphabet.
	 * @param A the alphabet.
	 */
	public FreeWord(final Alphabet<? extends E> A, final FreeWord<?> word) {
	    this(A, word.data, true);
	}
	
	/**
	 * Constructs a FreeWord instance of length 0.
	 * @param A the alphabet.
	 */
	public FreeWord(final Alphabet<? extends E> A) {
		this(A, null, false);
	}
	
	/**
	 * Constructs a FreeWord instance of length 1.
	 * @param A the alphabet.
	 * @param i the single letter.
	 */
	public FreeWord(final Alphabet<? extends E> A, final int i) {
		this(A, new int[] { i });
	}

	/**
	 * Constructs a FreeWord instance.
	 * @param A the underlying alphabet.
	 * @param data an array of integers describing the word.
	 */
	public FreeWord(final Alphabet<? extends E> A, final int[] data) {
		this(A, data, true);
	}
	
	/**
	 * Constructs a FreeWord instance from a string representation, as for
	 * example "a*b*a^-1*(b*c)^3", where "*" stands for multiplication and
	 * "^" stand for exponentiation. Letter names in this case are limited to
	 * strings which consist solely of ordinary alphabetical letters, digits and
	 * underscores.
	 * @param A the underlying alphabet.
	 * @param word a string representation of the word.
	 */
    public static <X> FreeWord<X> parsedWord(Alphabet<X> A, String word) {
        return new Parser<X>(A, word).getWord();
    }
	
	/**
	 * Constructs a FreeWord instance.
	 * @param alphabet the underlying alphabet.
	 * @param data an array of integers describing the word.
	 * @param check if true, reduce the word first and check for junk.
	 */
	protected <X extends E> FreeWord(final Alphabet<X> alphabet,
	                                 final int[] data, final boolean check) {
	    
	    @SuppressWarnings("unchecked")
        final Alphabet<E> a = (Alphabet<E>) alphabet;
		this.alphabet = a;
		if (data == null) {
			this.data = new int[0];
		} else if (check) {
			final int tmp[] = new int[data.length];
			int k = -1;
			for (int i = 0; i < data.length; ++i) {
				final int next = data[i];
				if (k >= 0 && next == -tmp[k]) {
					--k;
				} else if (next == 0) {
				    // ignore
				} else if (alphabet.letterToName(Math.abs(next)) == null) {
					throw new IllegalArgumentException("illegal entry " + next);
				} else {
					tmp[++k] = next;
				}
			}
			++k;
			this.data = new int[k];
			for (int i = 0; i < k; ++i) {
				this.data[i] = tmp[i];
			}
		} else {
			this.data = new int[data.length];
			for (int i = 0; i < data.length; ++i) {
				this.data[i] = data[i];
			}
		}
	}

	/**
	 * Translates letters in an alphabet to their indices.
	 * @param A the alphabet to use.
	 * @param letters an array of letters
	 * @return the array of corresponding indices.
	 */
	protected static <X> int[] translate (final Alphabet<X> A,
	                                      final X[] letters) {
		final int[] res = new int[letters.length];
		for (int i = 0; i < letters.length; ++i) {
			res[i] = A.nameToLetter(letters[i]);
		}
		return res;
	}

    /**
	 * A tiny recursive descent parser for abstract free word expressions.
	 */
	protected static class Parser<X> {
	    final private Alphabet<X> alphabet;
	    final private String wordAsString;
	    final private int n;
	    private int pos;
	    final private FreeWord<X> word;
	    
	    public Parser(final Alphabet<X> alphabet,
	            final String wordAsString)
	    {
	        if (alphabet == null) {
	            throw new NullPointerException("null argument for aphabet");
	        }
	        if (wordAsString == null) {
	            throw new NullPointerException("null argument for wordAsString");
	        }
	        this.alphabet = alphabet;
	        this.wordAsString = wordAsString;
	        this.n = wordAsString.length();
	        if (this.n == 0 || wordAsString.equals("*")) {
	            this.word = new FreeWord<X>(alphabet);
	            this.pos = this.n;
	        } else {
	            this.pos = 0;
	            this.word = parseWord();
	            if (this.pos < this.n) {
	                throw new IllegalArgumentException("bad syntax in "
                                                       + wordAsString);
	            }
	        }
	    }
	    
	    public FreeWord<X> getWord() {
	        return this.word;
	    }
	    
	    protected boolean atEnd() {
	        return this.pos >= this.n;
	    }
	    
	    protected void skipBlanks() {
	        while (!atEnd() &&
	                Character.isWhitespace(this.wordAsString.charAt(this.pos)))
	        {
	            advance();
	        }
	    }
	    
	    protected char nextChar() {
	        if (atEnd()) {
	            return '$';
	        } else {
	            return this.wordAsString.charAt(this.pos);
	        }
	    }
	    
	    protected void advance() {
	        ++this.pos;
	    }
	    
	    protected FreeWord<X> parseWord() {
            FreeWord<X> tmp = parseFactor();
            skipBlanks();
            while (nextChar() == '*') {
                advance();
                final FreeWord<X> factor = parseFactor();
                tmp = tmp.times(factor);
            }
            return tmp;
        }
	    
	    protected FreeWord<X> parseFactor() {
	        final FreeWord<X> arg;
	        skipBlanks();
	        if (nextChar() == '(') {
	            advance();
	            arg = parseWord();
	            skipBlanks();
	            if (nextChar() != ')') {
	                throw new IllegalArgumentException("unmatched parenthesis");
	            }
	            advance();
	        } else if (nextChar() == '[') {
	            advance();
	            final FreeWord<X> arg1 = parseWord();
	            skipBlanks();
	            if (nextChar() != ',') {
	                final String s = "missing comma in commutator expression";
	                throw new IllegalArgumentException(s);
	            }
	            advance();
	            final FreeWord<X> arg2 = parseWord();
	            skipBlanks();
	            if (nextChar() != ']') {
	                throw new IllegalArgumentException("unmatched '['");
	            }
	            advance();
	            arg = arg1.times(arg2).times(arg1.inverse()).times(
                        arg2.inverse());
	        } else {
	            arg = parseLetter();
	        }
	        skipBlanks();
	        if (nextChar() == '^') {
	            advance();
	            skipBlanks();
	            final StringBuffer buf = new StringBuffer(10);
	            if (nextChar() == '-') {
	                buf.append('-');
	                advance();
	            }
	            while (Character.isDigit(nextChar())) {
	                buf.append(nextChar());
	                advance();
	            }
	            final int exp = Integer.parseInt(buf.toString());
	            return arg.raisedTo(exp);
	        } else {
	            return arg;
	        }
	    }
	    
	    protected FreeWord<X> parseLetter() {
	        skipBlanks();
	        final StringBuffer letter = new StringBuffer(10);
	        while (Character.isLetterOrDigit(nextChar()) || nextChar() == '_') {
	            letter.append(nextChar());
	            advance();
	        }
	        final int i;
	        try {
	            @SuppressWarnings("unchecked")
                final X name = (X) letter.toString();
	            i = this.alphabet.nameToLetter(name);
	        } catch (Exception ex) {
	            throw new IllegalArgumentException();
	        }
	        return new FreeWord<X>(this.alphabet, i);
	    }
	}
	
    /**
     * Returns the number of letters in this word.
     * @return the number of letters.
     */
    public int size() {
        return data.length;
    }
    
	/**
	 * Returns the value of alphabet.
	 * @return the current value of alphabet.
	 */
	public Alphabet<E> getAlphabet() {
		return alphabet;
	}
	
	/**
	 * Returns the length of this word.
	 * @return the number of letters in this word.
	 */
	public int length() {
		return data.length;
	}
	
	/**
	 * Retrieves the letter at a specific position.
	 * @param i the position.
	 * @return the ith letter.
	 */
	public int getLetter(final int i) {
		return Math.abs(data[i]);
	}

	/**
	 * Retrieves the letter at a specific position.
	 * @param i the position.
	 * @return the ith letter.
	 */
	public Object getLetterName(final int i) {
		return alphabet.letterToName(Math.abs(data[i]));
	}

	/**
	 * Returns the sign of the letter at a specific position.
	 * @param i the position.
	 * @return the sign of the ith letter.
	 */
	public int getSign(final int i) {
		if (data[i] < 0) {
			return -1;
		} else {
			return 1;
		}
	}
	
	/**
	 * Extracts a subword.
	 * @param start the smallest index in the subword.
	 * @param end the smallest index after the end of the subword.
	 * @return the specified subword.
	 */
	public FreeWord<E> subword(final int start, final int end) {
	    if (start < 0 || start >= length()) {
	        throw new IllegalArgumentException("start index out of bounds");
	    }
	    if (end < start || end > length()) {
	        throw new IllegalArgumentException("end index out of bounds");
	    }
	    if (end == start) {
	        return new FreeWord<E>(getAlphabet());
	    } else {
	        final int len = end - start;
	        final int tmp[] = new int[len];
	        for (int i = 0; i < len; ++i) {
	            tmp[i] = this.data[i+start];
	        }
	        return new FreeWord<E>(getAlphabet(), tmp, false);
	    }
	}
	
	/**
	 * Computes the product of this with another free word over the same
	 * alphabet.
	 * @param other the other word.
	 * @return the free product of the two words.
	 */
	public FreeWord<E> times(final FreeWord<? extends E> other) {
	    
		if (!this.getAlphabet().equals(other.getAlphabet())) {
			throw new IllegalArgumentException("must have equal alphabets");
		}
		final int n1 = this.data.length;
		final int n2 = other.data.length;
		final int m = Math.min(n1, n2);
		int k = 0;
		while (k < m && other.data[k] == -this.data[n1 - 1 - k]) {
			++k;
		}
		final int out[] = new int[n1 + n2 - 2 * k];
		for (int i = 0; i < n1 - k; ++i) {
			out[i] = this.data[i];
		}
		for (int i = 0; i < n2 - k; ++i) {
			out[n1 - k + i] = other.data[k + i];
		}
		return new FreeWord<E>(this.getAlphabet(), out, false);
	}
	
	/**
	 * Returns the inverse of this word.
	 * @return the inverse.
	 */
	public FreeWord<E> inverse() {
	    final int n = length();
	    final int out[] = new int[n];
	    for (int i = 0; i < n; ++i) {
	        out[i] = -data[n-1-i];
	    }
	    return new FreeWord<E>(getAlphabet(), out, false);
	}
	
    /**
     * Returns this word raised to an integer power, which may be negative.
     * @param n the exponent.
     * @return this word raised to the ith power.
     */
    public FreeWord<E> raisedTo(final int n) {
        final int out[];
        final int m = size();
        
        if (n == 0 || m == 0) {
            out = null;
        } else if (n == 1) {
            out = (int[]) data.clone();
        } else if (n == -1) {
            return inverse();
        } else if (n < 0) {
            return inverse().raisedTo(-n);
        } else {
    		int k = 0;
    		while (k < m && data[k] == -data[m - 1 - k]) {
    			++k;
    		}
    		final int len = n * m - 2 * (n-1) * k;
    		out = new int[len];
    		for (int i = 0; i < k; ++i) {
    		    out[i] = data[i];
    		    out[len - 1 - i] = data[m - 1 - i];
    		}
    		for (int j = 0; j < n; ++j) {
    		    for (int i = k; i < m - k; ++i) {
    		        out[j * (m - 2 * k) + i] = data[i];
    		    }
    		}
        }
        return new FreeWord<E>(this.getAlphabet(), out, false);
    }

	/*
	 * Produces a string representation.
	 */
	public String toString() {
		final StringBuffer buf = new StringBuffer(100);
		if (data.length == 0) {
			buf.append("*");
		} else {
			for (int i = 0; i < data.length; ++i) {
				if (i > 0) {
					buf.append("*");
				}
				buf.append(getLetterName(i));
				if (getSign(i) < 0) {
					buf.append("^-1");
				}
			}
		}
		return buf.toString();
	}

    /**
     * Compares with another word over the same or a different alphabet. The
     * comparison is done lexicographically, first by sign (negative is
     * considered larger), then by index.
     */
    public int compareTo(final FreeWord<E> w) {
        final int n = Math.min(this.length(), w.length());
        for (int i = 0; i < n; ++i) {
            int d = w.getSign(i) - this.getSign(i);
            if (d != 0) {
                return d;
            }
            d = this.getLetter(i) - w.getLetter(i);
            if (d != 0) {
                return d;
            }
        }
        return this.length() - w.length();
    }

    /**
     * Overrides {@link Object#equals(java.lang.Object)}.
     */
    public boolean equals(final Object other) {
        if (other instanceof FreeWord) {
            @SuppressWarnings("unchecked")
            final FreeWord<E> w = (FreeWord<E>) other;
            return this.getAlphabet().equals(w.getAlphabet())
                    && this.compareTo(w) == 0;
        } else {
            return false;
        }
    }
    
    /**
     * Overrides {@link Object#hashCode()}.
     */
    public int hashCode() {
        int code = getAlphabet().hashCode();
        for (int i =0; i < length(); ++i) {
            code = code * 37 + data[i];
        }
        return code;
    }
}
