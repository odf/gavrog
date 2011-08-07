package buoy.xml.delegate;

import buoy.widget.*;
import java.beans.*;

/**
 * This class is a PersistenceDelegate for serializing BSplitPanes.
 *
 * @author Peter Eastman
 */

public class BSplitPaneDelegate extends EventSourceDelegate
{
  /**
   * Create a BSplitPaneDelegate.
   */
  
  public BSplitPaneDelegate()
  {
  }

  protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out)
  {
    super.initialize(type, oldInstance, newInstance, out);
    BSplitPane old = (BSplitPane) oldInstance;
    if (old.getChildCount() != ((BSplitPane) newInstance).getChildCount())
      for (int i = 0; i < old.getChildCount(); i++)
        out.writeStatement(new Statement(oldInstance, "add", new Object [] {
            old.getChild(i), new Integer(i)}));
  }
}
