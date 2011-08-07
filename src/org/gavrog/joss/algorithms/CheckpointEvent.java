/*
   Copyright 2008 Olaf Delgado-Friedrichs

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

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class CheckpointEvent {
	final private ResumableGenerator source;
	final private boolean old;
	final private String message;
	
	public CheckpointEvent(final ResumableGenerator source, final boolean old,
			final String message) {
		this.source = source;
		this.old = old;
		this.message = message;
	}

	public ResumableGenerator getSource() {
		return source;
	}

	public String getCheckpoint() {
		return getSource().getCheckpoint();
	}
	
	public boolean isOld() {
		return old;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String toString() {
		final String msg = getMessage();
		final String p = isOld() ? "# OLD" : "#@";
		final String c = getCheckpoint();
		final String s = msg != null ? String.format(" (%s)", msg) : "";
		return String.format("%s CHECKPOINT %s%s", p, c, s);
	}
}
