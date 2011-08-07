package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This is an event corresponding to a Widget gaining keyboard focus.
 *
 * @author Peter Eastman
 */

public class FocusGainedEvent extends WidgetFocusEvent
{
  /**
   * Create a FocusGainedEvent.
   *
   * @param source        the Widget which generated this event
   * @param temporary     specifies whether this represents a permanent or temporary change in focus state
   */
  
  public FocusGainedEvent(Widget source, boolean temporary)
  {
    super(source, FocusEvent.FOCUS_GAINED, temporary);
  }
}