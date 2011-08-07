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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;

import buoy.event.MouseDraggedEvent;
import buoy.event.MousePressedEvent;
import buoy.event.MouseReleasedEvent;
import buoy.event.RepaintEvent;
import buoy.event.ValueChangedEvent;
import buoy.event.WidgetMouseEvent;

public class Slider extends SliderBase {
	private double value;
	private double oldValue;

	public Slider(final double value, final double min, final double max) {
		this.min = min;
		this.max = Math.max(min, max);
		
	    addEventLink(MousePressedEvent.class, this, "mousePressed");
	    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
	    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
	    addEventLink(RepaintEvent.class, this, "paint");
	    
	    setValue(value);
	}
	
	public Dimension getPreferredSize() {
		final int width = showValue ? 210 : 180;
		final int height = showTicks ? 14 : 11;
		return new Dimension(width, height);
	}
	
	public void paint(final RepaintEvent ev) {
		final Graphics2D g = ev.getGraphics();
		if (g == null) return;
		
		g.setStroke(new BasicStroke(1));
		
		clearCanvas(g);
		drawGuide(g);
		drawTicks(g);
		if (showValue) {
			showValue(g);
		}
		double base;
		if (max < 0) {
			base = max;
		} else if (min < 0){
			base = 0;
		} else {
			base = min;
		}
		if (value < base) {
			fillGuide(g, value, base);
		} else {
			fillGuide(g, base, value);
		}
		drawMarker(g, value);
	}

	protected void showValue(final Graphics2D g) {
		final Font f = new Font("Verdana", Font.PLAIN, 10);
		g.setFont(f);
		g.setColor(new Color(0.0f, 0.4f, 0.6f));
		final String s;
		if (value == (int) value) {
			s = String.format("%d", (int) value);
		} else {
			s = String.format("%.2f", value);
		}
		g.drawString(s, sliderWidth() + 8, 10);
	}

	protected int sliderWidth() {
		return getBounds().width - 7 - (showValue ? 30 : 0);
	}
	
	@SuppressWarnings("unused")
	protected void mousePressed(MousePressedEvent ev) {
		oldValue = value;
		mouseDragged(ev);
	}

	protected void mouseDragged(WidgetMouseEvent ev) {
		setValue(xToValue(ev.getPoint().x));
	}

	@SuppressWarnings("unused")
	protected void mouseReleased(MouseReleasedEvent ev) {
		if (value != oldValue) {
			dispatchEvent(new ValueChangedEvent(this));
		}
	}
	
	public double getValue() {
		return value;
	}

	public void setValue(final double newValue) {
		value = newValue;
		if (snapInterval > 0) {
			value = Math.round((value - min) / snapInterval) * snapInterval
					+ min;
		}
		if (value < min) value = min;
		if (value > max) value = max;
		repaint();
	}
}
