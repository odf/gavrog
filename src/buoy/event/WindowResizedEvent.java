package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This event indicates that a window has been resized by the user.
 *
 * @author Peter Eastman
 */

public class WindowResizedEvent extends WidgetWindowEvent
{
  /**
   * Create a WindowResizedEvent.
   *
   * @param source      the widget to which this event occurred
   */
  
  public WindowResizedEvent(WindowWidget source)
  {
    super(source, ComponentEvent.COMPONENT_RESIZED);
  }
}