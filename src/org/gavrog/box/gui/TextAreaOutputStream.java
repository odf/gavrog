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

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.SwingUtilities;

import buoy.widget.BScrollBar;
import buoy.widget.BScrollPane;
import buoy.widget.BTextArea;
import buoy.widget.Widget;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class TextAreaOutputStream extends OutputStream {
	final private BTextArea output;
    final private StringBuffer buffer;
    final private BScrollPane scrollPane;
	final private BScrollBar vscroll;
	private char last_seen = '\0';

    public TextAreaOutputStream() {
        buffer = new StringBuffer(128);
    	output = new BTextArea(20, 40);
		scrollPane = new BScrollPane(output, BScrollPane.SCROLLBAR_AS_NEEDED,
				BScrollPane.SCROLLBAR_ALWAYS);
		scrollPane.setForceHeight(true);
		scrollPane.setForceWidth(true);
		vscroll = scrollPane.getVerticalScrollBar();
		scrollPane.setBackground(null);
    }
    
    public void write(int b) throws IOException {
        final char c = (char) b;
		if (c == '\n' || c == '\r') {
			if (last_seen != '\r') {
				buffer.append('\n');
				flush();
			}
		} else if (c == '\t') {
			buffer.append(c);
		} else if (c < 0x20) {
			buffer.append('^');
			buffer.append((char) (c + 0x40));
		} else if (!Character.isISOControl(c)) {
			buffer.append(c);
		}
		if (buffer.length() > 1023) {
            flush();
        }
        last_seen = c;
    }
    
    public void flush() {
        output.append(buffer.toString());
        buffer.delete(0, buffer.length());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                vscroll.setValue(vscroll.getMaximum());
            }
        });
    }
    
    public Widget getWidget() {
    	return scrollPane;
    }
}
