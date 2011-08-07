package buoy.xml.delegate;

import buoy.widget.*;
import java.beans.*;

/**
 * This class is a PersistenceDelegate for serializing OverlayContainers.
 *
 * @author Peter Eastman
 */

public class OverlayContainerDelegate extends EventSourceDelegate
{
  /**
   * Create a OverlayContainerDelegate.
   */
  
  public OverlayContainerDelegate()
  {
  }

  protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out)
  {
    super.initialize(type, oldInstance, newInstance, out);
    OverlayContainer old = (OverlayContainer) oldInstance;
    if (old.getChildCount() != ((OverlayContainer) newInstance).getChildCount())
      for (int i = 0; i < old.getChildCount(); i++)
        out.writeStatement(new Statement(oldInstance, "add", new Object [] {
            old.getChild(i), new Integer(i)}));
  }
}
