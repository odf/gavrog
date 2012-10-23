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

package org.gavrog.joss.algorithms;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.simple.NamedConstant;

/**
 * Abstract base class for generators, defining the basic branch-and-cut
 * strategy.
 */
public abstract class BranchAndCut<T> extends ResumableGenerator<T> {
	// --- set to true to enable logging
	final private static boolean LOGGING = false;

	public static class Status extends NamedConstant {
		// --- move was performed okay
		final public static Status OK = new Status("ok");

		// --- void move; changes nothing
		final public static Status VOID = new Status("void");

		// --- move contradicts current state
		final public static Status ILLEGAL = new Status("illegal");

		private Status(final String name) {
			super(name);
		}
	}

	public static class Type extends NamedConstant {
		// --- no actual move, just indicates a choice to be made
		final public static Type CHOICE = new Type("Choice");
		
		// --- a decision made upon a choice
		final public static Type DECISION = new Type("Decision");
		
		// --- a consequence of previous decisions
		final public static Type DEDUCTION = new Type("Deduction");
		
		private Type(final String name) {
			super(name);
		}
	}
	
	public interface Move {}
	
	private class BC_Move {
		final private Move data;      // -- description of the move
		final private Type type;      // -- what type of move
		final private int decisionNr; // -- decision count at the current choice
		
		/**
		 * Creates a new instance.
		 * 
		 * @param type the type of this move
		 */
		public BC_Move(final Move data, final Type type, final int decisionNr) {
			this.data = data;
			this.type = type;
			this.decisionNr = decisionNr;
		}
	
		/**
		 * @return true if this move is a decision made upon a choice.
		 */
		public boolean isDecision() {
			return type == Type.DECISION;
		}
		
		/**
		 * @return true if this move is a consequence of previous decisions.
		 */
		public boolean isDeduction() {
			return type == Type.DEDUCTION;
		}
		
		/**
		 * @return the index of the current decision upon the current choice
		 */
		public int getDecisionNumber() {
			return decisionNr;
		}
		
		public Move getData() {
			return data;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return String.format("<%s, %s, %s>", getData(), type,
					getDecisionNumber());
		}
	}

	// --- flag set when generation is done
	private boolean done = false;

	// --- the generation history
	final private LinkedList<BC_Move> stack = new LinkedList<BC_Move>();

    // --- point within the search tree at which to resume an old computation
	private int resume[] = new int[] {};
	
	// --- the current progress towards the resume point
	private int resume_level = 0;
	private int resume_stack_level = 0;
	private boolean resume_point_reached = false;
	
