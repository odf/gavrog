/*
   Copyright 2012 Olaf Delgado-Friedrichs

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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class ColorWrapper extends Color {
    private static final long serialVersionUID = 7656257584537188197L;

    final private static Map<String, Color> name2color =
		new HashMap<String, Color>();
	final private static Map<Color, String> color2name =
		new HashMap<Color, String>();
	static {
		final Field fields[] = Color.class.getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			try {
				final Field f = fields[i];
				if (f.getType().isAssignableFrom(Color.class)) {
					final Color color = (Color) fields[i].get(null);
					final String name = fields[i].getName();
					name2color.put(name, color);
					if (!color2name.containsKey(color)) {
						color2name.put(color, name);
					}
				}
			} catch (Exception ex) {
			}
		}
	}
	
	public ColorWrapper(final Color c) {
		super(c.getRGB());
	}
	
	public ColorWrapper(final String spec) {
		super(parseColor(spec));
	}

	public static int parseColor(String spec) {
		spec = spec.trim();
		final Color c = name2color.get(spec);
		if (c != null) {
			return c.getRGB();
		} else {
			int n = 0;
			try {
				if (spec.startsWith("#")) {
					n = Integer.parseInt(spec.substring(1), 16);
				} else if (spec.startsWith("0x")) {
					n = Integer.parseInt(spec.substring(2), 16);
				} else if (spec.startsWith("0")) {
					n = Integer.parseInt(spec.substring(1), 8);
				} else {
					n = Integer.parseInt(spec);
				}
			} catch (final NumberFormatException ex) {
			}
			return n;
		}
	}
	
	public String toString() {
		final String name = color2name.get(this);
		if (name != null) {
			return name;
		} else {
			return String.valueOf(getRGB());
		}
	}
}
