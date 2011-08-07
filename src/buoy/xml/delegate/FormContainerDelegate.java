package buoy.xml.delegate;

import buoy.widget.*;
import java.awt.*;
import java.beans.*;

/**
 * This class is a PersistenceDelegate for serializing FormContainers.
 *
 * @author Peter Eastman
 */

public class FormContainerDelegate extends EventSourceDelegate
{
  /**
   * Create a FormContainerDelegate.
   */
  
  public FormContainerDelegate()
  {
    super(new String [] {"columnCount", "rowCount"});
  }
  
  protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out)
  {
    super.initialize(type, oldInstance, newInstance, out);
    FormContainer oldC = (FormContainer) oldInstance;
    FormContainer newC = (FormContainer) newInstance;
    for (int i = 0; i < oldC.getColumnCount(); i++)
      if (oldC.getColumnWeight(i) != newC.getColumnWeight(i))
        out.writeStatement(new Statement(oldC, "setColumnWeight", new Object [] {
            new Integer(i), new Double(oldC.getColumnWeight(i))}));
    for (int i = 0; i < oldC.getRowCount(); i++)
      if (oldC.getRowWeight(i) != newC.getRowWeight(i))
        out.writeStatement(new Statement(oldC, "setRowWeight", new Object [] {
            new Integer(i), new Double(oldC.getRowWeight(i))}));
    if (oldC.getChildCount() != newC.getChildCount())
      for (int i = 0; i < oldC.getChildCount(); i++)
      {
        Widget child = oldC.getChild(i);
        Rectangle cells = oldC.getChildCells(i);
        LayoutInfo layout = oldC.getChildLayout(i);
        out.writeStatement(new Statement(oldC, "add", new Object [] {
            child, new Integer(cells.x), new Integer(cells.y), new Integer(cells.width),
            new Integer(cells.height), layout}));
      }
  }
}
