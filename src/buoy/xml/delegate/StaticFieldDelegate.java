package buoy.xml.delegate;

import java.lang.reflect.*;
import java.beans.*;

/**
 * This class is a PersistenceDelegate for serializing the values of static fields of classes.  It is
 * used when a class defines various constants, and the appropriate way to "instantiate" one of them
 * is to get it from the appropriate static field.
 * <p>
 * The constructor to this class simply takes the Class object which owns the static field.  When
 * it is asked to serialize an object, it searches the class for a public static final field whose value
 * is the object in question, then records a reference to that field.
 *
 * @author Peter Eastman
 */

public class StaticFieldDelegate extends PersistenceDelegate
{
  private Class cls;
  
  /**
   * Create a StaticFieldDelegate.
   *
   * @param cls     the class which should be searched for the field
   */
  
  public StaticFieldDelegate(Class cls)
  {
    this.cls = cls;
  }
  
  protected Expression instantiate(Object oldInstance, Encoder out)
  {
    Field field[] = cls.getFields();
    for (int i = 0; i < field.length; i++)
    {
      int mod = field[i].getModifiers();
      try
      {
        if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod) && field[i].get(null) == oldInstance)
          return new Expression(oldInstance, field[i], "get", new Object [] {null});
      }
      catch (Exception ex)
      {
      }
    }
    return null;
  }

  protected boolean mutatesTo(Object oldInstance, Object newInstance)
  {
    return (oldInstance == newInstance);
  }
}
