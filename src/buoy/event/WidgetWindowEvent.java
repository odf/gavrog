package buoy.event;

import buoy.widget.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class defines an event caused by the user interacting with a window.  It is an abstract class,
 * with subclasses for specific types of events.
 *
 * @author Peter Eastman
 */

public abstract class WidgetWindowEvent extends WindowEvent implements WidgetEvent
{
  private Widget widget;
  
  /**
   * Create a WidgetWindowEvent.
   *
   * @param source      the window to which this event occurred
   * @param id          the event ID
   */
  
  public WidgetWindowEvent(WindowWidget source, int id)
  {
    super((Window) source.getComponent(), id);
    widget = source;
  }

  /**
   * Get the object which generated this event.
   */
  
  public Object getSource()
  {
    // The superclass requires the source to be a Window.  This is overridden so getSource()
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