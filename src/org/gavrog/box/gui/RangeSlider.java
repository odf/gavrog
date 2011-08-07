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
import java.awt.Point;

import buoy.event.MouseDraggedEvent;
import buoy.event.MousePressedEvent;
import buoy.event.MouseReleasedEvent;
import buoy.event.RepaintEvent;
import buoy.event.ValueChangedEvent;
import buoy.event.WidgetMouseEvent;

public class RangeSlider extends SliderBase {
	private double lo;
	private double hi;
	private double oldLo;
	private double oldHi;
	private boolean draggingLo;
	private boolean draggingHi;

	public RangeSlider(final double lo, final double hi, final double min,
			final double max) {
		this.min = min;
		this.max = Math.max(min, max);
		
	    addEventLink(MousePressedEvent.class, this, "mousePressed");
	    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
	    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
	    addEventLink(RepaintEvent.class, this, "paint");
	    
	    setLow(lo);
	    setHigh(hi);
	}
	
	public Dimension getPreferredSize() {
		final int width = showValue ? 240 : 180;
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
		fillGuide(g, lo, hi);
		drawMarker(g, lo);
		drawMarker(g, hi);
	}

	protected void showValue(final Graphics2D g) {
		final Font f = new Font("Verdana", Font.PLAIN, 10);
		g.setFont(f);
		g.setColor(new Color(0.0f, 0.4f, 0.6f));
		final String s;
		if (lo == (int) lo && hi == (int) hi) {
			s = String.format("%d:%d", (int) lo, (int) hi);
		} else {
			s = String.format("%.2f:%.2f", lo, hi);
		}
		g.drawString(s, sliderWidth() + 8, 10);
	}

	protected int sliderWidth() {
		return getBounds().width - 7 - (showValue ? 60 : 0);
	}
	
	@SuppressWarnings("unused")
	protected void mousePressed(MousePressedEvent ev) {
		draggingLo = draggingHi = false;
		oldLo = lo;
		oldHi = hi;
		mouseDragged(ev);
	}

	protected void mouseDragged(WidgetMouseEvent ev) {
		final int x = ev.getPoint().x;
		decide(x);
		if (draggingHi) {
			setHigh(xToValue(x));
		} else if (draggingLo) {
			setLow(xToValue(x));
		}
	}

	protected void decide(final int x) {
		if (draggingLo || draggingHi) {
			return;
		}
		final int xlo = valueToX(lo);
		final int xhi = valueToX(hi);
		if (x < xlo || xhi > xlo && 3 * x < 2 * xlo + xhi) {
			draggingLo = true;
		} else if (x > xhi + 5 || xhi > xlo && 3 * x > xlo + 2 * xhi) {
			draggingHi = true;
		}
	}
	
	@SuppressWarnings("unused")
	protected void mouseReleased(MouseReleasedEvent ev) {
		final Point pos = ev.getPoint();
		if (lo != oldLo || hi != oldHi) {
			dispatchEvent(new ValueChangedEvent(this));
		}
	}

	public double getLow() {
		return lo;
	}

	public double getHigh() {
		return hi;
	}

	public void setLow(final double newValue) {
		lo = newValue;
		if (snapInterval > 0) {
			lo = Math.round((lo - min) / snapInterval) * snapInterval + min;
		}
		if (lo < min) lo = min;
		if (lo > hi) lo = hi;
		repaint();
	}

	public void setHigh(final double newValue) {
		hi = newValue;
		if (snapInterval > 0) {
			hi = Math.round((hi - min) / snapInterval) * snapInterval + min;
		}
		if (hi < lo) hi = lo;
		if (hi > max) hi = max;
		repaint();
	}
}
