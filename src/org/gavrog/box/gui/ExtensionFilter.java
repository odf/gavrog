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

package org.gavrog.box.gui;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.filechooser.FileFilter;

public class ExtensionFilter extends FileFilter {
	final private Set<String> extensions = new HashSet<String>();
	final private String description;

	public ExtensionFilter(final String extensions[], final String description) {
		final StringBuffer desc = new StringBuffer(description);
		desc.append("  - ");
		for (int i = 0; i < extensions.length; ++i) {
			final String ext = extensions[i];
			this.extensions.add(ext);
			desc.append(" *.");
			desc.append(ext);
		}
		this.description = desc.toString();
	}

	public ExtensionFilter(final String extension, final String description) {
		this(new String[] { extension }, description);
	}
	
	public boolean accept(final File f) {
		if (f.isDirectory()) {
			return true;
		}
		final String name = f.getName();
		final int i = name.lastIndexOf('.');
		final String ext = name.substring(i+1);
		return extensions.contains(ext.toLowerCase());
	}

	public String getDescription() {
		return this.description;
	}
}
