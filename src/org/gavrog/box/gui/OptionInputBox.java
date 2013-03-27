/*
   Copyright 2013 Olaf Delgado-Friedrichs

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

import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import buoy.event.EventProcessor;
import buoy.event.EventSource;
import buoy.event.ToolTipEvent;
import buoy.event.ValueChangedEvent;
import buoy.widget.BLabel;
import buoy.widget.BTextField;
import buoy.widget.BToolTip;
import buoy.widget.BorderContainer;
import buoy.widget.LayoutInfo;

public class OptionInputBox extends BorderContainer {
	private boolean eventsLocked = false;
	
	private BTextField input;

	public OptionInputBox(
			final String label, final Object target, final String option)
			throws Exception {
		this(label, target, option, 5, null);
	}
	
    public OptionInputBox(
            final String label, final Object target, final String option,
            final String toolTip)
            throws Exception {
        this(label, target, option, 5, toolTip);
    }
    
	public OptionInputBox(
			final String label, final Object target, final String option,
			final int size, final String toolTip) throws Exception {

		super();
		this.setBackground(null);
		this.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
				new Insets(2, 10, 2, 10), null));

        if (toolTip != null)
            this.addEventLink(ToolTipEvent.class, new BToolTip(toolTip));

		this.input = new BTextField(size);
		this.add(input, BorderContainer.WEST);
		this.add(new BLabel(label), BorderContainer.EAST);

		final PropertyDescriptor prop = Config.namedProperty(target, option);
		if (prop == null) {
			throw new IllegalArgumentException("Target class has no property "
					+ option);
		}
		final Method getter = prop.getReadMethod();
		final Method setter = prop.getWriteMethod();
		final Class<?> optionType = setter.getParameterTypes()[0];

		this.input.setText(Config.asString(getter.invoke(target)));
		this.input.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				if (obtainLock()) {
					try {
						setter.invoke(target,
								Config.construct(optionType, input.getText()));
					} catch (final Exception ex) {
					}
					releaseLock();
				}
			}
		});
		
		if (target instanceof EventSource) {
			final EventSource s = (EventSource) target;
			s.addEventLink(PropertyChangeEvent.class, new EventProcessor() {
				public void handleEvent(Object event) {
					if (obtainLock()) {
						final PropertyChangeEvent e = (PropertyChangeEvent) event;
						try {
							if (e.getPropertyName().equals(option)) {
								input.setText(Config.asString(e.getNewValue()));
							}
						} catch (Exception ex) {
						}
						releaseLock();
					}
				}
			});
		}
	}
	
	private boolean obtainLock() {
		if (this.eventsLocked) {
			return false;
		} else {
			this.eventsLocked = true;
			return true;
		}
	}
	
	private void releaseLock() {
		this.eventsLocked = false;
	}
}
