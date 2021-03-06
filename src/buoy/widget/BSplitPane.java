package buoy.widget;

import buoy.event.*;
import buoy.internal.*;
import buoy.xml.*;
import buoy.xml.delegate.*;
import java.awt.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;

/**
 * BSplitPane is a WidgetContainer whose space is divided between two child Widgets.  A drag
 * bar is placed between them, which the user can move to change how much space is given to each child.
 * <p>
 * In addition to the event types generated by all Widgets, BSplitPanes generate the following event types:
 * <ul>
 * <li>{@link buoy.event.ValueChangedEvent ValueChangedEvent}</li>
 * </ul>
 *
 * @author Peter Eastman
 */

public class BSplitPane extends WidgetContainer
{
  private Widget child[];
  private int suppressEvents;
  
  public static final Orientation HORIZONTAL = new Orientation(JSplitPane.HORIZONTAL_SPLIT);
  public static final Orientation VERTICAL = new Orientation(JSplitPane.VERTICAL_SPLIT);

  static
  {
    WidgetEncoder.setPersistenceDelegate(BSplitPane.class, new BSplitPaneDelegate());
    WidgetEncoder.setPersistenceDelegate(Orientation.class, new StaticFieldDelegate(BSplitPane.class));
  }
  
  /**
   * Create a new BSplitPane which is split horizontally to place its children side by side.
   */
  
  public BSplitPane()
  {
    this(HORIZONTAL, null, null);
  }
  
  /**
   * Create a new BSplitPane.
   *
   * @param orient     the split orientation (HORIZONTAL or VERTICAL)
   */
  
  public BSplitPane(Orientation orient)
  {
    this(orient, null, null);
  }

  /**
   * Create a new BSplitPane.
   *
   * @param orient     the split orientation (HORIZONTAL or VERTICAL)
   * @param child1     the first (top or left) child Widget
   * @param child2     the second (bottom or right) child Widget
   */
  
