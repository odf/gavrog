package buoy.widget;

import buoy.internal.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * A BOutline is a WidgetContainer that draws an outline around another Widget.  The appearance
 * of the outline is determined by a javax.swing.border.Border object.  There are static methods
 * for creating several common types of outlines, or you can use a different type by providing
 * your own Border object.
 *
 * @author Peter Eastman
 */

public class BOutline extends WidgetContainer
{
  private Widget content;
  
  /**
   * Create a new BOutline with no content Widget and no border.
   */
  
  public BOutline()
  {
    this(null, null);
  }

  /**
   * Create a new BOutline.
   *
   * @param content    the Widget to use as the content of the BOutline
   * @param border     the outline to draw around the content Widget
   */
  
  public BOutline(Widget content, Border border)
  {
    component = new WidgetContainerPanel(this);
    setContent(content);
    setBorder(border);
  }
  
  /**
   * Create a BOutline with an empty border.
   *
   * @param content    the Widget to use as the content of the BOutline
   * @param thickness  the thickness of the border
   */
  
  public static BOutline createEmptyBorder(Widget content, int thickness)
  {
    return new BOutline(content, BorderFactory.createEmptyBorder(thickness, thickness, thickness, thickness));
  }
  
  /**
   * Create a BOutline with an etched border.
   *
   * @param content    the Widget to use as the content of the BOutline
   * @param raised     if true, the border will have a raised appearance.  If false, it will
   *                   have a lowered appearance.
   */
  
  public static BOutline createEtchedBorder(Widget content, boolean raised)
  {
    return new BOutline(content, BorderFactory.createEtchedBorder(raised ? EtchedBorder.RAISED : EtchedBorder.LOWERED));
  }
  
  /**
   * Create a BOutline with a beveled border.
   *
   * @param content    the Widget to use as the content of the BOutline
   * @param raised     if true, the border will have a raised appearance.  If false, it will
   *                   have a lowered appearance.
   */
  
  public static BOutline createBevelBorder(Widget content, boolean raised)
  {
    return new BOutline(content, BorderFactory.createBevelBorder(raised ? BevelBorder.RAISED : BevelBorder.LOWERED));
  }
  
  /**
   * Create a BOutline with a line border.
   *
   * @param content    the Widget to use as the content of the BOutline
   * @param color      the color of the border
   * @param thickness  the thickness of the border
   */
  
  public static BOutline createLineBorder(Widget content, Color color, int thickness)
  {
    return new BOutline(content, BorderFactory.createLineBorder(color, thickness));
  }
  
  /**
   * Get the Border object which draws this Widget's outline.
   */
  
  public Border getBorder()
  {
    return ((JComponent) component).getBorder();
  }
  
  /**
   * Set the Border object which draws this Widget's outline.
   */
  
  public void setBorder(Border border)
  {
    ((JComponent) component).setBorder(border);
    invalidateSize();
  }
  
  /**
   * Get the content Widget.
   */
  
  public Widget getContent()
  {
    return content;
  }
  
  /**
   * Set the content Widget.
   */
  
  public void setContent(Widget contentWidget)
  {
    if (content != null)
      remove(content);
    content = contentWidget;
    if (content != null)
    {
      if (content.getParent() != null)
        content.getParent().remove(content);
      ((JComponent) component).add(content.component);
      setAsParent(content);
    }
    invalidateSize();
  }

  /**
   * Get the number of children in this container.
   */
  
  public int getChildCount()
  {
    return (content == null ? 0 : 1);
  }
  
  /**
   * Get a Collection containing all child Widgets of this container.
   */
  
  public Collection<Widget> getChildren()
  {
    ArrayList<Widget> ls = new ArrayList<Widget>(1);
    if (content != null)
      ls.add(content);
    return ls;
  }
  
  /**
   * Remove a child Widget from this container.
   */
  
  public void remove(Widget widget)
  {
    if (content == widget)
    {
      ((JComponent) component).remove(widget.component);
      removeAsParent(content);
      content = null;
      invalidateSize();
    }
  }
  
  /**
   * Remove the content Widget from this container.
   */
  
  public void removeAll()
  {
    setContent(null);
  }
  
  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */
  
  public void layoutChildren()
  {
    Rectangle bounds = getBounds();
    Insets insets = ((JComponent) component).getInsets();
    if (content != null)
    {
      content.component.setBounds(new Rectangle(insets.left, insets.top,
          bounds.width-insets.left-insets.right, bounds.height-insets.top-insets.bottom));
      if (content instanceof WidgetContainer)
        ((WidgetContainer) content).layoutChildren();
    }
  }

  
  /**
   * Get the smallest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget smaller than its minimum size.
   */
  
  public Dimension getMinimumSize()
  {
    Dimension size = (content == null ? new Dimension() : content.getMinimumSize());
    Insets insets = ((JComponent) component).getInsets();
    return new Dimension(size.width+insets.left+insets.right, size.height+insets.top+insets.bottom);
  }
  
  /**
   * Get the largest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget larger than its maximum size.
   */
  
  public Dimension getMaximumSize()
  {
    Dimension size = (content == null ? new Dimension() : new Dimension(content.getMaximumSize()));
    Insets insets = ((JComponent) component).getInsets();
    if (size.width < Integer.MAX_VALUE-insets.left-insets.right)
      size.width += insets.left+insets.right;
    else
      size.width = Integer.MAX_VALUE;
    if (size.height < Integer.MAX_VALUE-insets.top-insets.bottom)
      size.height += insets.top+insets.bottom;
    else
      size.height = Integer.MAX_VALUE;
    return size;
  }

  /**
   * Get the preferred size at which this Widget will look best.  When a WidgetContainer lays out
   * its contents, it will attempt to make this Widget as close as possible to its preferred size.
   */
  
  public Dimension getPreferredSize()
  {
    Dimension size = (content == null ? new Dimension() : content.getPreferredSize());
    Insets insets = ((JComponent) component).getInsets();
    return new Dimension(size.width+insets.left+insets.right, size.height+insets.top+insets.bottom);
  }
}
