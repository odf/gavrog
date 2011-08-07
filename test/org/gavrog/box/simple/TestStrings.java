package org.gavrog.box.simple;

import junit.framework.TestCase;

public class TestStrings extends TestCase {
	public void testParsable() {
		assertEquals("\"\"", Strings.parsable(" \t", false));
		assertEquals("\"\"", Strings.parsable("", false));
		assertEquals("\"Doctor \\\"Who\\\" ?\"", Strings.parsable(
				" Doc\btor \n \"W\000ho\" \t?  ", false));
		assertEquals("_Under_Scores_", Strings
				.parsable("_Under_Scores_", false));
		assertEquals("\"_Under_%_Scores_\"", Strings
				.parsable("_Under_%_Scores_", true));
	}
}
