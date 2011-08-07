package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;
import java.awt.*;

/**
 * This class is used to create a Widget that is a thin wrapper around an arbitrary AWT/Swing component.
 * 
 * @author Peter Eastman
 */

public class AWTWidget extends Widget
{
  static
  {
    WidgetEncoder.setPersistenceDelegate(AWTWidget.class, new EventSourceDelegate(new String [] {"component"}));
  }

  /**
   * Create a Widget which acts as a wrapper around an arbitrary AWT/Swing component.
   */

  public AWTWidget(Component comp)
  {
    component = comp;
  }

  /**
   * This method should be called any time this Widget's minimum, maximum, or preferred size changes.
   * It notifies the Widget's parent container so that it can discard any cached layout information.
   * Calling this method does not actually cause the layout to change.  To do that, you must call
   * layoutChildren() on the WidgetContainer.
   */

  public void invalidateSize()
  {
    super.invalidateSize();
  }
}