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


package org.gavrog.apps._3dt;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;

import buoy.widget.BFileChooser;
import buoy.widget.BStandardDialog;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class FileChooser extends BFileChooser {
	private boolean append = false;
	private boolean appendEnabled = false;
	
	public FileChooser(final SelectionMode mode, final String title) {
		super(mode, title);
	}
	
	public FileChooser(final SelectionMode mode) {
		super(mode, defaultTitle(mode));
	}
	
	private static String defaultTitle(final SelectionMode mode) {
		if (mode == OPEN_FILE) {
			return "Open File";
		} else if (mode == SAVE_FILE) {
			return "Save File";
		} else if (mode == SELECT_FOLDER) {
			return "Select Folder";
		} else {
			return "Select File";
		}
	}
	
	public File pickFile(final File path, final String ext) {
		if (path.isDirectory()) {
			setDirectory(path);
		} else {
			setSelectedFile(path);
		}
		final boolean success = showDialog(null);
		if (!success) {
			return null;
		}
		File selected = getSelectedFile();
		final String filename = selected.getName();
		if (ext != null && !ext.equals("") && filename.indexOf('.') < 0) {
			selected = new File(selected.getAbsolutePath() + "." + ext);
		}
		append = false;
		if (getMode() == SAVE_FILE && selected.exists()) {
			if (appendEnabled) {
				int choice = showOverwriteDialog(selected, new String[] {
						"Overwrite", "Append", "Cancel" }, "Cancel");
				if (choice > 1) {
					return null;
				} else {
					append = (choice == 1);
				}
			} else {
				int choice = showOverwriteDialog(selected, new String[] {
						"Overwrite", "Cancel" }, "Cancel");
				if (choice > 0) {
					return null;
				}
			}
		}
		return selected;
	}
	
    private int showOverwriteDialog(final File file, final String[] choices,
    		final String defaultChoice) {
    	return new BStandardDialog("3dt - File exists", "File \"" + file
				+ "\" already exists. Overwrite?", BStandardDialog.QUESTION)
				.showOptionDialog(null, choices, defaultChoice);
    }
    
	public void addChoosableFileFilter(final FileFilter filter) {
		getComponent().addChoosableFileFilter(filter);
	}

	public void setAccessory(final JComponent accessory) {
		getComponent().setAccessory(accessory);
	}

	public boolean getAppend() {
		return this.append;
	}

	public boolean getAppendEnabled() {
		return this.appendEnabled;
	}

	public void setAppendEnabled(boolean appendEnabled) {
		this.appendEnabled = appendEnabled;
	}
}
