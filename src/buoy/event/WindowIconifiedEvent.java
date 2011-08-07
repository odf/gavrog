package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This event indicates that a window has been iconified by the user.
 *
 * @author Peter Eastman
 */

public class WindowIconifiedEvent extends WidgetWindowEvent
{
  /**
   * Create a WindowIconifiedEvent.
   *
   * @param source      the window to which this event occurred
   */
  
  public WindowIconifiedEvent(WindowWidget source)
  {
    super(source, WindowEvent.WINDOW_ICONIFIED);
  }
}