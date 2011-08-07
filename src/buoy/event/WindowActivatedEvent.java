package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This is an event corresponding to a window becoming the active window.
 *
 * @author Peter Eastman
 */

public class WindowActivatedEvent extends WidgetWindowEvent
{
  /**
   * Create a WindowActivatedEvent.
   *
   * @param source      the window to which this event occurred
   */
  
  public WindowActivatedEvent(WindowWidget source)
  {
    super(source, WindowEvent.WINDOW_ACTIVATED);
  }
}