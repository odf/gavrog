package org.gavrog.apps.systre;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class WriteBuiltinArchive {
	public static void main(final String args[]) {
		final SystreCmdline systre = new SystreCmdline();
		try {
			final Writer out = new FileWriter(args[0]);
			systre.writeBuiltinArchive(out);
			out.flush();
			out.close();
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
