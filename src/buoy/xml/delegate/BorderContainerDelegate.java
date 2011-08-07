package buoy.xml.delegate;

import buoy.widget.*;
import java.beans.*;

/**
 * This class is a PersistenceDelegate for serializing BorderContainers.
 *
 * @author Peter Eastman
 */

public class BorderContainerDelegate extends EventSourceDelegate
{
  /**
   * Create a BorderContainerDelegate.
   */
  
  public BorderContainerDelegate()
  {
  }

  protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out)
  {
    super.initialize(type, oldInstance, newInstance, out);
    BorderContainer oldC = (BorderContainer) oldInstance;
    BorderContainer newC = (BorderContainer) newInstance;
    BorderContainer.Position pos[] = new BorderContainer.Position [] {
        BorderContainer.CENTER, BorderContainer.NORTH, BorderContainer.SOUTH, BorderContainer.EAST, BorderContainer.WEST};
    for (int i = 0; i < pos.length; i++)
      if (oldC.getChild(pos[i]) != newC.getChild(pos[i]))
        out.writeStatement(new Statement(oldInstance, "add", new Object [] {
            oldC.getChild(pos[i]), pos[i], oldC.getChildLayout(pos[i])}));
  }
}
