package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;
import java.awt.*;
import javax.swing.*;

/**
 * A BLabel is a Widget that displays a text string, an image, or both.  The text may be specified as
 * HTML, allowing it to contain complex formatting, multiple fonts, etc.
 *
 * @author Peter Eastman
 */

public class BLabel extends Widget
{
  public static final Position CENTER = new Position(0);
  public static final Position NORTH = new Position(1);
  public static final Position SOUTH = new Position(2);
  public static final Position WEST = new Position(4);
  public static final Position EAST = new Position(8);
  public static final Position NORTHEAST = new Position(NORTH.value+EAST.value);
  public static final Position SOUTHEAST = new Position(SOUTH.value+EAST.value);
  public static final Position NORTHWEST = new Position(NORTH.value+WEST.value);
  public static final Position SOUTHWEST = new Position(SOUTH.value+WEST.value);

  static
  {
    WidgetEncoder.setPersistenceDelegate(Position.class, new StaticFieldDelegate(BLabel.class));
  }

  /**
   * Create a new BLabel with no text or image.
   */
  
  public BLabel()
  {
    this((String) null, WEST);
  }

  /**
   * Create a new BLabel which displays text.
   *
   * @param text     the text to display on the BLabel
   */
  
  public BLabel(String text)
  {
    this(text, WEST);
  }

  /**
   * Create a new BLabel which displays text.
   *
   * @param text     the text to display on the BLabel
   * @param align    the alignment of the label contents (CENTER, NORTH, NORTHEAST, etc.)
   */
  
  public BLabel(String text, Position align)
  {
    component = createComponent(text, null);
    setAlignment(align);
  }

  /**
   * Create a new BLabel which displays an image.
   *
   * @param image     the image to display on the BLabel
   */
  
  public BLabel(Icon image)
  {
    this(image, WEST);
  }

  /**
   * Create a new BLabel which displays an image.
   *
   * @param image     the image to display on the BLabel
   * @param align     the alignment of the label contents (CENTER, NORTH, NORTHEAST, etc.)
   */
  
  public BLabel(Icon image, Position align)
  {
    component = createComponent(null, image);
    setAlignment(align);
  }

  /**
   * Create a new BLabel which displays both text and an image.
   *
   * @param text        the text to display on the BLabel
   * @param image       the image to display on the BLabel
   * @param align       the alignment of the label contents (CENTER, NORTH, NORTHEAST, etc.)
   * @param textPos     the position of the text relative to the image (CENTER, NORTH, NORTHEAST, etc.)
   */
  
  public BLabel(String text, Icon image, Position align, Position textPos)
  {
    component = createComponent(text, image);
    setAlignment(align);
    setTextPosition(textPos);
  }
  
  /**
   * Create the JLabel which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   *
   * @param text        the text to display on the BLabel
   * @param image       the image to display on the BLabel
   */
  
  protected JLabel createComponent(String text, Icon image)
  {
    return new JLabel(text, image, SwingConstants.RIGHT);
  }

  public JLabel getComponent()
  {
    return (JLabel) component;
  }

  /**
   * Get the text which appears on this label.
   */
  
  public String getText()
  {
    return getComponent().getText();
  }
  
  /**
   * Set the text which appears on this label.
   */
  
  public void setText(String text)
  {
    getComponent().setText(text);
    invalidateSize();
  }
  
  /**
   * Get the image which appears on this label.
   */
  
  public Icon getIcon()
  {
    return getComponent().getIcon();
  }
  
  /**
   * Set the image which appears on this label.
   */
  
  public void setIcon(Icon image)
  {
    getComponent().setIcon(image);
    invalidateSize();
  }

  /**
   * Get the largest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget larger than its maximum size.
   */
  
  public Dimension getMaximumSize()
  {
    return new Dimension(32767, 32767);
  }
  
  /**
   * Get the alignment of the label's contents.  This will be one of the alignment constants
   * defined by this class: CENTER, NORTH, NORTHEAST, EAST, etc.
   */

  public Position getAlignment()
  {
    int halign = getComponent().getHorizontalAlignment();
    int valign = getComponent().getVerticalAlignment();
    return Position.get(halign, valign);
  }

  /**
   * Set the alignment of the label's contents.  This should be one of the alignment constants
   * defined by this class: CENTER, NORTH, NORTHEAST, EAST, etc.
   */

  public void setAlignment(Position alignment)
  {
    int align = alignment.value;
    JLabel jl = getComponent();
    if ((align&NORTH.value) != 0)
      jl.setVerticalAlignment(SwingConstants.TOP);
    else if ((align&SOUTH.value) != 0)
      jl.setVerticalAlignment(SwingConstants.BOTTOM);
    else
      jl.setVerticalAlignment(SwingConstants.CENTER);
    if ((align&EAST.value) != 0)
      jl.setHorizontalAlignment(SwingConstants.RIGHT);
    else if ((align&WEST.value) != 0)
      jl.setHorizontalAlignment(SwingConstants.LEFT);
    else
      jl.setHorizontalAlignment(SwingConstants.CENTER);
    invalidateSize();
  }
  
  /**
   * Get the position of the text relative to the image.  This will be one of the alignment constants
   * defined by this class: CENTER, NORTH, NORTHEAST, EAST, etc.
   */

  public Position getTextPosition()
  {
    int hpos = getComponent().getHorizontalTextPosition();
    int vpos = getComponent().getVerticalTextPosition();
    return Position.get(hpos, vpos);
  }
  
  /**
   * Set the position of the text relative to the image.  This should be one of the alignment constants
   * defined by this class: CENTER, NORTH, NORTHEAST, EAST, etc.  If this BLabel does not display both
   * text and an image, this method will have no effect.
   */

  public void setTextPosition(Position position)
  {
    int pos = position.value;
    JLabel jl = getComponent();
    if ((pos&NORTH.value) != 0)
      jl.setVerticalTextPosition(SwingConstants.TOP);
    else if ((pos&SOUTH.value) != 0)
      jl.setVerticalTextPosition(SwingConstants.BOTTOM);
    else
      jl.setVerticalTextPosition(SwingConstants.CENTER);
    if ((pos&EAST.value) != 0)
      jl.setHorizontalTextPosition(SwingConstants.RIGHT);
    else if ((pos&WEST.value) != 0)
      jl.setHorizontalTextPosition(SwingConstants.LEFT);
    else
      jl.setHorizontalTextPosition(SwingConstants.CENTER);
    invalidateSize();
  }

  /**
   * This inner class represents a value for the alignment or text position.
   */
  
  public static class Position
  {
    protected int value;
    
    private Position(int value)
    {
      this.value = value;
    }
    
    private static Position get(int hpos, int vpos)
    {
      switch (hpos)
      {
        case SwingConstants.LEFT:
          switch (vpos)
          {
            case SwingConstants.TOP:
              return NORTHWEST;
            case SwingConstants.BOTTOM:
              return SOUTHWEST;
            default:
              return WEST;
          }
        case SwingConstants.RIGHT:
          switch (vpos)
          {
            case SwingConstants.TOP:
              return NORTHEAST;
            case SwingConstants.BOTTOM:
              return SOUTHEAST;
            default:
              return EAST;
          }
        default:
          switch (vpos)
          {
            case SwingConstants.TOP:
              return NORTH;
            case SwingConstants.BOTTOM:
              return SOUTH;
            default:
              return CENTER;
          }
      }
    }
  }
}