	/**
	 * If logging is enabled, print a message to the standard error stream.
	 * 
	 * @param text the message to print.
	 */
	protected void log(final String text) {
		if (LOGGING) {
			System.out.println("# " + text);
			System.out.flush();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
	 */
	protected T findNext() throws NoSuchElementException {
		if (this.done) {
			throw new NoSuchElementException();
		}

		log("");
		log("entering findNext(): stack size = " + this.stack.size());

		if (stack.size() == 0) {
			final Move choice = nextChoice(null);
			if (choice == null) {
				log("leaving findNext(): no initial choice");
				this.done = true;
				throw new NoSuchElementException();
			}
			log("  adding initial choice " + choice);
			this.stack.addLast(new BC_Move(choice, Type.CHOICE, 0));
		}

		while (true) {
        	if (!resume_point_reached && resume_level >= resume.length) {
				resume_point_reached = true;
				if (resume.length > 0) {
					postCheckpoint("resume point reached");
				}
				if (resume_level > resume.length) {
					log("  past resume point at [" + getCheckpoint() + "]");
				} else {
					log("  resume point reached at [" + getCheckpoint() + "]");
				}
			}
			final BC_Move decision = undoLastDecision();
			if (decision == null) {
				log("leaving findNext(): no more decisions to undo");
				this.done = true;
				throw new NoSuchElementException();
			}
			log("  last decision was " + decision);
            if (!resume_point_reached && stack.size() < resume_stack_level) {
				resume_point_reached = true;
				if (resume.length > 0) {
					postCheckpoint("resume point reached");
				}
				log("  past resume point at [" + getCheckpoint() + "]");
			}

			final Move move = nextDecision(decision.getData());
			if (move == null) {
				log("  no potential move");
				continue;
			}
			final BC_Move next = new BC_Move(move, Type.DECISION,
					decision.getDecisionNumber() + 1);
			log("  found potential move " + next);
			
			final int old_stack_size = stack.size();
			final boolean success = performMoveAndDeductions(next);
			postCheckpoint(null);
            if (!resume_point_reached
            		&& old_stack_size == resume_stack_level
					&& resume_level < resume.length
					&& next.getDecisionNumber() == resume[resume_level]) {
				resume_stack_level = stack.size();
				resume_level += 1;
				log("  resume level raised to " + resume_level
						+ " at stack level " + resume_stack_level);
			}
			
			if (success) {
				if (isValid()) {
					final T result = isComplete() ? makeResult() : null;
					final Move choice = nextChoice(move);
					if (choice != null) {
						if (resume_point_reached
								|| stack.size() == resume_stack_level) {
							log("  adding choice " + choice);
							this.stack.addLast(new BC_Move(choice, Type.CHOICE,
									0));
						}
					}
					if (result != null) {
						log("leaving findNext() with result " + result);
						return result;
					}
				} else {
					log("  result or move is not valid");
				}
			} else {
				log("  move was rejected");
			}
		}
	}

	private boolean performMoveAndDeductions(final BC_Move initial) {
		// --- we maintain a queue of deductions, starting with the initial move
		final LinkedList<BC_Move> queue = new LinkedList<BC_Move>();
		queue.addLast(initial);

		while (queue.size() > 0) {
			// --- get the next move from the queue
			final BC_Move move = (BC_Move) queue.removeFirst();

			// --- see if the move can be performed
			final Status status = checkMove(move.getData());

			// --- a void move has no consequences
			if (status == Status.VOID) {
				continue;
			}

			// --- if the move was illegal, return immediately
			if (status == Status.ILLEGAL) {
				log("    move " + move + " is impossible; backtracking");
				this.stack.addLast(move); // -- record move so it can be undone
				return false;
			}

			// --- perform and record the move
			performMove(move.getData());
			this.stack.addLast(move);

			// --- finally, find and enqueue deductions
			final List<Move> deductions = deductions(move.getData());
			if (deductions != null) {
				for (final Move d: deductions) {
					log("    adding deduction " + d);
					queue.add(new BC_Move(d, Type.DEDUCTION, 0));
				}
			}
		}

		return true;
	}

	private BC_Move undoLastDecision() {
		while (stack.size() > 0) {
			final BC_Move last = this.stack.removeLast();
			if (last == null) {
				continue;
			}

			log("  undoing " + last);
			undoMove(last.getData());

			if (!last.isDeduction()) {
				return last;
			}
		}
		return null;
	}

	private void postCheckpoint(final String message) {
		dispatchEvent(
		        new CheckpointEvent(this, !resume_point_reached, message));
	}

	/**
	 * Retreives the current checkpoint value as a string.
	 * 
	 * @return the current checkpoint.
	 */
    public String getCheckpoint() {
    	final StringBuffer buf = new StringBuffer(20);
    	for (BC_Move move: stack) {
    		if (move.isDecision()) {
    			if (buf.length() > 0) {
    				buf.append('-');
    			}
    			buf.append(move.decisionNr);
    		}
    	}
    	return buf.toString();
    }
    
    /**
     * Sets the point in the search tree at which the algorithm should resume.
     * 
     * @param resume specifies the checkpoint to resume execution at.
     */
    public void setResumePoint(final String spec) {
    	if (spec == null || spec.length() == 0) {
    		return;
    	}
    	final String fields[] = spec.trim().split("-");
    	resume = new int[fields.length];
    	for (int i = 0; i < fields.length; ++i) {
    		resume[i] = Integer.valueOf(fields[i]);
    	}
    }

	// --- The following methods have to implemented by every derived class:

	/**
	 * Constructs a {@link Move} object which describes the next choice to make
	 * given the current state.
	 * 
	 * @param previous the last decision made (<code>null</code> at start).
	 * @return the next choice to make.
	 */
	abstract protected Move nextChoice(final Move previous);

	/**
	 * Constructs a {@link Move} object which describes the next possible way to
	 * decide upon the given choice.
	 * 
	 * @param previous the current choice or the last decision regarding that choice.
	 * @return the next decision for the given choice.
	 */
	abstract protected Move nextDecision(final Move previous);

	/**
	 * Returns the status of the given move: OK if it can be performed as
	 * requested, VOID if it would not change the current state, and ILLEGAL if
	 * it conflicts with the current state.
	 * 
	 * @return the status of the move (OK, VOID or ILLEGAL).
	 */
	abstract protected Status checkMove(final Move move);

	/**
	 * Performs the given move which must have been established as legal and
	 * non-void by calling {@link #checkMove(Move)}.
	 * 
	 * @param move the move to perform.
	 */
	abstract protected void performMove(final Move move);

	/**
	 * Undo the given move under the assumption that it was the last move
	 * performed on the path to the current state.
	 * 
	 * @param move
	 *            the move to undo.
	 */
	abstract protected void undoMove(final Move move);

	/**
	 * Determine forced moves (deductions) based on the current state and the
	 * last move performed to reach it.
	 * 
	 * @param move the last move performed.
	 * @return the list of forced moves.
	 */
	abstract protected List<Move> deductions(final Move move);

	/**
	 * Implements a final test of the current state after a decision move and a
	 * possible series of deductions. This might, for example, check if the
	 * current state describes a partial result in canonical form, or implement
	 * any other tests that would be too costly to be performed after every
	 * elementary move.
	 * 
	 * @return true if the current state is well-formed.
	 */
	abstract protected boolean isValid();

	/**
	 * Checks if the current state describes a complete result.
	 * 
	 * @return true if a complete result can be constructed.
	 */
	abstract protected boolean isComplete();

	/**
	 * Constructs an output object based on the current state. It is supposed
	 * that the current state describes a complete result, as verified by
	 * calling {@link #isComplete()}.
	 * 
	 * @return the result of the current state.
	 */
	abstract protected T makeResult();
}
