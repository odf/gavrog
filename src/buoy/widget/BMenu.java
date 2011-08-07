package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;
import java.util.*;
import javax.swing.*;

/**
 * A BMenu is a WidgetContainer corresponding to a pulldown menu in the menu bar of a window.
 *
 * @author Peter Eastman
 */

public class BMenu extends WidgetContainer implements MenuWidget
{
  private ArrayList<MenuWidget> elements;
  
  static
  {
    WidgetEncoder.setPersistenceDelegate(BMenu.class, new IndexedContainerDelegate(new String [] {"getChild"}));
  }

  /**
   * Create a new BMenu with no title.
   */
  
  public BMenu()
  {
    this(null);
  }

  /**
   * Create a new BMenu.
   *
   * @param title     the title of the menu
   */
  
  public BMenu(String title)
  {
    component = createComponent();
    elements = new ArrayList<MenuWidget>();
    getComponent().setText(title);
  }
  
  /**
   * Create the JMenu which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   */
  
  protected JMenu createComponent()
  {
    return new JMenu();
  }


  public JMenu getComponent()
  {
    return (JMenu) component;
  }

  /**
   * Get the title of this menu which appears in the menu bar.
   */
  
  public String getText()
  {
    return getComponent().getText();
  }
  
  /**
   * Set the title of this menu which appears in the menu bar.
   */
  
  public void setText(String title)
  {
    getComponent().setText(title);
  }
  
  /**
   * Get the mnemonic which can be used to activate this menu in keyboard navigation mode.
   *
   * @return the key code (defined by the KeyEvent class) which activates this menu
   */
  
  public int getMnemonic()
  {
    return getComponent().getMnemonic();
  }
  
  /**
   * Set the mnemonic which can be used to activate this menu in keyboard navigation mode.
   *
   * @param key   the key code (defined by the KeyEvent class) which activates this menu
   */
  
  public void setMnemonic(int key)
  {
    getComponent().setMnemonic(key);
  }

  /**
   * Add a MenuWidget (typically a BMenuItem or another BMenu) to the end of the menu.
   *
   * @param widget    the MenuWidget to add
   */

  public void add(MenuWidget widget)
  {
    add(widget, ((Widget) widget).getParent() == this ? getChildCount()-1 : getChildCount());
  }

  /**
   * Add a MenuWidget (typically a BMenuItem or another BMenu) to the menu.
   *
   * @param widget    the MenuWidget to add
   * @param index     the position at which to add it
   */

  public void add(MenuWidget widget, int index)
  {
    WidgetContainer parent = ((Widget) widget).getParent();
    if (parent != null)
      parent.remove((Widget) widget);
    elements.add(index, widget);
    getComponent().add(((Widget) widget).getComponent(), index);
    setAsParent((Widget) widget);
  }

  /**
   * Add a dividing line (a BSeparator) to the end of the menu.
   */
  
  public void addSeparator()
  {
    add(new BSeparator());
  }
  
  /**
   * Get the number of children in this container.
   */
  
  public int getChildCount()
  {
    return elements.size();
  }
  
  /**
   * Get the i'th child of this container.
   */
  
  public MenuWidget getChild(int i)
  {
    return elements.get(i);
  }
  
  /**
   * Get a Collection containing all child Widgets of this container.
   */
  
  public Collection<Widget> getChildren()
  {
    ArrayList<Widget> children = new ArrayList<Widget>(elements.size());
    for (MenuWidget widget : elements)
      children.add((Widget) widget);
    return children;
  }
  
  /**
   * Remove a child Widget from this container.
   */
  
  public void remove(Widget widget)
  {
    elements.remove((MenuWidget) widget);
    getComponent().remove(widget.getComponent());
    removeAsParent(widget);
  }
  
  /**
   * Remove all child Widgets from this container.
   */
  
  public void removeAll()
  {
    for (int i = 0; i < elements.size(); i++)
      removeAsParent((Widget) elements.get(i));
    getComponent().removeAll();
    elements.clear();
  }
  
  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */
  
  public void layoutChildren()
  {
  }
}
