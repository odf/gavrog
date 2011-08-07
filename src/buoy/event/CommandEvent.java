package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This event represents a user action that issues a command, such as pressing a button or selecting
 * a menu item.
 *
 * @author Peter Eastman
 */

public class CommandEvent extends ActionEvent implements WidgetEvent
{
  private Widget widget;
  
  /**
   * Create a CommandEvent.
   *
   * @param source      the widget to which this event occurred
   * @param when        the time at which the event occurred
   * @param modifiers   describes the state of various keys at the time when the event occurred
   *                    (a sum of the constants defined by ActionEvent)
   */
  
  public CommandEvent(Widget source, long when, int modifiers)
  {
    this(source, when, modifiers, null);
  }

  /**
   * Create a CommandEvent.
   *
   * @param source      the widget to which this event occurred
   * @param when        the time at which the event occurred
   * @param modifiers   describes the state of various keys at the time when the event occurred
   *                    (a sum of the constants defined by ActionEvent)
   * @param command     the command String describing the action which was performed
   */
  
  public CommandEvent(Widget source, long when, int modifiers, String command)
  {
    super(source, ActionEvent.ACTION_PERFORMED, command, when, modifiers);
    widget = source;
  }
  
  /**
   * Get the Widget which generated this event.
   */
  
  public Widget getWidget()
  {
    return widget;
  }
}