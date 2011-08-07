package buoy.widget;

import java.awt.*;
import javax.swing.*;

/**
 * A WindowWidget is a WidgetContainer corresponding to a window.  This is an abstract class, with
 * subclasses for particular types of windows.
 *
 * @author Peter Eastman
 */

public abstract class WindowWidget extends WidgetContainer
{
  protected Widget content;
  protected Dimension lastSize;

  private Boolean mockVisible;
  private BButton defaultButton;
  private static ThreadLocal encodingInProgress = new ThreadLocal();


  public Window getComponent()
  {
    return (Window) component;
  }

  /**
   * Set the position and size of the window, then re-layout the window contents.
   */
  
  public void setBounds(Rectangle bounds)
  {
    if (encodingInProgress.get() != Boolean.TRUE && !getComponent().isDisplayable())
      getComponent().addNotify();
    lastSize = new Dimension(bounds.width, bounds.height);
    getComponent().setBounds(bounds);
  }
  
  /**
   * Get the Widget that holds the main contents of the window.
   */

  public Widget getContent()
  {
    return content;
  }
  
  /**
   * Set the Widget that holds the main contents of the window.
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
      JComponent contentPane = (JComponent) ((RootPaneContainer) getComponent()).getContentPane();
      contentPane.add(content.getComponent());
      setAsParent(content);
    }
  }

  /**
   * Select an appropriate size for the window, based on the preferred size of its contents, then re-layout
   * all of the window contents.
   */
  
  public void pack()
  {
    if (!getComponent().isDisplayable())
      getComponent().addNotify();
    JComponent contentPane = (JComponent) ((RootPaneContainer) getComponent()).getContentPane();
    if (content == null)
      contentPane.setPreferredSize(new Dimension(0, 0));
    else
      contentPane.setPreferredSize(content.getPreferredSize());
    Dimension prefSize = getPreferredSize();
    Rectangle bounds = getBounds();
    setBounds(new Rectangle(bounds.x, bounds.y, prefSize.width, prefSize.height));
  }
  
  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */
  
  public void layoutChildren()
  {
    if (content != null)
    {
      Container contentPane = ((RootPaneContainer) getComponent()).getContentPane();
      contentPane.validate();
      Dimension max = content.getMaximumSize();
      Dimension total = contentPane.getSize();
      content.getComponent().setBounds(0, 0, Math.min(max.width, total.width), Math.min(max.height, total.height));
      if (content instanceof WidgetContainer)
        ((WidgetContainer) content).layoutChildren();
    }
  }
  
  /**
   * Close the window, and dispose of all resources associated with it.
   */
  
  public void dispose()
  {
    getComponent().dispose();
  }
  
  /**
   * Request that this window be brought to the front, so that it is displayed over all other
   * windows.
   * <p>
   * Note that the behavior of this method is highly platform dependent.  It is not guaranteed
   * to work on all platforms.  In addition, this method may or may not affect which Widget
   * has focus.
   */
  
  public void toFront()
  {
    getComponent().toFront();
  }
  
  /**
   * Request that this window be sent to the back, so that it is displayed behind all other
   * windows.
   * <p>
   * Note that the behavior of this method is highly platform dependent.  It is not guaranteed
   * to work on all platforms.  In addition, this method may or may not affect which Widget
   * has focus.
   */
  
  public void toBack()
  {
    getComponent().toBack();
  }
  
  /**
   * Determine whether this Widget is currently visible.
   */
  
  public boolean isVisible()
  {
    if (mockVisible != null)
    {
      // This window was created internally in the process of encoding a window as XML.
      
      return mockVisible.booleanValue();
    }
    return getComponent().isVisible();
  }
  
  /**
   * Set whether this Widget should be visible.
   */
  
  public void setVisible(boolean visible)
  {
    if (encodingInProgress.get() == Boolean.TRUE)
    {
      // This window was created internally in the process of encoding a window as XML.
      // Do not really make it visible, but pretend to.
      
      mockVisible = (visible ? Boolean.TRUE : Boolean.FALSE);
      return;
    }
    super.setVisible(visible);
  }

  /**
   * Get the default button for this window.  If the user presses the Return or Enter key
   * while the window has focus, it will be treated as if they had clicked the default
   * button (unless another Widget first consumes the event).  The default button is
   * typically drawn differently to indicate its special status.  This may be null.
   */

  public BButton getDefaultButton()
  {
    return defaultButton;
  }

  /**
   * Set the default button for this window.  If the user presses the Return or Enter key
   * while the window has focus, it will be treated as if they had clicked the default
   * button (unless another Widget first consumes the event).  The default button is
   * typically drawn differently to indicate its special status.  This may be null.
   */

  public void setDefaultButton(BButton button)
  {
    defaultButton = button;
    getRootPane().setDefaultButton(button == null ? null : (JButton) button.getComponent());
  }

  /**
   * Get the JRootPane for this Widget's component.
   */

  protected abstract JRootPane getRootPane();
}