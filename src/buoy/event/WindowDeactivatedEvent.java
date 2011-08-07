package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This is an event corresponding to a window ceasing to be the active window.
 *
 * @author Peter Eastman
 */

public class WindowDeactivatedEvent extends WidgetWindowEvent
{
  /**
   * Create a WindowDeactivatedEvent.
   *
   * @param source      the window to which this event occurred
   */
  
  public WindowDeactivatedEvent(WindowWidget source)
  {
    super(source, WindowEvent.WINDOW_DEACTIVATED);
  }
}