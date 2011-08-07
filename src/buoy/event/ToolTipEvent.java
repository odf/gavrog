package buoy.event;

import buoy.widget.*;
import java.awt.*;
import java.util.*;

/**
 * This event indicates that the user has performed the series of actions which signal that a
 * tool tip should be displayed.  Generally, this involves placing the mouse pointer over a Widget and
 * not moving it for a certain amount of time.
 * <p>
 * For more information on how to display tool tips, see the {@link buoy.widget.BToolTip BToolTip}
 * class.
 *
 * @author Peter Eastman
 */

public class ToolTipEvent extends EventObject implements WidgetEvent
{
  private Widget widget;
  private long when;
  private Point pos, tipPos;

  /**
   * Create a ToolTipEvent.
   *
   * @param source        the Widget which generated this event
   * @param when          the time at which the event occurred
   * @param pos           the position of the mouse pointer
   * @param tipPos        the position at which the tool tip should be displayed
   */
  
  public ToolTipEvent(Widget source, long when, Point pos, Point tipPos)
  {
    super(source);
    widget = source;
    this.when = when;
    this.pos = pos;
    this.tipPos = tipPos;
  }
  
  /**
   * Get the Widget which generated this event.
   */
  
  public Widget getWidget()
  {
    return widget;
  }
  
  /**
   * Get the time at which the event occurred.
   */
  
  public long getWhen()
  {
    return when;
  }
  
  /**
   * Get the position of the mouse pointer.
   */
  
  public Point getPoint()
  {
    return pos;
  }
  
  /**
   * Get the suggested position at which the tool tip should be displayed.
   */
  
  public Point getToolTipLocation()
  {
    return tipPos;
  }
}
