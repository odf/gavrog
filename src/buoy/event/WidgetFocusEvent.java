package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This class defines an event caused by a change in whether a Widget has keyboard focus.
 * This is an abstract class, with subclasses for particular types of events.
 *
 * @author Peter Eastman
 */

public abstract class WidgetFocusEvent extends FocusEvent implements WidgetEvent
{
  private Widget widget;
  
  /**
   * Create a WidgetFocusEvent.
   *
   * @param source        the Widget which generated this event
   * @param id            the event ID
   * @param temporary     specifies whether this represents a permanent or temporary change in focus state
   */
  
  protected WidgetFocusEvent(Widget source, int id, boolean temporary)
  {
    super(source.getComponent(), id, temporary);
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