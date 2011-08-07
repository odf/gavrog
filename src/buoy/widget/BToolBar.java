package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;
import buoy.internal.*;

import javax.swing.*;
import java.util.*;

/**
 * A BToolBar is a WidgetContainer which displays a series of Widgets in a row or column.  It is
 * similar to a {@link RowContainer} or {@link ColumnContainer}, but has a different appearance.
 * Most often, the child Widgets are {@link BButton BButtons} with icons, and they act as shortcuts for performing
 * common operations.
 *
 * @author Peter Eastman
 */

public class BToolBar extends WidgetContainer
{
  private ArrayList<Widget> child;

  public static final Orientation HORIZONTAL = new Orientation(SwingConstants.HORIZONTAL);
  public static final Orientation VERTICAL = new Orientation(SwingConstants.VERTICAL);

  static
  {
    WidgetEncoder.setPersistenceDelegate(BToolBar.class, new IndexedContainerDelegate(new String [] {"getChild"}));
    WidgetEncoder.setPersistenceDelegate(Orientation.class, new StaticFieldDelegate(BToolBar.class));
  }

  /**
   * Create a new BToolBar whose orientation is set to HORIZONTAL.
   */

  public BToolBar()
  {
    this(HORIZONTAL);
  }

  /**
   * Create a new BToolBar.
   *
   * @param orientation     the orientation of the toolbar
   */

  public BToolBar(Orientation orientation)
  {
    component = createComponent();
    child = new ArrayList<Widget>();
    setOrientation(orientation);
  }

  /**
   * Create the JToolbar which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   */

  protected JToolBar createComponent()
  {
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    return toolbar;
  }

  public JToolBar getComponent()
  {
    return (JToolBar) component;
  }

  /**
   * Get the orientation of this toolbar.
   */

  public Orientation getOrientation()
  {
    int orientation = ((JToolBar) getComponent()).getOrientation();
    return (orientation == SwingConstants.HORIZONTAL ? HORIZONTAL : VERTICAL);
  }

  /**
   * Set the orientation of this toolbar.
   */

  public void setOrientation(Orientation orientation)
  {
    ((JToolBar) getComponent()).setOrientation(orientation.value);
  }

  /**
   * Add a Widget (usually a BButton) to the end of the toolbar.
   *
   * @param widget    the Widget to add
   */

  public void add(Widget widget)
  {
    add(widget, getChildCount());
  }

  /**
   * Add a Widget (usually a BButton) to the toolbar.
   *
   * @param widget    the Widget to add
   * @param index     the position at which to add it
   */

  public void add(Widget widget, int index)
  {
    if (widget.getParent() != null)
      widget.getParent().remove(widget);
    child.add(index, widget);
    getComponent().add(new SingleWidgetPanel((Widget) widget), index);
    setAsParent((Widget) widget);
  }

  /**
   * Add a dividing line (a BSeparator) to the end of the toolbar.
   */

  public void addSeparator()
  {
    add(new BSeparator(getOrientation() == HORIZONTAL ? BSeparator.VERTICAL : BSeparator.HORIZONTAL));
  }

  /**
   * Get the number of children in this container.
   */

  public int getChildCount()
  {
    return child.size();
  }

  /**
   * Get the i'th child of this container.
   */

  public Widget getChild(int i)
  {
    return child.get(i);
  }

  /**
   * Get a Collection containing all child Widgets of this container.
   */

  public Collection<Widget> getChildren()
  {
    return new ArrayList<Widget>(child);
  }

  /**
   * Remove a child Widget from this container.
   */

  public void remove(Widget widget)
  {
    child.remove(widget);
    getComponent().remove(widget.getComponent().getParent());
    removeAsParent(widget);
  }

  /**
   * Remove all child Widgets from this container.
   */

  public void removeAll()
  {
    for (int i = 0; i < child.size(); i++)
      removeAsParent((Widget) child.get(i));
    getComponent().removeAll();
    child.clear();
  }

  /**
   * Get the index of a particular Widget.
   *
   * @param widget      the Widget to locate
   * @return the position of the Widget within this container, or -1 if the Widget is not a child
   * of this container
   */

  public int getChildIndex(Widget widget)
  {
    return child.indexOf(widget);
  }

  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */

  public void layoutChildren()
  {
    getComponent().validate();
    for (int i = 0; i < child.size(); i++)
    {
      Widget w = child.get(i);
      if (w instanceof WidgetContainer)
        ((WidgetContainer) w).layoutChildren();
    }
  }

  /**
   * This inner class represents an orientation for the toolbar.
   */

  public static class Orientation
  {
    private int value;

    private Orientation(int value)
    {
      this.value = value;
    }
  }
}
