package buoy.xml.delegate;

import buoy.widget.*;
import java.awt.*;
import java.beans.*;

/**
 * This class is a PersistenceDelegate for serializing BTableHeaders.
 *
 * @author Peter Eastman
 */

public class BTableHeaderDelegate extends EventSourceDelegate
{
  /**
   * Create a BTableHeaderDelegate.
   */
  
  public BTableHeaderDelegate()
  {
  }
  
  protected Expression instantiate(Object oldInstance, Encoder out)
  {
    BTable.BTableHeader old = (BTable.BTableHeader) oldInstance;
    BTable table = old.getTable();
    return new Expression(old, table, "getTableHeader", new Object [0]);
  }
}
