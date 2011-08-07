package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This is an event corresponding to the mouse being moved inside a Widget.
 *
 * @author Peter Eastman
 */

public class MouseMovedEvent extends WidgetMouseEvent
{
  /**
   * Create a MouseMovedEvent.
   *
   * @param source        the Widget which generated this event
   * @param when          the time at which the event occurred
   * @param modifiers     describes the state of various keys and buttons at the time when the event occurred
   *                      (a sum of the constants defined by InputEvent)
   * @param x             the x coordinate at which the event occurred
   * @param y             the y coordinate at which the event occurred
   */
  
  public MouseMovedEvent(Widget source, long when, int modifiers, int x, int y)
  {
    super(source, MOUSE_MOVED, when, modifiers, x, y, 1, false, NOBUTTON);
  }
}