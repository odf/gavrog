package buoy.xml.delegate;

import buoy.widget.*;
import java.awt.*;
import java.beans.*;

/**
 * This class is a PersistenceDelegate for serializing GridContainers.
 *
 * @author Peter Eastman
 */

public class GridContainerDelegate extends EventSourceDelegate
{
  /**
   * Create a GridContainerDelegate.
   */
  
  public GridContainerDelegate()
  {
    super(new String [] {"columnCount", "rowCount"});
  }
  
  protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out)
  {
    super.initialize(type, oldInstance, newInstance, out);
    GridContainer oldC = (GridContainer) oldInstance;
    GridContainer newC = (GridContainer) newInstance;
    if (oldC.getChildCount() != newC.getChildCount())
      for (int col = 0; col < oldC.getColumnCount(); col++)
        for (int row = 0; row < oldC.getRowCount(); row++)
        {
          Widget child = oldC.getChild(col, row);
          if (child == null)
            continue;
          LayoutInfo layout = oldC.getChildLayout(col, row);
          out.writeStatement(new Statement(oldC, "add", new Object [] {
              child, new Integer(col), new Integer(row), layout}));
        }
  }
}
