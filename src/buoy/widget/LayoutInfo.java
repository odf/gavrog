package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;
import java.awt.*;

/**
 * A LayoutInfo object contains information about how a Widget should be layed out within the space provided
 * by its WidgetContainer.  Most containers allow you to specify a default LayoutInfo for all Widgets, and also
 * to specify a different one for each Widget.
 * <p>
 * It is permitted to reuse a single LayoutInfo object for multiple Widgets.  In that case, any change made to the
 * LayoutInfo will affect all Widgets for which it is used.
 *
 * @author Peter Eastman
 */

public class LayoutInfo implements Cloneable
{
  private Alignment align;
  private FillType fill;
  private Insets insets;
  private Dimension padding;
  
  public static final Alignment CENTER = new Alignment(0);
  public static final Alignment NORTH = new Alignment(1);
  public static final Alignment SOUTH = new Alignment(2);
  public static final Alignment WEST = new Alignment(4);
  public static final Alignment EAST = new Alignment(8);
  public static final Alignment NORTHEAST = new Alignment(NORTH.value+EAST.value);
  public static final Alignment SOUTHEAST = new Alignment(SOUTH.value+EAST.value);
  public static final Alignment NORTHWEST = new Alignment(NORTH.value+WEST.value);
  public static final Alignment SOUTHWEST = new Alignment(SOUTH.value+WEST.value);
  
  public static final FillType NONE = new FillType();
  public static final FillType HORIZONTAL = new FillType();
  public static final FillType VERTICAL = new FillType();
  public static final FillType BOTH = new FillType();

  static
  {
    WidgetEncoder.setPersistenceDelegate(Alignment.class, new StaticFieldDelegate(LayoutInfo.class));
    WidgetEncoder.setPersistenceDelegate(FillType.class, new StaticFieldDelegate(LayoutInfo.class));
  }

  /**
   * Create a LayoutInfo object with the following default values: no padding, no insets, center alignment,
   * and no fill.
   */
  
  public LayoutInfo()
  {
    this(CENTER, NONE, null, null);
  }

  /**
   * Create a LayoutInfo object with no padding or insets.
   *
   * @param align      the alignment of the Widget.  This should be equal to one of the alignment constants
   *                   defined by this class: CENTER, NORTH, NORTHEAST, EAST, etc.
   * @param fill       specifies whether the Widget should grow to fill the available space.  This should be
   *                   equal to NONE, HORIZONTAL, VERTICAL, BOTH.
   */
  
  public LayoutInfo(Alignment align, FillType fill)
  {
    this(align, fill, null, null);
  }
  
  /**
   * Create a LayoutInfo object.
   *
   * @param align      the alignment of the Widget.  This should be equal to one of the alignment constants
   *                   defined by this class: CENTER, NORTH, NORTHEAST, EAST, etc.
   * @param fill       specifies whether the Widget should grow to fill the available space.  This should be
   *                   equal to NONE, HORIZONTAL, VERTICAL, BOTH.
   * @param insets     specifies extra space (in pixels) that should be added around the Widget.  If this is
   *                   null, no insets will be used.
   * @param padding    extra padding, in pixels.  This requests that the Widget be made larger than its
   *                   preferred size.  If this is null, no padding will be used.
   */
  
  public LayoutInfo(Alignment align, FillType fill, Insets insets, Dimension padding)
  {
    this.align = align;
    this.fill = fill;
    this.insets = insets;
    this.padding = padding;
  }
  
  /**
   * Get the alignment of the Widget within its available space.  This should be equal to one of the alignment
   * constants defined by this class: CENTER, NORTH, NORTHEAST, EAST, etc.
   */
  
  public Alignment getAlignment()
  {
    return align;
  }
  
  /**
   * Set the alignment of the Widget within its available space.  This should be equal to one of the alignment
   * constants defined by this class: CENTER, NORTH, NORTHEAST, EAST, etc.
   */
  
  public void setAlignment(Alignment align)
  {
    this.align = align;
  }
  
  /**
   * Get whether the Widget should grow to fill the available space.  This should be equal to NONE,
   * HORIZONTAL, VERTICAL, BOTH.
   */
  
  public FillType getFill()
  {
    return fill;
  }
  
