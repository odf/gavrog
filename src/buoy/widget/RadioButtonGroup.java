package buoy.widget;

import buoy.event.*;
import java.util.*;
import javax.swing.*;

/**
 * A RadioButtonGroup manages a set of {@link BRadioButton BRadioButtons} and
 * {@link BRadioButtonMenuItem BRadioButtonMenuItems}.  It ensures that only
 * one member of the group is selected at any time.  It provides methods for determining which member
 * is currently selected, and for changing the selection.
 * <p>
 * RadioButtonGroup extends {@link buoy.event.EventSource EventSource}.  It generates a
 * {@link buoy.event.SelectionChangedEvent SelectionChangedEvent}
 * whenever the user changes the selected radio button.  Therefore, you can simply add an event
 * link to a RadioButtonGroup, rather than adding one to each radio button independently.
 *
 * @author Peter Eastman
 */

public class RadioButtonGroup extends EventSource
{
  private ArrayList buttons;
  
  /**
   * Create a new RadioButtonGroup.
   */
  
  public RadioButtonGroup()
  {
    buttons = new ArrayList();
  }
  
  /**
   * Add a BRadioButton to this group.  This is called from the BRadioButton constructor.
   */

  void add(BRadioButton button)
  {
    buttons.add(button);
    button.addEventLink(ValueChangedEvent.class, this);
  }
  
  /**
   * Add a BRadioButtonMenuItem to this group.  This is called from the BRadioButtonMenuItem constructor.
   */

  void add(BRadioButtonMenuItem item)
  {
    buttons.add(item);
    item.addEventLink(CommandEvent.class, this);
  }
  
  /**
   * Remove the i'th radio button from this group.
   */
  
  void remove(int i)
  {
    EventSource button = (EventSource) buttons.get(i);
    if (button instanceof BRadioButton)
      button.removeEventLink(ValueChangedEvent.class, this);
    else
      button.removeEventLink(CommandEvent.class, this);
    buttons.remove(i);
  }
  
  /**
   * Get the currently selected radio button, or null if none is selected.
   */
  
  public Object getSelection()
  {
    for (int i = 0; i < buttons.size(); i++)
      if (getState(i))
        return buttons.get(i);
    return null;
  }
  
  /**
   * Select a particular radio button, and deselect all others in the group.
   */
  
  public void setSelection(Object sel)
  {
    for (int i = 0; i < buttons.size(); i++)
    {
      Widget radio = (Widget) buttons.get(i);
      boolean state = (radio == sel);
      if (radio instanceof BRadioButton)
        ((JRadioButton) radio.getComponent()).setSelected(state);
      else
        ((JRadioButtonMenuItem) radio.getComponent()).setSelected(state);
    }
  }
  
  /**
   * Get an Iterator listing all members of the group.
   */
  
  public Iterator getRadioButtons()
  {
    return buttons.iterator();
  }
  
  /**
   * Get the number of members in this group.
   */
  
  public int getRadioButtonCount()
  {
    return buttons.size();
  }
  
  /**
   * Get the i'th radio button in this group.
   */
  
  public Object getRadioButton(int i)
  {
    return buttons.get(i);
  }
  
  /**
   * Get the selection state of the i'th radio button.
   */
  
  private boolean getState(int i)
  {
    Object button = buttons.get(i);
    if (button instanceof BRadioButton)
      return ((BRadioButton) button).getState();
    return ((BRadioButtonMenuItem) button).getState();
  }
  
  /**
   * When the user clicks a radio button, deselect all others and forward the event.
   */
  
  private void processEvent(WidgetEvent ev)
  {
    setSelection(ev.getWidget());
    dispatchEvent(new SelectionChangedEvent(ev.getWidget(), false));
  }
}
