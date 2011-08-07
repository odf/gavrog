package buoy.xml.delegate;

import buoy.widget.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * This class is a PersistenceDelegate for serializing Shortcuts.
 *
 * @author Peter Eastman
 */

public class ShortcutDelegate extends DefaultPersistenceDelegate
{
  /**
   * Create a ShortcutDelegate.
   */
  
  public ShortcutDelegate()
  {
  }
  
  protected Expression instantiate(Object oldInstance, Encoder out)
  {
    Shortcut old = (Shortcut) oldInstance;
    Object arg1;
    if (old.getKeyCode() == KeyEvent.VK_UNDEFINED)
      arg1 = new Character(old.getKeyChar());
    else
      arg1 = new Integer(old.getKeyCode());
    return new Expression(old, old.getClass(), "new", new Object [] {arg1, new Integer(old.getModifiers())});
  }
}
