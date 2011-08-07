package buoy.widget;

import buoy.event.*;
import buoy.xml.*;
import buoy.xml.delegate.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;

/**
 * A BPopupMenu is a WidgetContainer corresponding to a popup menu.  It is typically displayed in
 * response to a {@link buoy.event.WidgetMouseEvent WidgetMouseEvent} whose <code>isPopupTrigger()</code>
 * method returns true.  The exact conditions which represent a popup trigger are platform specific.
 * <p>
 * The easiest way to add a popup menu to a widget is to invoke
 * <p>
 * <code>widget.addEventLink(WidgetMouseEvent.class, popup, "show");</code>
 * <p>
 * on the widget.  This will automatically take care of showing the menu whenever the user performs
 * the appropriate action.  Alternatively, you can write your own code to listen for the event and
 * call <code>show()</code> on the popup menu.  For example, you might want to disable certain
 * menu items before showing it based on the position of the mouse click.
 *
 * @author Peter Eastman
 */

public class BPopupMenu extends WidgetContainer implements MenuWidget
{
  private ArrayList<MenuWidget> elements;
  
  static
  {
    WidgetEncoder.setPersistenceDelegate(BPopupMenu.class, new IndexedContainerDelegate(new String [] {"getChild"}));
  }

  /**
   * Create a new BPopupMenu.
   */
  
  public BPopupMenu()
  {
    component = createComponent();
    elements = new ArrayList<MenuWidget>();
  }
  
  /**
   * Create the JPopupMenu which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   */
  
  protected JPopupMenu createComponent()
  {
    return new JPopupMenu();
  }

  public JPopupMenu getComponent()
  {
    return (JPopupMenu) component;
  }

  /**
   * Display the popup menu over another Widget.
   *
   * @param widget   the Widget over which to display the popup menu
   * @param x        the x coordinate at which to display the popup menu
   * @param y        the y coordinate at which to display the popup menu
   */
  
  public void show(Widget widget, int x, int y)
  {
    getComponent().show(widget.getComponent(), x, y);
  }
  
  /**
   * Display the popup menu in response to an event.  If <code>event.isPopupTrigger()</code>
   * returns false, this method returns without doing anything.  Otherwise it shows the popup,
   * determining the appropriate location based on information stored in the event.
   * <p>
   * This version of show() is provided as a convenience.  It allows you to very easily add
   * a popup menu to a Widget by invoking
   * <p>
   * <code>widget.addEventLink(WidgetMouseEvent.class, popup, "show");</code>
   *
   * @param event    the user event which is triggering the popup menu 
   */
  
  public void show(WidgetMouseEvent event)
  {
    if (!event.isPopupTrigger())
      return;
    show(event.getWidget(), event.getX(), event.getY());
  }
  
  /**
   * Add a MenuWidget (typically a BMenuItem or BMenu) to the end of the menu.
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
    elements.remove(widget);
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
