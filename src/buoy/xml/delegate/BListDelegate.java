package buoy.xml.delegate;

import buoy.widget.*;
import java.awt.*;
import java.beans.*;
import javax.swing.*;

/**
 * This class is a PersistenceDelegate for serializing BLists.
 *
 * @author Peter Eastman
 */

public class BListDelegate extends EventSourceDelegate
{
  /**
   * Create a BListDelegate.
   */
  
  public BListDelegate()
  {
  }
  
  protected Expression instantiate(Object oldInstance, Encoder out)
  {
    BList old = (BList) oldInstance;
    ListModel defaultModel = (ListModel) getField(old, "defaultModel");
    if (defaultModel != old.getModel())
      return new Expression(old, old.getClass(), "new", new Object [] {old.getModel()});
    Object contents[] = new Object [old.getItemCount()];
    for (int i = 0; i < contents.length; i++)
      contents[i] = old.getItem(i);
    return new Expression(old, old.getClass(), "new", new Object [] {contents});
  }
}
