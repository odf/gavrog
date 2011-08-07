package buoy.widget;

import buoy.event.*;
import java.awt.*;
import javax.swing.*;

/**
 * A BToolTip is a small floating window that appears in front of another Widget.  It contains a single
 * line of text, and generally provides information about the function of the Widget it is displayed
 * over.
 * <p>
 * The user triggers a tool tip by placing the mouse pointer over a Widget and leaving it there for a
 * fixed amount of time.  When this happens, the Widget sends out a {@link buoy.event.ToolTipEvent ToolTipEvent}.
 * The program should respond to the event by calling either {@link buoy.widget.BToolTip#show show()} or
 * {@link buoy.widget.BToolTip#processEvent processEvent()} to display the tool tip.
 * <p>
 * The easiest way of doing this is to have the BToolTip listen for the event directly.  For example,
 * you can set a fixed String as the tool tip for a button by invoking:
 * <p>
 * <pre>
 * button.addEventLink(ToolTipEvent.class, new BToolTip("Do Not Press This Button"));
 * </pre>
 * <p>
 * In some cases, you may want to do additional processing before the tool tip is displayed.  For
 * example, you might want to customize the tool tip text based on the mouse location, or exercise
 * finer control over where the tool tip appears.  In these cases, your program should listen for
 * the event and handle it in the appropriate way.
 * <p>
 * As an example, the following code will display a tool tip over a BTable, showing the row and column
 * the mouse is currently pointing to:
 * <p>
 * <pre>
 * table.addEventLink(ToolTipEvent.class, new Object()
 * {
 *   void processEvent(ToolTipEvent ev)
 *   {
 *     Point pos = ev.getPoint();
 *     String text = "("+table.findRow(pos)+", "+table.findColumn(pos)+")";
 *     new BToolTip(text).processEvent(ev);
 *   }
 * });
 * </pre>
 * <p>
 * There can only be one tool tip showing at any time.  It will automatically be hidden as soon as
 * a new tool tip is shown, or the mouse is moved away from the Widget.  You can call
 * {@link buoy.widget.BToolTip#getShowingToolTip getShowingToolTip()} to get the currently displayed
 * tool tip, or {@link buoy.widget.BToolTip#hide hide()} to hide it.
 *
 * @author Peter Eastman
 */

public class BToolTip extends Widget
{
  private static BToolTip currentTip;
  private static Popup tipWindow;
  
  /**
   * Create a new BToolTip with no text.
   */
  
  public BToolTip()
  {
    component = createComponent();
  }
  
  /**
   * Create a new BToolTip.
   *
   * @param text      the text to display in the tool tip
   */
  
  public BToolTip(String text)
  {
    this();
    setText(text);
  }
  
  /**
   * Create the JToolTip which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   */
  
  protected JToolTip createComponent()
  {
    return new JToolTip();
  }

  public JToolTip getComponent()
  {
    return (JToolTip) component;
  }

  /**
   * Get the text to display on the tool tip.
   */
  
  public String getText()
  {
    return getComponent().getTipText();
  }

  /**
   * Set the text to display on the tool tip.
   */
  
  public void setText(String text)
  {
    getComponent().setTipText(text);
    invalidateSize();
  }
  
  /**
   * Display the tool tip.
   *
   * @param widget    the Widget over which to display the tool tip
   * @param where     the location at which to display the tool tip
   */
  
  public void show(Widget widget, Point where)
  {
    hide();
    Point basePos = widget.getComponent().getLocationOnScreen();
    tipWindow = PopupFactory.getSharedInstance().getPopup(widget.getComponent(), getComponent(), basePos.x+where.x, basePos.y+where.y);
    tipWindow.show();
    currentTip = this;
  }
  
  /**
   * Display the tool tip in response to a ToolTipEvent.
   *
   * @param event    the event from which to determine where to display the tool tip
   */
  
  public void processEvent(ToolTipEvent event)
  {
    Widget widget = event.getWidget();
    Point where = event.getToolTipLocation();
    
    // Try to keep the tool tip inside the window.
    
    Window window = SwingUtilities.getWindowAncestor(widget.getComponent());
    if (window != null)
    {
      where = SwingUtilities.convertPoint(widget.getComponent(), where, window);
      Dimension winSize = window.getSize();
      Dimension tipSize = getPreferredSize();
      if (where.x+tipSize.width > winSize.width)
        where.x = Math.max(0, winSize.width-tipSize.width);
      if (where.y+tipSize.height > winSize.height)
        where.y = Math.max(0, winSize.height-tipSize.height);
      where = SwingUtilities.convertPoint(window, where, widget.getComponent());
    }
    show(widget, where);
  }
  
  /**
   * Get the currently showing tool tip, or null if none is showing.
   */
  
  public static BToolTip getShowingToolTip()
  {
    return currentTip;
  }
  
  /**
   * Hide the currently showing tool tip.
   */
  
  public static void hide()
  {
    if (tipWindow != null)
      tipWindow.hide();
    tipWindow = null;
    currentTip = null;
  }
}
