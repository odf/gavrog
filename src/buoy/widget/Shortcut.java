package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;
import java.awt.*;
import javax.swing.*;

/**
 * A Shortcut represents a keyboard shortcut that can be used for activating a menu item.  It consists of a
 * particular key that must be pressed, plus a set of modifier keys.
 * <p>
 * Every platform has a "standard" modifier key which is normally used for activating menu items.  That modifier
 * is represented by the DEFAULT_MASK constant.  In most cases, you can simply use a constructor which does not
 * specify modifier keys, in which case the platform-specific default modifier will be used.  If you want to
 * add other modifier keys, you should generally combine them with the default mask.  For example:
 * <p>
 * new Shortcut('X', Shortcut.SHIFT_MASK | Shortcut.DEFAULT_MASK)
 * <p>
 * will correspond to control-shift-X on Windows, and meta-shift-X on Macintosh.
 */

public class Shortcut
{
  private KeyStroke stroke;
  private int modifiers;

  public static final int SHIFT_MASK = Event.SHIFT_MASK;
  public static final int CTRL_MASK = Event.CTRL_MASK;
  public static final int META_MASK = Event.META_MASK;
  public static final int ALT_MASK = Event.ALT_MASK;
  public static final int DEFAULT_MASK = 65536;
  
  static
  {
    WidgetEncoder.setPersistenceDelegate(Shortcut.class, new ShortcutDelegate());
  }

  /**
   * Create a Shortcut representing a particular character, combined with the platform-specific default
   * modifier key.
   *
   * @param c     the character which must be typed to activate the shortcut
   */
  
  public Shortcut(char c)
  {
    this(c, DEFAULT_MASK);
  }
  
  /**
   * Create a Shortcut representing a particular key, combined with the platform-specific default
   * modifier key.
   *
   * @param key    the key code (defined by the KeyEvent class) which must be typed to activate the shortcut
   */
  
  public Shortcut(int key)
  {
    this(key, DEFAULT_MASK);
  }

  /**
   * Create a Shortcut representing a particular character, combined with a set of modifier keys.
   *
   * @param c           the character which must be typed to activate the shortcut
   * @param modifiers   the set of modifier keys required to activate the shortcut.  This should be an ORed
   *                    combination of the mask constants defined in this class
   */
  
  public Shortcut(char c, int modifiers)
  {
    this.modifiers = modifiers;
    stroke = KeyStroke.getKeyStroke(c, getKeyStrokeModifiers());
  }
  
  /**
   * Create a Shortcut representing a particular key, combined with a set of modifier keys.
   *
   * @param key         the key code (defined by the KeyEvent class) which must be typed to activate the shortcut
   * @param modifiers   the set of modifier keys required to activate the shortcut.  This should be an ORed
   *                    combination of the mask constants defined in this class
   */
  
  public Shortcut(int key, int modifiers)
  {
    this.modifiers = modifiers;
    stroke = KeyStroke.getKeyStroke(key, getKeyStrokeModifiers());
  }
  
  /**
   * Given the modifier flags for this Shortcut, calculate the modifier flags for its KeyStroke.
   */
  
  private int getKeyStrokeModifiers()
  {
    if ((modifiers&DEFAULT_MASK) == 0)
      return modifiers;
    return ((modifiers-DEFAULT_MASK)|Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
  }
  
  /**
   * Get a KeyStroke corresponding to this Shortcut.
   */
  
  public KeyStroke getKeyStroke()
  {
    return stroke;
  }
  
  /**
   * Get the character which must be typed to activate this shortcut.  If this Shortcut is specified by a
   * key code rather than a character, this returns KeyEvent.CHAR_UNDEFINED.
   */
  
  public char getKeyChar()
  {
    return stroke.getKeyChar();
  }
  
  /**
   * Get the key code (defined by the KeyEvent class) which must be typed to activate this shortcut.
   * If this Shortcut is specified by a character rather than a key code, this returns KeyEvent.VK_UNDEFINED.
   */
  
  public int getKeyCode()
  {
    return stroke.getKeyCode();
  }
  
  /**
   * Get the set of modifier keys (a sum of the mask constants defined by this class) which must be
   * held down to activate this shortcut.
   */
  
  public int getModifiers()
  {
    return modifiers;
  }
}