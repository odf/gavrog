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
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Olaf Delgado
 * @version $Id: Config.java,v 1.4 2007/05/24 23:17:51 odf Exp $
 */
public class Config {
	final private static Map<Class, Class> mappedTypes =
		new HashMap<Class, Class>();
	static {
		mappedTypes.put(int.class, Integer.class);
		mappedTypes.put(long.class, Long.class);
		mappedTypes.put(float.class, Float.class);
		mappedTypes.put(double.class, Double.class);
		mappedTypes.put(boolean.class, Boolean.class);
		mappedTypes.put(Color.class, ColorWrapper.class);
	}
	
	public static Class<?> wrapperType(final Class type) {
		if (mappedTypes.containsKey(type)) {
			return mappedTypes.get(type);
		} else {
			return type;
		}
	}
	
	public static Object construct(final Class type, final String value)
			throws Exception {
		return wrapperType(type).getConstructor(String.class).newInstance(value);
	}

	public static String asString(final Object value) throws Exception {
		if (value == null) {
			return "";
		} else {
			final Class type = value.getClass();
			if (wrapperType(type).equals(type)) {
				return String.valueOf(value);
			} else {
				return String.valueOf(wrapperType(type).getConstructor(type)
						.newInstance(value));
			}
		}
	}
	
	public static void pushProperties(final Properties props, final Object obj)
			throws Exception {
		final Class type = obj.getClass();
		final String prefix = type.getCanonicalName() + ".";
		final BeanInfo info = Introspector.getBeanInfo(type);
		final PropertyDescriptor desc[] = info.getPropertyDescriptors();
		for (int i = 0; i < desc.length; ++i) {
			if (desc[i].getWriteMethod() == null
					|| desc[i].getReadMethod() == null) {
				continue;
			}
			final String value = props.getProperty(prefix + desc[i].getName());
			if (value == null) {
				continue;
			}
			final Method setter = desc[i].getWriteMethod();
            final Class valueType = setter.getParameterTypes()[0];
			setter.invoke(obj, construct(valueType, value));
		}
	}
	
	public static void pullProperties(final Properties props, final Object obj)
			throws Exception {
		final Class type = obj.getClass();
		final String prefix = type.getCanonicalName() + ".";
		final PropertyDescriptor desc[] =
			Introspector.getBeanInfo(type).getPropertyDescriptors();
		for (int i = 0; i < desc.length; ++i) {
			if (desc[i].getWriteMethod() == null
					|| desc[i].getReadMethod() == null) {
				continue;
			}
			props.setProperty(prefix + desc[i].getName(), asString(desc[i]
					.getReadMethod().invoke(obj)));
		}
	}

	public static Properties getProperties(final Object source)
			throws Exception {
		final Properties props = new Properties();
		pullProperties(props, source);
		return props;
	}
	
	public static PropertyDescriptor namedProperty(final Object source,
			final String name) throws Exception {
		final Class type = (source instanceof Class ? (Class) source : source
				.getClass());
		final BeanInfo info = Introspector.getBeanInfo(type);
		final PropertyDescriptor props[] = info.getPropertyDescriptors();
		PropertyDescriptor prop = null;
		for (int i = 0; i < props.length; ++i) {
			if (props[i].getName().equals(name)) {
				prop = props[i];
				break;
			}
		}
		return prop;
	}
}
