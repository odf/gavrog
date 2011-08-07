package buoy.xml.delegate;

import buoy.widget.*;
import java.awt.*;
import java.beans.*;
import javax.swing.table.*;

/**
 * This class is a PersistenceDelegate for serializing BTables.
 *
 * @author Peter Eastman
 */

public class BTableDelegate extends EventSourceDelegate
{
  /**
   * Create a BTableDelegate.
   */
  
  public BTableDelegate()
  {
  }
  
  protected Expression instantiate(Object oldInstance, Encoder out)
  {
    BTable old = (BTable) oldInstance;
    TableModel defaultModel = (TableModel) getField(old, "defaultModel");
    if (defaultModel != old.getModel())
      return new Expression(old, old.getClass(), "new", new Object [] {old.getModel()});
    Object contents[][] = new Object [old.getRowCount()][old.getColumnCount()];
    for (int i = 0; i < contents.length; i++)
      for (int j = 0; j < contents[i].length; j++)
        contents[i][j] = old.getCellValue(i, j);
    Object titles[] = new Object [old.getColumnCount()];
    for (int i = 0; i < titles.length; i++)
      titles[i] = old.getColumnHeader(i);
    return new Expression(old, old.getClass(), "new", new Object [] {contents, titles});
  }

  protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out)
  {
    super.initialize(type, oldInstance, newInstance, out);
    BTable oldTable = (BTable) oldInstance;
    BTable newTable = (BTable) newInstance;
    int numCol = oldTable.getColumnCount();
    int numRow = oldTable.getRowCount();
    for (int i = 0; i < numCol; i++)
      if (oldTable.getColumnWidth(i) != newTable.getColumnWidth(i))
        out.writeStatement(new Statement(oldTable, "setColumnWidth", new Object [] {
            new Integer(i), new Integer(oldTable.getColumnWidth(i))}));
    for (int i = 0; i < numRow; i++)
      if (oldTable.getRowHeight(i) != newTable.getRowHeight(i))
        out.writeStatement(new Statement(oldTable, "setRowHeight", new Object [] {
            new Integer(i), new Integer(oldTable.getRowHeight(i))}));
    TableModel defaultModel = (TableModel) getField(oldTable, "defaultModel");
    if (defaultModel == oldTable.getModel())
      for (int i = 0; i < numCol; i++)
        if (oldTable.isColumnEditable(i) != newTable.isColumnEditable(i))
          out.writeStatement(new Statement(oldTable, "setColumnEditable", new Object [] {
              new Integer(i), new Boolean(oldTable.isColumnEditable(i))}));
  }
}
