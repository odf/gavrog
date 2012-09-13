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

package org.gavrog.box.simple;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class TaskController {
	final static private Map<Thread, TaskController> controllers =
	        new HashMap<Thread, TaskController>();

	final private Thread thread;
	private boolean cancelled = false;

	protected TaskController(final Thread thread) {
		this.thread = thread;
	}

	public synchronized static TaskController getInstance(final Thread thread) {
		TaskController instance = (TaskController) controllers.get(thread);
		if (instance == null) {
			instance = new TaskController(thread);
			controllers.put(thread, instance);
		}
		return instance;
	}

	public static TaskController getInstance() {
		return getInstance(Thread.currentThread());
	}

	public static void cleanup() {
		final List<Thread> dead = new LinkedList<Thread>();
		for (final Thread thread: controllers.keySet()) {
			if (!thread.isAlive()) {
				dead.add(thread);
			}
		}
		for (final Thread trash: dead) {
			controllers.remove(trash);
		}
	}

	public synchronized void cancel() {
		this.cancelled = true;
	}

	public synchronized void reset() {
		this.cancelled = false;
	}

	public synchronized void bailOutIfCancelled() {
		if (this.cancelled) {
			this.cancelled = false;
			throw new TaskStoppedException(this.thread);
		}
	}

	public static void main(final String args[]) {
		for (int i = 0; i < 10; ++i) {
			final Thread thread = new Thread(new Runnable() {
				public void run() {
					final TaskController cntrl = TaskController.getInstance();

					for (long x = 0; x < 10000000; ++x) {
						try {
							cntrl.bailOutIfCancelled();
						} catch (TaskStoppedException ex) {
							System.err.println("Stopped at x = " + x);
							return;
						}
					}
					System.err.println("Finished");
				}
			});
			System.err.print(i + ": ");
			thread.start();
			final TaskController controller = TaskController
					.getInstance(thread);
			try {
				Thread.sleep(0, 0);
				if (i % 3 != 0) {
					controller.cancel();
				}
				thread.join();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		TaskController.cleanup();
	}
}
