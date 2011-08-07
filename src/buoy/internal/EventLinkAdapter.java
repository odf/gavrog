package buoy.internal;

import buoy.widget.*;
import buoy.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class handles interaction with the AWT event model.  Its job is to implement every listener
 * interface, then generate and dispatch appropriate event objects.
 *
 * @author Peter Eastman
 */

public class EventLinkAdapter implements FocusListener, KeyListener, MouseListener,
    MouseMotionListener, MouseWheelListener, WindowListener
{
  private Widget widget;
  private int eventFlags;
  
  private static final int MOUSE_EVENT = 1;
  private static final int MOUSE_MOTION_EVENT = 2;
  private static final int MOUSE_WHEEL_EVENT = 4;
  private static final int KEY_EVENT = 8;
  private static final int FOCUS_EVENT = 16;
  private static final int WINDOW_EVENT = 32;
  
  /**
   * Create a new adapter for a particular Widget.
   */
  
  public EventLinkAdapter(Widget widget)
  {
    this.widget = widget;
  }
  
  /**
   * This is called when a new event link is added to a Widget.  It checks to see whether this adapter
   * has already been added as a listener for that event type, and if not, it adds itself.
   */
  
  public void newEventType(Class eventType)
  {
    if ((eventFlags & MOUSE_EVENT) == 0 &&
        (eventType.isAssignableFrom(MousePressedEvent.class) ||
        eventType.isAssignableFrom(MouseReleasedEvent.class) ||
        eventType.isAssignableFrom(MouseClickedEvent.class) ||
        eventType.isAssignableFrom(MouseEnteredEvent.class) ||
        eventType.isAssignableFrom(MouseExitedEvent.class) ||
        eventType.isAssignableFrom(ToolTipEvent.class)))
    {
      eventFlags |= MOUSE_EVENT;
      widget.getComponent().addMouseListener(this);
    }

    if ((eventFlags & MOUSE_MOTION_EVENT) == 0 &&
        (eventType.isAssignableFrom(MouseMovedEvent.class) ||
        eventType.isAssignableFrom(MouseDraggedEvent.class) ||
        eventType.isAssignableFrom(ToolTipEvent.class)))
    {
      eventFlags |= MOUSE_MOTION_EVENT;
      widget.getComponent().addMouseMotionListener(this);
    }

    if ((eventFlags & MOUSE_WHEEL_EVENT) == 0 && eventType.isAssignableFrom(MouseScrolledEvent.class))
    {
      eventFlags |= MOUSE_WHEEL_EVENT;
      widget.getComponent().addMouseWheelListener(this);
    }

    if ((eventFlags & KEY_EVENT) == 0 &&
        (eventType.isAssignableFrom(KeyPressedEvent.class) ||
        eventType.isAssignableFrom(KeyReleasedEvent.class) ||
        eventType.isAssignableFrom(KeyTypedEvent.class)))
    {
      eventFlags |= KEY_EVENT;
      widget.getComponent().addKeyListener(this);
    }

    if ((eventFlags & FOCUS_EVENT) == 0 &&
        (eventType.isAssignableFrom(FocusGainedEvent.class) ||
        eventType.isAssignableFrom(FocusLostEvent.class)))
    {
      eventFlags |= FOCUS_EVENT;
      widget.getComponent().addFocusListener(this);
    }

    if ((eventFlags & WINDOW_EVENT) == 0 && widget instanceof WindowWidget && widget.getComponent() instanceof Window &&
        (eventType.isAssignableFrom(WindowActivatedEvent.class) ||
        eventType.isAssignableFrom(WindowClosingEvent.class) ||
        eventType.isAssignableFrom(WindowDeactivatedEvent.class) ||
        eventType.isAssignableFrom(WindowDeiconifiedEvent.class) ||
        eventType.isAssignableFrom(WindowIconifiedEvent.class)))
    {
      eventFlags |= WINDOW_EVENT;
      ((Window) widget.getComponent()).addWindowListener(this);
    }
  }
  
  public void mousePressed(MouseEvent ev)
  {
    dispatchEvent(ev, new MousePressedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getX(), ev.getY(),
        ev.getClickCount(), ev.isPopupTrigger(), ev.getButton()));
  }
  
  public void mouseReleased(MouseEvent ev)
  {
    dispatchEvent(ev, new MouseReleasedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getX(), ev.getY(),
        ev.getClickCount(), ev.isPopupTrigger(), ev.getButton()));
  }
  
  public void mouseClicked(MouseEvent ev)
  {
    dispatchEvent(ev, new MouseClickedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getX(), ev.getY(),
        ev.getClickCount(), ev.isPopupTrigger(), ev.getButton()));
  }
  
  public void mouseEntered(MouseEvent ev)
  {
    dispatchEvent(ev, new MouseEnteredEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getX(), ev.getY()));
  }
  
  public void mouseExited(MouseEvent ev)
  {
    dispatchEvent(ev, new MouseExitedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getX(), ev.getY()));
  }

  public void mouseMoved(MouseEvent ev)
  {
    dispatchEvent(ev, new MouseMovedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getX(), ev.getY()));
  }

  public void mouseDragged(MouseEvent ev)
  {
    dispatchEvent(ev, new MouseDraggedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getX(), ev.getY()));
  }

  public void mouseWheelMoved(MouseWheelEvent ev)
  {
    dispatchEvent(ev, new MouseScrolledEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getX(), ev.getY(), ev.getScrollType(), ev.getScrollAmount(), ev.getWheelRotation()));
  }

  public void keyPressed(KeyEvent ev)
  {
    dispatchEvent(ev, new KeyPressedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getKeyCode()));
  }

  public void keyReleased(KeyEvent ev)
  {
    dispatchEvent(ev, new KeyReleasedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getKeyCode()));
  }

  public void keyTyped(KeyEvent ev)
  {
    dispatchEvent(ev, new KeyTypedEvent(widget, ev.getWhen(), ev.getModifiersEx(), ev.getKeyChar()));
  }

  public void focusGained(FocusEvent ev)
  {
    widget.dispatchEvent(new FocusGainedEvent(widget, ev.isTemporary()));
  }

  public void focusLost(FocusEvent ev)
  {
    widget.dispatchEvent(new FocusLostEvent(widget, ev.isTemporary()));
  }

  public void windowActivated(WindowEvent ev)
  {
    widget.dispatchEvent(new WindowActivatedEvent((WindowWidget) widget));
  }

  public void windowClosed(WindowEvent ev)
  {
    // This event is ignored, since it does not represent a user action.
  }

  public void windowClosing(WindowEvent ev)
  {
    widget.dispatchEvent(new WindowClosingEvent((WindowWidget) widget));
  }

  public void windowDeactivated(WindowEvent ev)
  {
    widget.dispatchEvent(new WindowDeactivatedEvent((WindowWidget) widget));
  }

  public void windowDeiconified(WindowEvent ev)
  {
    widget.dispatchEvent(new WindowDeiconifiedEvent((WindowWidget) widget));
  }

  public void windowIconified(WindowEvent ev)
  {
    widget.dispatchEvent(new WindowIconifiedEvent((WindowWidget) widget));
  }

  public void windowOpened(WindowEvent ev)
  {
    // This event is ignored, since it does not represent a user action.
  }

  /**
   * Dispatch a Buoy event in response to an AWT event, then consume the original event
   * if the Buoy event was consumed.
   */

  private void dispatchEvent(InputEvent originalEvent, InputEvent newEvent)
  {
    if (originalEvent.isConsumed())
      newEvent.consume();
    widget.dispatchEvent(newEvent);
    if (newEvent.isConsumed())
      originalEvent.consume();
  }
}