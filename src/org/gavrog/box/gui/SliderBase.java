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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

import buoy.widget.CustomWidget;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public abstract class SliderBase extends CustomWidget {
	protected double min;
	protected double max;
	protected boolean showTicks = true;
	protected boolean showValue = true;
	protected double majorTickSpacing = 0.0;
	protected double minorTickSpacing = 0.0;
	protected double snapInterval = 0.0;
	
	protected abstract int sliderWidth();

	protected void clearCanvas(final Graphics2D g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getBounds().width, getBounds().height);
	}

	protected void drawGuide(final Graphics2D g) {
		g.setColor(Color.WHITE);
		g.fillRect(3, 2, sliderWidth(), 5);
		g.setColor(new Color(0.8f, 0.88f, 0.92f));
		g.drawLine(4, 3, sliderWidth() + 2, 3);
		g.setColor(Color.GRAY);
		g.drawRect(3, 2, sliderWidth(), 5);
	}

	protected void drawTicks(final Graphics2D g) {
		if (showTicks) {
			g.setColor(Color.GRAY);
			if (minorTickSpacing > 0) {
				for (double t = min; t <= max; t += minorTickSpacing) {
					final int x = valueToX(t) + 3;
					g.drawLine(x, 8, x, 11);
				}
			}
			if (majorTickSpacing > 0) {
				for (double t = min; t <= max; t += majorTickSpacing) {
					final int x = valueToX(t) + 3;
					g.drawLine(x, 8, x, 14);
				}
			}
		}
	}

	protected void fillGuide(final Graphics2D g, double lo, double hi) {
		final int xlo = valueToX(lo) + 4;
		final int xhi = valueToX(hi) + 2;
	
		g.setColor(new Color(1.0f, 0.6f, 0.2f));
		g.drawLine(xlo, 6, xhi, 6);
		g.setColor(new Color(1.0f, 0.75f, 0.5f));
		g.drawLine(xlo, 5, xhi, 5);
		g.setColor(new Color(1.0f, 0.9f, 0.8f));
		g.drawLine(xlo, 4, xhi, 4);
		g.setColor(Color.WHITE);
		g.drawLine(xlo, 3, xhi, 3);
	}

	protected void drawMarker(final Graphics2D g, final double pos) {
		final int x = valueToX(pos);
		g.setColor(new Color(0.9f, 0.9f, 0.9f));
		g.drawLine(x + 1, 0, x + 1,  9);
		g.drawLine(x + 2, 0, x + 2, 10);
		g.setColor(new Color(0.8f, 0.88f, 0.92f));
		g.drawLine(x + 3, 0, x + 3, 11);
		g.setColor(new Color(0.6f, 0.76f, 0.84f));
		g.drawLine(x + 4, 0, x + 4, 10);
		g.setColor(new Color(0.5f, 0.7f, 0.8f));
		g.drawLine(x + 5, 0, x + 5,  9);
		g.setColor(Color.BLACK);
		g.draw(new Polygon(
				new int[] { x, x + 6, x + 6, x + 3, x },
				new int[] { 0,     0,     8,    11, 8 }, 5));
	}

	protected int valueToX(final double val) {
		return (int) Math.round(sliderWidth() * (val - min) / (max - min));
	}

	protected double xToValue(final int x) {
		return min + (double) x / sliderWidth() * (max - min);
	}

	public void setShowTicks(final boolean b) {
		this.showTicks = b;
	}

	public void setShowValue(final boolean b) {
		this.showValue = b;
	}

	public void setMajorTickSpacing(final double major) {
		this.majorTickSpacing = major;
	}

	public void setMinorTickSpacing(final double minor) {
		this.minorTickSpacing = minor;
	}

	public void setSnapInterval(final double snap) {
		this.snapInterval = snap;
	}
}
