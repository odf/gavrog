package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This is an event corresponding to the mouse button being pressed and released.
 *
 * @author Peter Eastman
 */

public class MouseClickedEvent extends WidgetMouseEvent
{
  /**
   * Create a MouseClickedEvent.
   *
   * @param source        the Widget which generated this event
   * @param when          the time at which the event occurred
   * @param modifiers     describes the state of various keys and buttons at the time when the event occurred
   *                      (a sum of the constants defined by InputEvent)
   * @param x             the x coordinate at which the event occurred
   * @param y             the y coordinate at which the event occurred
   * @param clickCount    the number of successive times the mouse has been clicked
   * @param popupTrigger  true if this event corresponds to the platform-specific trigger for displaying
   *                      popup menus
   * @param button        the flag for the button which has just changed state
   */
  
  public MouseClickedEvent(Widget source, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger, int button)
  {
    super(source, MOUSE_CLICKED, when, modifiers, x, y, clickCount, popupTrigger, button);
  }
}