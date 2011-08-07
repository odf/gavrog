package buoy.internal;

import buoy.widget.*;
import java.awt.*;
import javax.swing.JPanel;

/**
 * This is a JPanel subclass, which is used internally by various WidgetContainers.  It contains a
 * single Widget, and matches its minimum, maximum, and preferred sizes to the Widget.
 *
 * @author Peter Eastman
 */

public class SingleWidgetPanel extends JPanel
{
  protected Widget widget;
  
  public SingleWidgetPanel(Widget widget)
  {
    super(new BorderLayout());
    this.widget = widget;
    add(widget.getComponent(), BorderLayout.CENTER);
  }
  
  public Dimension getMinimumSize()
  {
    return widget.getMinimumSize();
  }
  
  public Dimension getMaximumSize()
  {
    return widget.getMaximumSize();
  }
  
  public Dimension getPreferredSize()
  {
    return widget.getPreferredSize();
  }
}
