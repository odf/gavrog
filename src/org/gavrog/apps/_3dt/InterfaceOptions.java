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

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.lang.reflect.Field;

import buoy.event.EventSource;

/**
 * Contains global (not document specific) options for 3dt.
 * 
 * @author Olaf Delgado
 * @version $Id:$
 */
public class InterfaceOptions extends EventSource {
    private int viewerWidth = 800;
    private int viewerHeight = 800;
    private double rotationStep = 5.0;

    private String _3dtHome = System.getProperty("3dt.home");
    private String userHome = System.getProperty("user.home");
	private File lastInputPath = new File(_3dtHome + "/Data");
	private File lastNetOutputPath = new File(userHome);
	private File lastTilingOutputPath = new File(userHome);
	private File lastSceneOutputPath = new File(userHome);
	private File lastObjExportPath = new File(userHome);
	private File lastSunflowRenderPath = new File(userHome);
	private File lastScreenshotPath = new File(userHome);
    
	/**
	 * Generic setter method. Uses introspection to find the field to set and
	 * generates a PropertyChangeEvent if the value has changed.
	 * @param name  the name of the field to set.
	 * @param value the new value.
	 */
	private void _setField(final String name, final Object value) {
		try {
			final Field field = this.getClass().getDeclaredField(name);
			final Object old = field.get(this);
			if ((value == null) ? (old != null) : (!value.equals(old))) {
				dispatchEvent(new PropertyChangeEvent(this, name, old, value));
				field.set(this, value);
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}
	
    public int getViewerWidth() {
    	return this.viewerWidth;
    }
    
	public void setViewerWidth(final int value) {
		_setField("viewerWidth", value);
	}

    public int getViewerHeight() {
    	return this.viewerHeight;
    }
    
	public void setViewerHeight(final int value) {
		_setField("viewerHeight", value);
	}

	public double getRotationStep() {
		return this.rotationStep;
	}
	
	public void setRotationStep(final double value) {
		_setField("rotationStep", Math.max(value, 0.1));
	}
	
	public File getLastInputPath() {
		return lastInputPath;
	}

	public void setLastInputPath(final File value) {
		_setField("lastInputPath", value);
	}

	public File getLastNetOutputPath() {
		return this.lastNetOutputPath;
	}

	public void setLastNetOutputPath(final File value) {
		_setField("lastNetOutputPath", value);
	}

	public File getLastSceneOutputPath() {
		return this.lastSceneOutputPath;
	}

	public void setLastSceneOutputPath(final File value) {
		_setField("lastSceneOutputPath", value);
	}

	public File getLastObjExportPath() {
		return this.lastObjExportPath;
	}

	public void setLastObjExportPath(final File value) {
		_setField("lastObjExportPath", value);
	}

	public File getLastTilingOutputPath() {
		return this.lastTilingOutputPath;
	}

	public void setLastTilingOutputPath(final File value) {
		_setField("lastTilingOutputPath", value);
	}

	public File getLastSunflowRenderPath() {
		return this.lastSunflowRenderPath;
	}

	public void setLastSunflowRenderPath(final File value) {
		_setField("lastSunflowRenderPath", value);
	}

	public File getLastScreenshotPath() {
		return this.lastScreenshotPath;
	}

	public void setLastScreenshotPath(final File value) {
		_setField("lastScreenshotPath", value);
	}
}
