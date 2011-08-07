package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This event indicates that the user is attempting to close a window, such as by clicking on
 * its close box.
 *
 * @author Peter Eastman
 */

public class WindowClosingEvent extends WidgetWindowEvent
{
  /**
   * Create a WindowClosingEvent.
   *
   * @param source      the window to which this event occurred
   */
  
  public WindowClosingEvent(WindowWidget source)
  {
    super(source, WindowEvent.WINDOW_CLOSING);
  }
}