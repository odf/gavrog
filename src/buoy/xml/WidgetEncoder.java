package buoy.xml;

import buoy.event.*;
import buoy.widget.*;
import buoy.xml.delegate.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * This class is used for serializing Widgets as XML.  This allows user interfaces to be saved in
 * a persistent form, then reconstructed using the WidgetDecoder class.
 * <p>
 * To use this class, simply call {@link buoy.xml.WidgetEncoder#writeObject WidgetEncoder.writeObject()},
 * passing it the root Widget
 * to save and an OutputStream to write the XML to.  It will save that Widget, along with all other objects
 * referenced by it: child Widgets, event listeners, models, etc.
 * <p>
 * The serialization is done through the java.beans.XMLEncoder class.  It therefore can encode any object
 * which is a bean.  An object which is not a bean can also be saved if a PersistenceDelegate has been
 * defined for it.  The buoy.xml.delegate package includes PersistenceDelegates for all of the Widgets
 * in the buoy.widget package.  If you wish to define delegates for other classes, you can do so by
 * calling {@link buoy.xml.WidgetEncoder#setPersistenceDelegate WidgetEncoder.setPersistenceDelegate()}.
 * If the class in question is an EventSource, the delegate should generally subclass {@link EventSourceDelegate}
 * and invoke <code>initializeEventLinks()</code> from its <code>initialize()</code> method so that its
 * event listeners will be properly saved.
 * <p>
 * It is possible for XML files to contain localized text, where the actual value to use is
 * supplied from a ResourceBundle at decoding time.  See {@link WidgetLocalization} for details.
 *
 * @author Peter Eastman
 */

public class WidgetEncoder
{
  private static HashMap delegateTable;
  private static EventSourceDelegate defaultDelegate;
  
  static
  {
    defaultDelegate = new EventSourceDelegate();
    delegateTable = new HashMap();
  }
  
  /**
   * WidgetEncoder defines only static methods, and should never be instantiated.
   */
  
  private WidgetEncoder()
  {
  }
  
  /**
   * Serialize an object hierarchy as XML.  The OutputStream will be closed once the XML is written.
   *
   * @param obj        the object which forms the root of the hierarchy to save
   * @param out        the OutputStream to write the XML to
   */
  
  public static void writeObject(Object obj, OutputStream out)
  {
    writeObject(obj, out, null);
  }
  
  /**
   * Serialize an object hierarchy as XML.  If recoverable errors occur during serialization,
   * the listener will be notified of them.  The OutputStream will be closed once the XML is written.
   *
   * @param obj        the object which forms the root of the hierarchy to save
   * @param out        the OutputStream to write the XML to
   * @param listener   the listener to notify of recoverable errors
   */
  
  public static void writeObject(Object obj, OutputStream out, ExceptionListener listener)
  {
    // If a window is being encoding, make sure the duplicate copy will never actually be shown.
    
    ThreadLocal encodingInProgress = null; 
    try
    {
      Field f = WindowWidget.class.getDeclaredField("encodingInProgress");
      f.setAccessible(true);
      encodingInProgress = (ThreadLocal) f.get(null);
    }
    catch (Exception ex)
    {
    }
    if (encodingInProgress != null)
      encodingInProgress.set(Boolean.TRUE);
    
    // Write out the object.
    
    WidgetXMLEncoder encoder = new WidgetXMLEncoder(out);
    if (listener != null)
      encoder.setExceptionListener(listener);
    encoder.writeExpression(new Expression(Class.class, "forName", new Object [] {"buoy.xml.WidgetLocalization"}));
    String localized[] = WidgetLocalization.getAllLocalizedStrings();
    for (int i = 0; i < localized.length; i++)
      encoder.writeExpression(new Expression(localized[i], WidgetLocalization.class, "getLocalizedString", new Object [] {new String(localized[i])}));
    encoder.writeObject(obj);
    encoder.close();
    
    // Reset the flag.
    
    if (encodingInProgress != null)
      encodingInProgress.set(Boolean.FALSE);
  }
  
  /**
   * Register a persistence delegate for a class.
   *
   * @param cls         the class for which to define a PersistenceDelegate
   * @param delegate    the delegate to use for saving that class
   */
  
  public static void setPersistenceDelegate(Class cls, PersistenceDelegate delegate)
  {
    delegateTable.put(cls, delegate);
  }
  
  /**
   * This inner class is the XMLEncoder used for doing the writing.
   */
  
  private static class WidgetXMLEncoder extends XMLEncoder
  {
    public WidgetXMLEncoder(OutputStream out)
    {
      super(out);
    }
    
    public PersistenceDelegate getPersistenceDelegate(Class cls)
    {
      if (cls != null)
      {
        PersistenceDelegate delegate = (PersistenceDelegate) delegateTable.get(cls);
        if (delegate != null)
          return delegate;
        if (EventSource.class.isAssignableFrom(cls))
          return defaultDelegate;
      }
      return super.getPersistenceDelegate(cls);
    }
  }
}

