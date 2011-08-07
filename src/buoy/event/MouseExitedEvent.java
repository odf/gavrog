package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This is an event corresponding to the mouse exiting a Widget.
 *
 * @author Peter Eastman
 */

public class MouseExitedEvent extends WidgetMouseEvent
{
  /**
   * Create a MouseExitedEvent.
   *
   * @param source        the Widget which generated this event
   * @param when          the time at which the event occurred
   * @param modifiers     describes the state of various keys and buttons at the time when the event occurred
   *                      (a sum of the constants defined by InputEvent)
   * @param x             the x coordinate at which the event occurred
   * @param y             the y coordinate at which the event occurred
   */
  
  public MouseExitedEvent(Widget source, long when, int modifiers, int x, int y)
  {
    super(source, MOUSE_EXITED, when, modifiers, x, y, 1, false, NOBUTTON);
  }
}