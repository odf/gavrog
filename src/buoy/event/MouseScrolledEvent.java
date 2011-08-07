package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This class defines an event caused by rotating the scroll wheel on a mouse.
 *
 * @author Peter Eastman
 */

public class MouseScrolledEvent extends MouseWheelEvent implements WidgetEvent
{
  private Widget widget;
  
  /**
   * Create a MouseScrolledEvent.
   *
   * @param source        the Widget which generated this event
   * @param when          the time at which the event occurred
   * @param modifiers     describes the state of various keys and buttons at the time when the event occurred
   *                      (a sum of the constants defined by InputEvent)
   * @param x             the x coordinate at which the event occurred
   * @param y             the y coordinate at which the event occurred
   * @param scrollType    the type of scrolling which should occur in response to this event (either
   *                      WHEEL_UNIT_SCROLL or WHEEL_BLOCK_SCROLL)
   * @param scrollAmount  the number of units which should be scrolled in response to this event
   * @param wheelRotation the total distance the mouse wheel was rotated
   */
  
  public MouseScrolledEvent(Widget source, long when, int modifiers, int x, int y, int scrollType, int scrollAmount, int wheelRotation)
  {
    super(source.getComponent(), MOUSE_WHEEL, when, modifiers, x, y, 1, false, scrollType, scrollAmount, wheelRotation);
    widget = source;
  }

  /**
   * Get the object which generated this event.
   */
  
  public Object getSource()
  {
    // The superclass requires the source to be a Component.  This is overridden so getSource()
    // will still return the Widget itself.
    
    return widget;
  }

  /**
   * Get the Widget which generated this event.
   */
  
  public Widget getWidget()
  {
    return widget;
  }
}