  /**
   * Set whether the Widget should grow to fill the available space.  This should be equal to NONE,
   * HORIZONTAL, VERTICAL, BOTH.
   */
  
  public void setFill(FillType fill)
  {
    this.fill = fill;
  }
  
  /**
   * Get the extra space (in pixels) that should be added around the Widget.  If this is null, no insets
   * will be used.
   */
  
  public Insets getInsets()
  {
    return insets;
  }

  /**
   * Set the extra space (in pixels) that should be added around the Widget.  If this is null, no insets
   * will be used.
   */
  
  public void setInsets(Insets insets)
  {
    this.insets = insets;
  }
  
  /**
   * Get the extra padding, in pixels, that should be added to the preferred size of the Widget.  If this is
   * null, no padding will be used.
   */

  public Dimension getPadding()
  {
    return padding;
  }

  /**
   * Set the extra padding, in pixels, that should be added to the preferred size of the Widget.  If this is
   * null, no padding will be used.
   */
  
  public void setPadding(Dimension padding)
  {
    this.padding = padding;
  }
  
  /**
   * Create a duplicate of this object.
   */
  
  public Object clone()
  {
    return new LayoutInfo(align, fill, insets == null ? null : (Insets) insets.clone(), padding == null ? null : (Dimension) padding.clone());
  }
  
  /**
   * Get the preferred size of a Widget, taking into account the insets and padding specified by this object.
   */
  
  public Dimension getPreferredSize(Widget widget)
  {
    Dimension dim = widget.getPreferredSize().getSize();
    if (insets != null)
    {
      dim.width += insets.left+insets.right;
      dim.height += insets.top+insets.bottom;
    }
    if (padding != null)
    {
      dim.width += padding.width;
      dim.height += padding.height;
    }
    return dim;
  }
  
  /**
   * Get the maximum size of a Widget, taking into account the insets and padding specified by this object.
   */
  
  public Dimension getMaximumSize(Widget widget)
  {
    Dimension dim = widget.getMaximumSize().getSize();
    if (insets != null)
    {
      dim.width += insets.left+insets.right;
      dim.height += insets.top+insets.bottom;
    }
    if (padding != null)
    {
      dim.width += padding.width;
      dim.height += padding.height;
    }
    return dim;
  }
  
  /**
   * Given a Widget, and a Rectangle in which to position it, return the desired bounds of the Widget.
   *
   * @param widget     the Widget to position
   * @param rect       the rectangle within which the Widget should be positioned.  This is typically the
   *                   region a WidgetContainer has set aside for this particular Widget.
   * @return the bounds which should be set for the Widget
   */
  
  public Rectangle getWidgetLayout(Widget widget, Rectangle rect)
  {
    Dimension pref = widget.getPreferredSize();
    Dimension max = widget.getMaximumSize();
    int left, right, bottom, top, xpad, ypad;
    if (insets == null)
      left = right = bottom = top = 0;
    else
    {
      left = insets.left;
      right = insets.right;
      bottom = insets.bottom;
      top = insets.top;
    }
    if (padding == null)
      xpad = ypad = 0;
    else
    {
      xpad = padding.width;
      ypad = padding.height;
    }
    int width = (fill == HORIZONTAL || fill == BOTH ? rect.width-left-right : pref.width+xpad);
    int height = (fill == VERTICAL || fill == BOTH ? rect.height-top-bottom : pref.height+ypad);
    if (width > max.width)
      width = max.width;
    if (height > max.height)
      height = max.height;
    int x, y;
    if ((align.value & NORTH.value) != 0)
      y = rect.y+top;
    else if ((align.value & SOUTH.value) != 0)
      y = rect.y+rect.height-bottom-height;
    else
      y = rect.y+((rect.height-height)>>1);
    if ((align.value & WEST.value) != 0)
      x = rect.x+left;
    else if ((align.value & EAST.value) != 0)
      x = rect.x+rect.width-right-width;
    else
      x = rect.x+((rect.width-width)>>1);
    return new Rectangle(x, y, width, height);
  }

  /**
   * This inner class represents a value for the alignment.
   */
  
  public static class Alignment
  {
    protected int value;
    
    private Alignment(int value)
    {
      this.value = value;
    }
  }

  /**
   * This inner class represents a value for the fill.
   */
  
  public static class FillType
  {
    private FillType()
    {
    }
  }
}