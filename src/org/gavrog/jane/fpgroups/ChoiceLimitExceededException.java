package org.gavrog.jane.fpgroups;

/**
 * Thrown when the iterator exceeds its time limit.
 */
public class ChoiceLimitExceededException extends RuntimeException {
	private static final long serialVersionUID = 849754526953863491L;

	public ChoiceLimitExceededException(final String msg) {
        super(msg);
    }
}
