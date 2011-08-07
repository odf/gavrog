package buoy.event;

import buoy.widget.*;
import java.awt.event.*;

/**
 * This is an event corresponding to a key on the keyboard being pressed.
 *
 * @author Peter Eastman
 */

public class KeyPressedEvent extends WidgetKeyEvent
{
  /**
   * Create a KeyPressedEvent.
   *
   * @param source        the Widget which generated this event
   * @param when          the time at which the event occurred
   * @param modifiers     describes the state of various keys and buttons at the time when the event occurred
   *                      (a sum of the constants defined by InputEvent)
   * @param keyCode       specifies which key on the keyboard generated the event.  This should be one of
   *                      the VK_ constants.
   */
  
  public KeyPressedEvent(Widget source, long when, int modifiers, int keyCode)
  {
    super(source, KeyEvent.KEY_PRESSED, when, modifiers, keyCode, CHAR_UNDEFINED);
  }
}