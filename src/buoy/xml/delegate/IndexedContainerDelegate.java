package buoy.xml.delegate;

import buoy.widget.*;

import java.beans.*;
import java.lang.reflect.*;

/**
 * This class is a PersistenceDelegate for serializing a variety of WidgetContainers.
 * It assumes the container has a list of children which are indexed by a single integer,
 * and that children can be added to it by calling an "add" method.
 *
 * @author Peter Eastman
 */

public class IndexedContainerDelegate extends EventSourceDelegate
{
  private String propertyMethods[];

  /**
   * Create an IndexedContainerDelegate.
   *
   * @param propertyMethods   the names of a set of public methods which take an int as their only
   *                          argument.  For each child of the container, these methods will
   *                          be invoked and their return values passed to the add() method.
   */

  public IndexedContainerDelegate(String propertyMethods[])
  {
    this.propertyMethods = propertyMethods;
  }

  protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out)
  {
    WidgetContainer old = (WidgetContainer) oldInstance;
    if (old.getChildCount() != ((WidgetContainer) newInstance).getChildCount())
    {
      try
      {
        // Find the list of methods to get property values.

        Method methods[] = new Method [propertyMethods.length];
        Class objClass = old.getClass();
        for (int i = 0; i < methods.length; i++)
          methods[i] = objClass.getMethod(propertyMethods[i], new Class [] {Integer.TYPE});

        // Add the children to the container.

        for (int i = 0; i < old.getChildCount(); i++)
        {
          Object args[] = new Object [methods.length];
          Object index[] = new Object [] {new Integer(i)};
          for (int j = 0; j < args.length; j++)
            args[j] = methods[j].invoke(old, index);
          out.writeStatement(new Statement(oldInstance, "add", args));
        }
      }
      catch (Exception ex)
      {
        out.getExceptionListener().exceptionThrown(ex);
      }
    }
    super.initialize(type, oldInstance, newInstance, out);
  }
}
