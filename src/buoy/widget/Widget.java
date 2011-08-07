package buoy.widget;

import buoy.event.*;
import buoy.internal.*;
import buoy.xml.*;
import java.awt.*;
import java.lang.reflect.*;

/**
 * A Widget is a graphical object.  It occupies a fixed region of the screen, and can respond to user actions.
 * This is an abstract class.  There are subclasses corresponding to particular kinds of Widgets.
 * <p>
 * Every Widget is mapped to a specific AWT/Swing Component.  This class can therefore be thought of as a
 * wrapper around the Component class.
 * <p>
 * All Widgets generate the following types of events:
 * <ul>
 * <li>{@link buoy.event.FocusGainedEvent FocusGainedEvent}</li>
 * <li>{@link buoy.event.FocusLostEvent FocusLostEvent}</li>
 * <li>{@link buoy.event.KeyPressedEvent KeyPressedEvent}</li>
 * <li>{@link buoy.event.KeyReleasedEvent KeyReleasedEvent}</li>
 * <li>{@link buoy.event.KeyTypedEvent KeyTypedEvent}</li>
 * <li>{@link buoy.event.MousePressedEvent MousePressedEvent}</li>
 * <li>{@link buoy.event.MouseReleasedEvent MouseReleasedEvent}</li>
 * <li>{@link buoy.event.MouseClickedEvent MouseClickedEvent}</li>
 * <li>{@link buoy.event.MouseEnteredEvent MouseEnteredEvent}</li>
 * <li>{@link buoy.event.MouseExitedEvent MouseExitedEvent}</li>
 * <li>{@link buoy.event.MouseMovedEvent MouseMovedEvent}</li>
 * <li>{@link buoy.event.MouseDraggedEvent MouseDraggedEvent}</li>
 * <li>{@link buoy.event.MouseScrolledEvent MouseScrolledEvent}</li>
 * <li>{@link buoy.event.ToolTipEvent ToolTipEvent}</li>
 * </ul>
 *
 * @author Peter Eastman
 */

public abstract class Widget extends EventSource
{
  protected Component component;
  protected EventLinkAdapter eventAdapter;
  private WidgetContainer parent;
  private boolean wantsToolTipEvents;
  
  /**
   * Because Widget is an abstract class, it cannot be instantiated directly.
   */
  
  protected Widget()
  {
  }
  
  /**
   * Get the java.awt.Component corresponding to this Widget.
   */
  
  public Component getComponent()
  {
    return component;
  }
  
  /**
   * Get this Widget's parent in the layout hierarchy (may be null).
   */
  
  public WidgetContainer getParent()
  {
    return parent;
  }
  
  /**
   * Set this Widget's parent in the layout hierarchy (may be null).
   */
  
  protected void setParent(WidgetContainer container)
  {
    parent = container;
  }
  
  /**
   * This method should be called any time this Widget's minimum, maximum, or preferred size changes.
   * The default implementation simply calls invalidateSize() on the Widget's parent.  Subclasses
   * that cache layout information should override this to discard the cached information.
   */
  
  protected void invalidateSize()
  {
    if (parent != null)
      parent.invalidateSize();
  }
  
  /**
   * Get the smallest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget smaller than its minimum size.
   */
  
  public Dimension getMinimumSize()
  {
    return component.getMinimumSize();
  }
  
  /**
   * Get the largest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget larger than its maximum size.
   */
  
  public Dimension getMaximumSize()
  {
    return component.getMaximumSize();
  }

  /**
   * Get the preferred size at which this Widget will look best.  When a WidgetContainer lays out
   * its contents, it will attempt to make this Widget as close as possible to its preferred size.
   */
  
  public Dimension getPreferredSize()
  {
    return component.getPreferredSize();
  }
  
  /**
   * Get the current location and size of this Widget.
   */
  
  public Rectangle getBounds()
  {
    return component.getBounds();
  }
  
  /**
   * Request that this Widget be repainted.  This method does not actually paint the Widget immediately.
   * Instead, it causes a repaint request to be placed onto the global event queue.  As a result, this
   * method can be safely called from any thread.
   */
  
  public void repaint()
  {
    component.repaint();
  }
  
  /**
   * Determine whether this Widget is currently visible.
   */
  
