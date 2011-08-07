/*
   Copyright 2007 Olaf Delgado-Friedrichs

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

import javax.swing.SwingUtilities;

/**
 * @author Olaf Delgado
 * @version $Id: Invoke.java,v 1.2 2008/02/27 08:14:27 odf Exp $
 */
public class Invoke {
    /**
     * Wrapper for {@link javax.swing.SwingUtilities.invokeAndWait}}. If we're in the event dispatch
     * thread, the argument is just invoked normally.
     * 
     * @param runnable what to invoke.
     */
    public static void andWait(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Wrapper for {@link javax.swing.SwingUtilities.invokeLater}}. If we're in the event dispatch
     * thread, the argument is just invoked normally.
     * 
     * @param runnable what to invoke.
     */
    public static void later(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeLater(runnable);
            } catch (Exception ex) {
            }
        }
    }
    
}
