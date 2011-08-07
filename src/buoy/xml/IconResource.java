package buoy.xml;

import javax.swing.*;
import java.net.*;
import java.beans.*;

/**
 * This is a subclass of ImageIcon which loads the image from the classpath by calling
 * <code>ClassLoader.getResource()</code>.  The main value of this class is that, unlike standard
 * ImageIcons, it can be successfully serialized as XML by {@link WidgetEncoder WidgetEncoder}.
 * Because the image is specified by a relative path, it will be found when the XML is decoded,
 * regardless of where the application is stored on the computer.
 *
 * @author Peter Eastman
 */

public class IconResource extends ImageIcon
{
  private String resourceName;

  private static URL NULL_URL;

  static
  {
    WidgetEncoder.setPersistenceDelegate(IconResource.class, new DefaultPersistenceDelegate(new String [] {"resourceName"}));

    // Create a dummy URL that we can use to avoid ever passing null to the superclass constructor.

    try
    {
      NULL_URL = new URL("file://");
    }
    catch (MalformedURLException e)
    {
    }
  }

  /**
   * Create an IconResource by loading an image file out of the classpath.
   *
   * @param resourceName     the name of the resource to load (by passing it to <code>ClassLoader.getResource()</code>)
   */

  public IconResource(String resourceName)
  {
    this(resourceName, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Create an IconResource by loading an image file out of the classpath.
   *
   * @param resourceName     the name of the resource to load (by passing it to <code>Class.getResource()</code>)
   * @param description      a brief description of the image
   */

  public IconResource(String resourceName, String description)
  {
    this(resourceName);
    setDescription(description);
  }

  /**
   * Create an IconResource by loading an image file out of the classpath.  This constructor
   * allows you to specify what ClassLoader should be used to load the resource.  This is
   * primarily useful in UI builder applications, where the image should be loaded from the
   * classpath of the application being edited, not the classpath of the UI builder application
   * itself.
   *
   * @param resourceName     the name of the resource to load (by passing it to <code>ClassLoader.getResource()</code>)
   * @param classloader      the ClassLoader with which to load the resource
   */

  public IconResource(String resourceName, ClassLoader classloader)
  {
    this(classloader.getResource(resourceName));
    this.resourceName = resourceName;
  }

  /**
   * This is the internal constructor which all of the others call.  It ensures that we never
   * pass a null URL to the superclass constructor (which would produce an exception).
   */

  private IconResource(URL url)
  {
    super(url == null ? NULL_URL : url, null);
  }

  /**
   * Get the icon's resource name.
   */

  public String getResourceName()
  {
    return resourceName;
  }
}
