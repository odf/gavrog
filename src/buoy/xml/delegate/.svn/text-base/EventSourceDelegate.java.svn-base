package buoy.xml.delegate;

import buoy.internal.*;
import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * This class is a PersistenceDelegate for serializing EventSources.  It extends DefaultPersistenceDelegate
 * to record the list of event links.
 *
 * @author Peter Eastman
 */

public class EventSourceDelegate extends DefaultPersistenceDelegate
{
  /**
   * Create an EventSourceDelegate.
   */
  
  public EventSourceDelegate()
  {
  }
  
  /**
   * Create an EventSourceDelegate for a class whose constructor takes a list of property values as arguments.
   */

  public EventSourceDelegate(String constructorPropertyNames[])
  {
    super(constructorPropertyNames);
  }

  protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out)
  {
    super.initialize(type, oldInstance, newInstance, out);
    initializeEventLinks(oldInstance, newInstance, out);
  }
  
  /**
   * This is called from initialize().  It initializes the list of event links for the object.
   */
  
  protected void initializeEventLinks(Object oldInstance, Object newInstance, Encoder out)
  {
    ArrayList oldLinks = (ArrayList) getField(newInstance, "eventLinks");
    ArrayList newLinks = (ArrayList) getField(oldInstance, "eventLinks");
    if (newLinks == null)
      return; // There are no event links.
    for (int i = 0; i < newLinks.size(); i++)
    {
      EventLinkRecord rec = (EventLinkRecord) newLinks.get(i);
      ArrayList targetList = (ArrayList) getField(rec, "targetList");
      ArrayList methodList = (ArrayList) getField(rec, "targetMethodList");
      if (targetList == null || methodList == null)
        continue;
      for (int j = 0; j < targetList.size(); j++)
      {
        Method method = (Method) methodList.get(j);
        if (!alreadyHasLink(oldLinks, rec.getEventType(), out.get(targetList.get(j)), method))
          out.writeStatement(new Statement(oldInstance, "addEventLink", new Object [] {
              rec.getEventType(), targetList.get(j), method.getName()}));
      }
    }
  }

  /**
   * Given the list of event links on oldInstance, determine whether a particular link has already
   * been added.
   */

  private boolean alreadyHasLink(ArrayList oldLinks, Class eventType, Object target, Method method)
  {
    if (oldLinks == null)
      return false;
    for (int i = 0; i < oldLinks.size(); i++)
    {
      EventLinkRecord rec = (EventLinkRecord) oldLinks.get(i);
      if (rec.getEventType() != eventType)
        continue;
      ArrayList targetList = (ArrayList) getField(rec, "targetList");
      ArrayList methodList = (ArrayList) getField(rec, "targetMethodList");
      if (targetList == null || methodList == null)
        continue;
      for (int j = 0; j < targetList.size(); j++)
        if (targetList.get(j) == target && methodList.get(j).equals(method))
          return true;
    }
    return false;
  }

  /**
   * This is a utility routine.  It uses reflection to look up the value of a particular field of
   * an object.  It makes use of the AccessibleObject API so that it can access private and protected
   * fields of other classes.
   *
   * @param obj      the object for which to get the field value
   * @param field    the name of the field to look up
   * @return the value of the field, or null if it could not be found
   */
  
  protected static Object getField(Object obj, String field)
  {
    Class cls = obj.getClass();
    while (cls != null)
    {
      try
      {
        Field f = cls.getDeclaredField(field);
        f.setAccessible(true);
        return f.get(obj);
      }
      catch (Exception ex)
      {
        cls = cls.getSuperclass();
      }
    }
    return null;
  }
}
