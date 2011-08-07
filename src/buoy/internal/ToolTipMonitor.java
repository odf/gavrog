package buoy.internal;

import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * ToolTipMonitor receives mouse events from Widgets, and sends out ToolTipEvents whenever the
 * appropriate trigger action occurs.
 *
 * @author Peter Eastman
 */

public class ToolTipMonitor
{
  private static WidgetMouseEvent lastMoveEvent;
  private static Timer timer;
  
  private static final int SHOW_DELAY = 1500;
  private static final int UPDATE_DELAY = 250;
  
  static
  {
    timer = new Timer(SHOW_DELAY, new ActionListener() {
      public void actionPerformed(ActionEvent ev)
      {
        SwingUtilities.invokeLater(new Runnable() {
          public void run()
          {
            if (lastMoveEvent == null)
            {
              BToolTip.hide();
              return;
            }
            Widget widget = lastMoveEvent.getWidget();
            if (!widget.getComponent().isDisplayable())
              return;
            Point lastPos = lastMoveEvent.getPoint();
            Point tipPos = new Point(lastPos.x, lastPos.y+15);
            widget.dispatchEvent(new ToolTipEvent(widget, lastMoveEvent.getWhen(), lastMoveEvent.getPoint(), tipPos));
          }
        });
      }
    });
    timer.setRepeats(false);
    timer.stop();
  }
    
  /**
   * Process a WidgetMouseEvent.
   */
  
  public static void processMouseEvent(WidgetMouseEvent ev)
  {
    if (ev instanceof MouseExitedEvent || ev instanceof MousePressedEvent)
    {
      lastMoveEvent = null;
      timer.setInitialDelay(UPDATE_DELAY);
      timer.restart();
    }
    else if (ev instanceof MouseMovedEvent)
    {
      lastMoveEvent = (MouseMovedEvent) ev;
      timer.setInitialDelay(BToolTip.getShowingToolTip() == null ? SHOW_DELAY : UPDATE_DELAY);
      timer.restart();
    }
  }
}

 
