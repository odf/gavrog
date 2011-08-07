package buoy.xml;

import java.util.*;
import java.lang.ref.*;

/**
 * This class cooperates with {@link WidgetEncoder} and {@link WidgetDecoder} to localized the
 * text stored in XML files.  Rather than containing the actual text which will appear in the
 * user interface, the XML file contains keys which are looked up from a ResourceBundle at
 * decoding time.  This class maintains a list of String objects which are to be localized at
 * encoding time, and performs the actual substitution at decoding time.
 * <p>
 * To use this class, follow these steps:
 * <ol>
 * <li>Create a ResourceBundle containing the strings to be localized.  For example,
 * <p>
 * <tt>
 * menu.file=File<br>
 * menu.edit=Edit
 * </tt><p></li>
 * <li>When creating the user interface, set any Strings which are to be localized to the keys defined
 * in the ResourceBundle:
 * <p>
 * <tt>
 * menu.setText("menu.file");
 * </tt><p></li>
 * <li>Add these Strings to the list of ones which should be localized:
 * <p>
 * <tt>
 * WidgetLocalization.addLocalizedString("menu.file");
 * </tt><p></li>
 * <li>Use WidgetEncoder to save the user interface to an XML file exactly as you
 * normally would.</li>
 * <li>When loading the XML file, pass the ResourceBundle as an argument to WidgetDecoder's
 * constructor.  All the Strings which were marked as needing to be localized will automatically
 * be replaced with values from the ResourceBundle.</li>
 * </ol>
 * The Strings to be localized are identified by object identity rather than value equality.
 * This means that when you call {@link #addLocalizedString addLocalizedString()}, you must pass
 * in the exact String object which is used in the user interface.  It also means that it is
 * possible for the same String value to appear twice in the user interface, and be localized in
 * one place but not in the other.
 * <p>
 * This class can also operate in another mode from that described above.  Suppose a graphical GUI
 * editor application is used to create a user interface.  That application defines the Strings to
 * be localized, then uses WidgetEncoder to save it as XML.  When that file is processed by
 * WidgetDecoder to generate the user interface for an application, the localized Strings are
 * obtained from a ResourceBundle.
 * <p>
 * Suppose, however, that you want to reload the XML file into the GUI editor application for
 * further editing.  In that case, load the XML file with WidgetDecoder, but use one of the
 * constructors which does <i>not</i> take a ResourceBundle.  This will cause the user interface
 * to be loaded exactly as it originally was before encoding.  The localization keys will be loaded
 * directly, not replaced with localized versions.  Furthermore, as they are loaded, they are
 * automatically added to the list of Strings to localize so that when the file is saved again,
 * all of the Strings will be properly localized.
 *
 * @author Peter Eastman
 */

public class WidgetLocalization
{
  private static HashSet localizedStringSet = new HashSet();
  private static ResourceBundle currentBundle;

  /**
   * Add a String object to the list of Strings which should be localized when the user interface
   * is reconstructed from XML.
   */

  public static void addLocalizedString(String s)
  {
    localizedStringSet.add(new WeakIdentityReference(s));
  }

  /**
   * Remove a String object from the list of Strings which should be localized when the user interface
   * is reconstructed from XML.
   */

  public static void removeLocalizedString(String s)
  {
    localizedStringSet.remove(new WeakIdentityReference(s));
  }

  /**
   * Determine whether a String object is currently in the list of Strings which should be localized
   * when the user interface is reconstructed from XML.
   */

  public static boolean isLocalizedString(String s)
  {
    return localizedStringSet.contains(new WeakIdentityReference(s));
  }

  /**
   * Set the ResourceBundle from which to obtain localized Strings.  This is called by WidgetDecoder.
   * It should not be invoked by any other class.
   */

  static void setResourceBundle(ResourceBundle bundle)
  {
    currentBundle = bundle;
  }

  /**
   * Get the full list of String objects which should be localized when the user interface
   * is reconstructed from XML.
   */

  public static String [] getAllLocalizedStrings()
  {
    Iterator entries = localizedStringSet.iterator();
    ArrayList strings = new ArrayList();
    while (entries.hasNext())
    {
      Object obj = ((Reference) entries.next()).get();
      if (obj != null)
        strings.add(obj);
      else
        entries.remove();
    }
    return (String []) strings.toArray(new String [strings.size()]);
  }

  /**
   * This method is invoked during decoding to get the localized String corresponding to a key.
   * It is intended for use by XMLDecoder, and you should not invoke it directly.
   */

  public static Object getLocalizedString(String key)
  {
    if (currentBundle == null)
    {
      addLocalizedString(key);
      return key;
    }
    try
    {
      return currentBundle.getString(key);
    }
    catch (MissingResourceException ex)
    {
      return key;
    }
  }

  /**
   * This class is used to store references to localized strings in the map.  It forces the
   * map to recognize objects by identity rather than equality, and also uses WeakReferences
   * so Strings can be removed from the map when they are no longer being used.
   */

  private static class WeakIdentityReference extends WeakReference
  {
    private int hash;

    public WeakIdentityReference(Object obj)
    {
      super(obj);
      hash = obj.hashCode();
    }

    public int hashCode()
    {
      return hash;
    }

    public boolean equals(Object obj)
    {
      obj = ((Reference) obj).get();
      Object thisObj = get();
      if (thisObj == null || obj == null)
        return false;
      return (thisObj == obj);
    }
  }
}