  public BSplitPane(Orientation orient, Widget child1, Widget child2)
  {
    component = createComponent();
    getComponent().addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent ev)
      {
        layoutChildren();
        if (suppressEvents == 0)
          SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
              dispatchEvent(new ValueChangedEvent(BSplitPane.this));
            }
          });
      }
    });
    child = new Widget [2];
    if (child1 != null)
      add(child1, 0);
    if (child2 != null)
      add(child2, 1);
    setOrientation(orient);
  }

  /**
   * Create the JSplitPane which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   */
  
  protected JSplitPane createComponent()
  {
    return new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
  }

  public JSplitPane getComponent()
  {
    return (JSplitPane) component;
  }

  /**
   * Get the location of the divider (in pixels).
   */
  
  public int getDividerLocation()
  {
    return getComponent().getDividerLocation();
  }
  
  /**
   * Set the location of the divider (in pixels).
   */
  
  public void setDividerLocation(int location)
  {
    try
    {
      suppressEvents++;
      getComponent().setDividerLocation(location);
    }
    finally
    {
      suppressEvents--;
    }
  }
  
  /**
   * Set the location of the divider as a fraction of the total size of the container.  0.0 is
   * all the way to the top/left, and 1.0 is all the way to the bottom/right.
   */
  
  public void setDividerLocation(double location)
  {
    try
    {
      suppressEvents++;
      getComponent().setDividerLocation(location);
    }
    finally
    {
      suppressEvents--;
    }
  }
  
  /**
   * Reposition the divider based on the minimum and preferred sizes of the child widgets,
   * and the current resize weight.
   */
  
  public void resetToPreferredSizes()
  {
    try
    {
      suppressEvents++;
      getComponent().resetToPreferredSizes();
    }
    finally
    {
      suppressEvents--;
    }
  }
  
  /**
   * Get which way the container is split, HORIZONTAL or VERTICAL.
   */
  
  public Orientation getOrientation()
  {
    return (getComponent().getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? HORIZONTAL : VERTICAL);
  }
  
  /**
   * Set which way the container is split, HORIZONTAL or VERTICAL.
   */
  
  public void setOrientation(Orientation orient)
  {
    getComponent().setOrientation(orient.value);
  }
  
  /**
   * Get whether the container should continuously resize its children as the divider bar is dragged,
   * or only when the mouse is released.
   */
  
  public boolean isContinuousLayout()
  {
    return getComponent().isContinuousLayout();
  }
  
  /**
   * Set whether the container should continuously resize its children as the divider bar is dragged,
   * or only when the mouse is released.
   */
  
  public void setContinuousLayout(boolean continuous)
  {
    getComponent().setContinuousLayout(continuous);
  }
  
  /**
   * Get whether the divider provides a control to collapse or expand the split with a single click.
   */
  
  public boolean isOneTouchExpandable()
  {
    return getComponent().isOneTouchExpandable();
  }
  
  /**
   * Set whether the divider provides a control to collapse or expand the split with a single click.
   */
  
  public void setOneTouchExpandable(boolean expandable)
  {
    getComponent().setOneTouchExpandable(expandable);
  }
  
  /**
   * Get how extra space is divided between the two child widgets.  A weight of 0 gives all extra
   * space to the second child, while a weight of 1 gives all extra space to the first child.
   * Values between 0 and 1 divide the extra space proportionally between the two.
   */
  
  public double getResizeWeight()
  {
    return getComponent().getResizeWeight();
  }
  
  /**
   * Set how extra space is divided between the two child widgets.  A weight of 0 gives all extra
   * space to the second child, while a weight of 1 gives all extra space to the first child.
   * Values between 0 and 1 divide the extra space proportionally between the two.
   */
  
  public void setResizeWeight(double weight)
  {
    getComponent().setResizeWeight(weight);
  }
  
  /**
   * Get the number of children in this container.
   */
  
  public int getChildCount()
  {
    int count = 0;
    for (int i = 0; i < child.length; i++)
      if (child[i] != null)
        count++;
    return count;
  }

  /**
   * Get a Collection containing all child Widgets of this container.
   */
  
  public Collection<Widget> getChildren()
  {
    ArrayList<Widget> ls = new ArrayList<Widget>(2);
    for (int i = 0; i < child.length; i++)
      if (child[i] != null)
        ls.add(child[i]);
    return ls;
  }
  
  /**
   * Get one of the child Widgets.
   *
   * @param index    the index of the Widget to get (0 or 1)
   */
  
  public Widget getChild(int index)
  {
    return child[index];
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
    for (int i = 0; i < child.length; i++)
      if (child[i] instanceof WidgetContainer)
        ((WidgetContainer) child[i]).layoutChildren();
  }

  /**
   * Add a Widget to this container.  If there is already a Widget in the specified position,
   * it is removed before the new one is added.
   *
   * @param widget     the Widget to add
   * @param index      the position at which to add it (0 or 1)
   */
  
  public void add(Widget widget, int index)
  {
    if (child[index] != null)
      remove(index);
    if (widget.getParent() != null)
      widget.getParent().remove(widget);
    child[index] = widget;
    if (index == 0)
      getComponent().setLeftComponent(new SingleWidgetPanel(widget));
    else
      getComponent().setRightComponent(new SingleWidgetPanel(widget));
    setAsParent(widget);
    invalidateSize();
  }
    
  /**
   * Remove a child Widget from this container.
   *
   * @param widget     the Widget to remove
   */
  
  public void remove(Widget widget)
  {
    for (int i = 0; i < child.length; i++)
      if (child[i] == widget)
      {
        remove(i);
        return;
      }
  }
  
  /**
   * Remove a child Widget from this container.
   *
   * @param index      the index of the Widget to remove (0 or 1)
   */
  
  public void remove(int index)
  {
    if (child[index] == null)
      return;
    getComponent().remove(child[index].getComponent().getParent());
    removeAsParent(child[index]);
    child[index] = null;
    invalidateSize();
  }
  
  /**
   * Remove all child Widgets from this container.
   */
  
  public void removeAll()
  {
    getComponent().removeAll();
  }
  
  /**
   * Get the smallest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget smaller than its minimum size.
   */
  
  public Dimension getMinimumSize()
  {
    Dimension dim0 = (child[0] == null ? new Dimension() : child[0].getMinimumSize());
    Dimension dim1 = (child[1] == null ? new Dimension() : child[1].getMinimumSize());
    int dividerWidth = getComponent().getDividerSize();
    if (getOrientation() == HORIZONTAL)
      return new Dimension(dim0.width+dim1.width+dividerWidth, Math.max(dim0.height, dim1.height));
    else
      return new Dimension(Math.max(dim0.width, dim1.width), dim0.height+dim1.height+dividerWidth);
  }

  /**
   * Get the preferred size at which this Widget will look best.  When a WidgetContainer lays out
   * its contents, it will attempt to make this Widget as close as possible to its preferred size.
   */
  
  public Dimension getPreferredSize()
  {
    Dimension dim0 = (child[0] == null ? new Dimension() : child[0].getPreferredSize());
    Dimension dim1 = (child[1] == null ? new Dimension() : child[1].getPreferredSize());
    int dividerWidth = getComponent().getDividerSize();
    if (getOrientation() == HORIZONTAL)
      return new Dimension(dim0.width+dim1.width+dividerWidth, Math.max(dim0.height, dim1.height));
    else
      return new Dimension(Math.max(dim0.width, dim1.width), dim0.height+dim1.height+dividerWidth);
  }
  
  /**
   * This inner class represents an orientation (horizontal or vertical) for the split.
   */
  
  public static class Orientation
  {
    protected int value;
    
    private Orientation(int value)
    {
      this.value = value;
    }
  }
}
