package buoy.widget;

import java.util.*;

/**
 * A WidgetContainer is a Widget which contains other Widgets.  It is responsible for arranging them
 * on the screen.  This is an abstract class.  There are subclasses which provide different methods of
 * laying out Widgets.
 *
 * @author Peter Eastman
 */

public abstract class WidgetContainer extends Widget
{
  protected boolean opaque;
  
  /**
   * Create a WidgetContainer.
   */
  
  public WidgetContainer()
  {
    opaque = true;
  }
  
  /**
   * Get the number of children in this container.
   */
  
  public abstract int getChildCount();
  
  /**
   * Get a Collection containing all child Widgets of this container.
   */
  
  public abstract Collection<Widget> getChildren();
  
  /**
   * Remove a child Widget from this container.
   */
  
  public abstract void remove(Widget widget);
  
  /**
   * Remove all child Widgets from this container.
   */
  
  public abstract void removeAll();
  
  /**
   * Set this container as the parent of another Widget.  Subclasses should call this whenever
   * a child Widget is added.
   */
  
  protected void setAsParent(Widget widget)
  {
    widget.setParent(this);
  }
  
  /**
   * Set the parent of another Widget to null.  Subclasses should call this whenever a child Widget
   * is removed.
   */
  
  protected void removeAsParent(Widget widget)
  {
    widget.setParent(null);
  }
  
  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */
  
  public abstract void layoutChildren();
  
  /**
   * Determine whether this WidgetContainer is set to be opaque.  If true, then it will be filled with its
   * background color before RepaintEvents are sent out for it or any of its children.  If false, this Widget's
   * parent container will show through.
   */
  
  public boolean isOpaque()
  {
    return opaque;
  }

  /**
   * Set whether this WidgetContainer should be opaque.  If true, then it will be filled with its background
   * color before RepaintEvents are sent out for it or any of its children.  If false, this Widget's parent
   * container will show through.
   */
  
  public void setOpaque(boolean opaque)
  {
    this.opaque = opaque;
  }
}