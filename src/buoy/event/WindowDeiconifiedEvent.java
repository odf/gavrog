package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This event indicates that a window has been deiconified by the user.
 *
 * @author Peter Eastman
 */

public class WindowDeiconifiedEvent extends WidgetWindowEvent
{
  /**
   * Create a WindowDeiconifiedEvent.
   *
   * @param source      the window to which this event occurred
   */
  
  public WindowDeiconifiedEvent(WindowWidget source)
  {
    super(source, WindowEvent.WINDOW_DEICONIFIED);
  }
}