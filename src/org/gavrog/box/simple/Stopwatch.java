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


package org.gavrog.box.simple;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Delgado
 * @version $Id: Stopwatch.java,v 1.6 2008/04/07 06:32:31 odf Exp $
 */
public class Stopwatch {
    private long accumulated = 0;
    private long start = 0;
    private boolean isRunning;
    
    private static boolean useCpuTime = true;
    final private static Map<String, Stopwatch> named =
    	new HashMap<String, Stopwatch>();
    
    public static Stopwatch global(final String name) {
    	if (!named.containsKey(name)) {
    		named.put(name, new Stopwatch());
    	}
    	return named.get(name);
    }
    
    private static long time() {
    	if (useCpuTime) {
    		final ThreadMXBean tb = ManagementFactory.getThreadMXBean();
    		if (tb.isThreadCpuTimeSupported() && tb.isThreadCpuTimeEnabled()) {
    			return tb.getCurrentThreadUserTime();
    		}
		}
		return System.nanoTime();
    }
    
    public String mode() {
    	if (useCpuTime) {
	    	final ThreadMXBean tb = ManagementFactory.getThreadMXBean();
			if (tb.isThreadCpuTimeSupported() && tb.isThreadCpuTimeEnabled()) {
				return "ThreadMXBean::getCurrentThreadUserTime()";
			}
		}
		return "System.nanoTime()";
    }
    
    public void start() {
        if (!isRunning) {
        	start = time();
            isRunning = true;
        }
    }
    
    public void stop() {
		if (isRunning) {
			accumulated += time() - start;
			isRunning = false;
		}
	}
    
    public void reset() {
    	accumulated = 0;
    	if (isRunning) {
    		start = time();
    	}
    }
    
    /**
     * Reports the elapsed time on this timer in milliseconds.
     * @return the elapsed time in milliseconds.
     */
    public long elapsed() {
    	return (accumulated + (isRunning ? time() - start : 0)) / (long) 1e6;
    }
    
    public String format() {
        return format(elapsed());
    }
    
    public static String format(final long milliseconds) {
    	return milliseconds / 10 / 100.0 + " seconds";
    }
}