  public boolean isVisible()
  {
    return component.isVisible();
  }
  
  /**
   * Set whether this Widget should be visible.
   */
  
  public void setVisible(boolean visible)
  {
    component.setVisible(visible);
    invalidateSize();
  }
  
  /**
   * Determine whether this Widget is currently enabled.
   */
  
  public boolean isEnabled()
  {
    return component.isEnabled();
  }
  
  /**
   * Set whether this Widget should be enabled.
   */
  
  public void setEnabled(boolean enabled)
  {
    component.setEnabled(enabled);
  }
  
  /**
   * Get the Cursor to display when the mouse is over this Widget.
   */
  
  public Cursor getCursor()
  {
    return component.getCursor();
  }
  
  /**
   * Set the Cursor to display when the mouse is over this Widget.
   */
  
  public void setCursor(Cursor cursor)
  {
    component.setCursor(cursor);
  }
  
  /**
   * Get the background color of this Widget.  If that is null and this Widget has been added to a
   * WidgetContainer, this returns the background color of the parent container.
   */
  
  public Color getBackground()
  {
    return component.getBackground();
  }
  
  /**
   * Set the background color of this Widget.  If this is set to null, the Widget will use the background
   * color of its parent WidgetContainer.
   */
  
  public void setBackground(Color background)
  {
    component.setBackground(background);
  }
  
  /**
   * Get the font used to draw text in this Widget.
   */
  
  public Font getFont()
  {
    return component.getFont();
  }
  
  /**
   * Set the font used to draw text in this Widget.  If this is set to null, the Widget will use the
   * Font of its parent WidgetContainer.
   */
  
  public void setFont(Font font)
  {
    component.setFont(font);
    invalidateSize();
  }
  
  /**
   * Determine whether this Widget currently has keyboard focus, so that WidgetKeyEvents will be sent
   * to it.
   */
  
  public boolean hasFocus()
  {
    return component.hasFocus();
  }

  /**
   * Request that keyboard focus be transferred to this Widget, so that WidgetKeyEvents will be sent
   * to it.
   */
  
  public void requestFocus()
  {
    component.requestFocus();
  }

  /**
   * Determine whether this Widget can receive keyboard focus through the user pressing Tab or Shift-Tab to
   * cycle between Widgets.
   */
  
  public boolean isFocusable()
  {
    return component.isFocusable();
  }

  /**
   * Set whether this Widget can receive keyboard focus through the user pressing Tab or Shift-Tab to
   * cycle between Widgets.
   */
  
  public void setFocusable(boolean focusable)
  {
    component.setFocusable(focusable);
  }
  
  /**
   * Get the name of this Widget.  This is used primarily to identify Widgets when they are serialized
   * to XML files.
   */
  
  public String getName()
  {
    return component.getName();
  }
  
  /**
   * Set the name of this Widget.  This is used primarily to identify Widgets when they are serialized
   * to XML files.
   */
  
  public void setName(String name)
  {
    component.setName(name);
    if (name != null)
      WidgetDecoder.registerObject(name, this);
  }

  /**
   * Create an event link from this object.  When events of the desired class (or any of its subclasses) are
   * generated by this object, the specified method will be called on the target object.
   *
   * @param eventType    the event class or interface which the target method wants to receive
   * @param target       the object to send the events to
   * @param method       the method to invoke on the target object.  The method must either take no
   *                     arguments, or take an object of class eventType (or any of its superclasses or
   *                     interfaces) as its only argument.
   */
  
  public void addEventLink(Class eventType, Object target, Method method)
  {
    super.addEventLink(eventType, target, method);
    if (eventAdapter == null)
      eventAdapter = new EventLinkAdapter(this);
    eventAdapter.newEventType(eventType);
    if (eventType.isAssignableFrom(ToolTipEvent.class))
      wantsToolTipEvents = true;
  }

  
  /**
   * Send out an object representing an event to every appropriate event link that has been added to this object.
   */
  
  public void dispatchEvent(Object event)
  {
    super.dispatchEvent(event);
    if (wantsToolTipEvents && event instanceof WidgetMouseEvent)
      ToolTipMonitor.processMouseEvent((WidgetMouseEvent) event);
  }
}