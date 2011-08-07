package buoy.xml;

import buoy.widget.*;
import java.beans.*;
import java.io.*;
import java.util.*;

/**
 * This class is used for reconstructing user interfaces that were serialized as XML by WidgetDecoder.
 * <p>
 * To use this class, simply create a new WidgetDecoder, passing it an input stream from which it can
 * read the XML file.  You then can call {@link buoy.xml.WidgetDecoder#getRootObject getRootObject()}
 * to get the root object of the hierarchy, or {@link buoy.xml.WidgetDecoder#getObject getObject()}
 * to look up any Widget in the file by name.
 * <p>
 * Note that WidgetDecoder does <i>not</i> close the stream you pass to it, so be sure to close the
 * stream when you are done with it.
 *
 * @author Peter Eastman
 */

public class WidgetDecoder
{
  private HashMap objectTable;
  private Object rootObject;

  private static ThreadLocal threadLocalObjects = new ThreadLocal();

  /**
   * Create a WidgetDecoder to reconstruct a Widget hierarchy from its XML representation.
   *
   * @param in        the InputStream from which to read the XML
   */
  
  public WidgetDecoder(InputStream in)
  {
    this(in, null, null);
  }

  /**
   * Create a WidgetDecoder to reconstruct a Widget hierarchy from its XML representation.
   *
   * @param in         the InputStream from which to read the XML
   * @param resources  a ResourceBundle from which to obtain localized Strings.  See
   *                   {@link WidgetLocalization} for details.
   */

  public WidgetDecoder(InputStream in, ResourceBundle resources)
  {
    this(in, null, resources);
  }

  /**
   * Create a WidgetDecoder to reconstruct a Widget hierarchy from its XML representation.  If
   * recoverable errors occur during reconstruction, the listener will be notified of them.
   *
   * @param in         the InputStream from which to read the XML
   * @param listener   the listener to notify of recoverable errors
   */
  
  public WidgetDecoder(InputStream in, ExceptionListener listener)
  {
    this(in, listener, null);
  }

  /**
   * Create a WidgetDecoder to reconstruct a Widget hierarchy from its XML representation.  If
   * recoverable errors occur during reconstruction, the listener will be notified of them.
   *
   * @param in         the InputStream from which to read the XML
   * @param listener   the listener to notify of recoverable errors
   * @param resources  a ResourceBundle from which to obtain localized Strings.  See
   *                   {@link WidgetLocalization} for details.
   */

  public WidgetDecoder(InputStream in, ExceptionListener listener, ResourceBundle resources)
  {
    threadLocalObjects.set(new HashMap());
    WidgetLocalization.setResourceBundle(resources);
    XMLDecoder decoder = new XMLDecoder(in, null, listener);
    rootObject = decoder.readObject();
    objectTable = (HashMap) threadLocalObjects.get();
    if (rootObject instanceof WidgetContainer)
      ((WidgetContainer) rootObject).layoutChildren();
    WidgetLocalization.setResourceBundle(null);
    threadLocalObjects.set(null);
  }

  /**
   * Get the root object that was stored in the XML file.
   */
  
  public Object getRootObject()
  {
    return rootObject;
  }
  
  /**
   * Get an object that was stored in the XML file.
   *
   * @param name     the name of the object to get
   * @return the specified object, or null if there was no object in the file with that name
   */
  
  public Object getObject(String name)
  {
    return objectTable.get(name);
  }
  
  /**
   * This method is used during decoding.  It records a named object into the table of objects
   * in the current file.  If no file is currently being read, this does nothing.  There usually
   * is no reason for you to call this directly.
   */
  
  public static void registerObject(String name, Object obj)
  {
    HashMap map = (HashMap) threadLocalObjects.get();
    if (map != null)
      map.put(name, obj);
  }
}

