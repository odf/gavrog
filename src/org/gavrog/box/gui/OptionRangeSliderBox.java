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

import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;

import org.gavrog.box.simple.Strings;

import buoy.event.EventProcessor;
import buoy.event.EventSource;
import buoy.event.ValueChangedEvent;
import buoy.widget.BLabel;
import buoy.widget.BorderContainer;
import buoy.widget.LayoutInfo;

public class OptionRangeSliderBox extends BorderContainer {
	private boolean eventsLocked = false;
	private final RangeSlider slider;
	
	public OptionRangeSliderBox(final String label, final Object target,
			final String optionLo, final String optionHi, final double min,
			final double max, final double major, final double minor,
			final double snap) throws Exception {
		super();
		this.setBackground(null);

		this.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST,
				LayoutInfo.HORIZONTAL, new Insets(2, 8, 2, 8), null));

		slider = new RangeSlider(min, min, min, max);
		slider.setBackground(null);
		slider.setShowTicks(true);
		slider.setShowValue(true);
		slider.setMajorTickSpacing(major);
		slider.setMinorTickSpacing(minor);
		slider.setSnapInterval(snap);
		this.add(slider, BorderContainer.WEST);
		this.add(new BLabel(label), BorderContainer.EAST, new LayoutInfo(
				LayoutInfo.WEST, LayoutInfo.NONE, new Insets(2, 10, 2, 10),
				null));
		
		final Class<?> klazz = (target instanceof Class ? (Class) target
				: target.getClass());

		final String optionLoCap = Strings.capitalized(optionLo);
		final String optionHiCap = Strings.capitalized(optionHi);
		final Method getterLo = klazz.getMethod("get" + optionLoCap);
		final Method setterLo;
		final Method getterHi = klazz.getMethod("get" + optionHiCap);
		final Method setterHi;
		
		Method t;
		try {
			t = klazz.getMethod("set" + optionLoCap, int.class);
		} catch (NoSuchMethodException ex) {
			t = klazz.getMethod("set" + optionLoCap, double.class);
		}
		setterLo = t;
		try {
			t = klazz.getMethod("set" + optionHiCap, int.class);
		} catch (NoSuchMethodException ex) {
			t = klazz.getMethod("set" + optionHiCap, double.class);
		}
		setterHi = t;

		updateLoValue(getterLo.invoke(target));
		updateHiValue(getterHi.invoke(target));

		slider.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				if (obtainLock()) {
					try {
						Object arg = slider.getLow();
						if (setterLo.getParameterTypes()[0].equals(int.class)) {
							arg = (int) Math.round((Double) arg);
						}
						setterLo.invoke(target, arg);
						arg = slider.getHigh();
						if (setterHi.getParameterTypes()[0].equals(int.class)) {
							arg = (int) Math.round((Double) arg);
						}
						setterHi.invoke(target, arg);
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
						PropertyChangeEvent e = (PropertyChangeEvent) event;
						if (e.getPropertyName().equals(optionLo)) {
							updateLoValue(e.getNewValue());
						} else if (e.getPropertyName().equals(optionHi)) {
							updateHiValue(e.getNewValue());
						}
						releaseLock();
					}
				}
			});
		}
	}
	
	private void updateLoValue(final Object newValue) {
		final double val = ((Number) newValue).doubleValue();
		if (val > slider.getHigh()) {
			slider.setHigh(val);
		}
		slider.setLow(val);
	}
	
	private void updateHiValue(final Object newValue) {
		final double val = ((Number) newValue).doubleValue();
		if (val < slider.getLow()) {
			slider.setLow(val);
		}
		slider.setHigh(val);
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

	public void setShowValue(boolean show) {
		this.slider.setShowValue(show);
	}

	public void setShowTicks(boolean show) {
		this.slider.setShowTicks(show);
	}

	public void setSnapInterval(final double snap) {
		this.slider.setSnapInterval(snap);
	}
}
