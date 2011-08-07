package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;

import javax.swing.*;

/**
 * BStandardDialog is used for displaying a variety of "standard" modal dialogs which display messages
 * or ask for simple types of input.  Most platforms define a standardized appearance for such dialogs
 * (the layout, the use of particular icons, etc.), and this class will automatically create dialogs
 * which look correct for the current platform.
 * <p>
 * BStandardDialog does not extend Widget.  It is a fully self contained user interface element.
 * You simply create the BStandardDialog and call an appropriate method to display it.  That method
 * blocks until the dialog has been dismissed, then returns whatever input was entered by the user.
 * <p>
 * There are three different methods to show a dialog, which correspond to the three major types of
 * dialog which can be shown:
 * <ul>
 * <li>{@link buoy.widget.BStandardDialog#showMessageDialog showMessageDialog()} simply displays a
 * message to the user, and blocks until they click the "OK" button.</li>
 * <li>{@link buoy.widget.BStandardDialog#showOptionDialog showOptionDialog()} displays a message
 * and offers two or three buttons for the user to choose from.  It blocks until the user clicks
 * one of the buttons, then returns the index of the button they selected.</li>
 * <li>{@link buoy.widget.BStandardDialog#showInputDialog showInputDialog()} displays a message
 * and gives the user space to enter a value.  The value may be unrestricted, in which case they
 * are given a text field to type the value, or it may be restricted to a list of allowed values,
 * in which case they are given a list or combo box from which to select a value.  It blocks until
 * the user clicks the "OK" or "Cancel" button, then returns the value they entered.</li>
 * </ul>
 * BStandardDialog allows you to specify a message to be displayed in the dialog.  Usually this is
 * a String, but it can be other types of object as well.
 * <ul>
 * <li>If it is an Icon, that icon will be displayed.</li>
 * <li>If it is a Widget, that Widget will be added to the dialog.</li>
 * <li>If it is an array, each element of the array will be displayed on a separate line.  This
 * allows you to display multiple lines of text, or a set of several Widgets.</li>
 * <li>All other objects are converted to Strings by calling <code>toString()</code> on them.</li>
 * </ul>
 * BStandardDialog allows you to specify a style, which may be any of the following values:
 * ERROR, INFORMATION, WARNING, QUESTION, or PLAIN.  This determines which of the platform-specific
 * standard icons will appear in the dialog.
 *
 * @author Peter Eastman
 */

public class BStandardDialog
{
  private Object message;
  private String title;
  private Style style;
  
  public static final Style ERROR = new Style(JOptionPane.ERROR_MESSAGE);
  public static final Style INFORMATION = new Style(JOptionPane.INFORMATION_MESSAGE);
  public static final Style WARNING = new Style(JOptionPane.WARNING_MESSAGE);
  public static final Style QUESTION = new Style(JOptionPane.QUESTION_MESSAGE);
  public static final Style PLAIN = new Style(JOptionPane.PLAIN_MESSAGE);

  static
  {
    WidgetEncoder.setPersistenceDelegate(Style.class, new StaticFieldDelegate(Style.class));
  }

  /**
   * Create a new BStandardDialog with no message whose style is PLAIN.
   */
  
  public BStandardDialog()
  {
    this("", "", PLAIN);
  }
  
  /**
   * Create a new BStandardDialog.
   *
   * @param title     the title to display on the dialog
   * @param message   the message to display inside the dialog
   * @param style     the style of the dialog to display
   */
  
  public BStandardDialog(String title, Object message, Style style)
  {
    setTitle(title);
    setMessage(message);
    setStyle(style);
  }
  
  /**
   * Get the title displayed on the dialog.
   */
  
  public String getTitle()
  {
    return title;
  }
  
  /**
   * Set the title displayed on the dialog.
   */
  
  public void setTitle(String title)
  {
    this.title = title;
  }

  /**
   * Get the message displayed in the dialog.
   */
  
  public Object getMessage()
  {
    return message;
  }
  
  /**
   * Set the message displayed in the dialog.
   */
  
  public void setMessage(Object message)
  {
    this.message = message;
  }
  
  /**
   * Get the style of the dialog.
   */
  
  public Style getStyle()
  {
    return style;
  }
  
  /**
   * Set the style of the dialog.
   */
  
  public void setStyle(Style style)
  {
    this.style = style;
  }
    
  /**
   * Show a dialog which contains the message.  This method blocks until the user clicks "OK",
   * dismissing the dialog.
   *
   * @param parent    the dialog's parent Widget (usually a WindowWidget).  This may be null.
   */
  
  public void showMessageDialog(Widget parent)
  {
    JOptionPane.showMessageDialog(parent == null ? null : parent.getComponent(), buildMessage(message), title, style.value);
  }
    
  /**
   * Show a dialog which contains the message and two or three buttons to choose from.  This method
   * blocks until the user clicks one of the buttons, dismissing the dialog.
   *
   * @param parent       the dialog's parent Widget (usually a WindowWidget).  This may be null.
   * @param options      the list of labels to display on the buttons.  This must be of length 2 or 3.
   * @param defaultVal   the option which should be shown as the default value
   * @return the index of the button selected by the user
   */
  
  public int showOptionDialog(Widget parent, String options[], String defaultVal)
  {
    if (options.length != 2 && options.length != 3)
      throw new IllegalArgumentException("Number of options must be 2 or 3");
    int response = JOptionPane.showOptionDialog(parent == null ? null : parent.getComponent(), buildMessage(message), title,
        (options.length == 2 ? JOptionPane.YES_NO_OPTION : JOptionPane.YES_NO_CANCEL_OPTION),
        style.value, null, options, defaultVal);
    if (response == JOptionPane.YES_OPTION)
      return 0;
    if (response == JOptionPane.NO_OPTION)
      return 1;
    return 2;
  }
    
  /**
   * Show a dialog which contains the message and space for the user to enter value.  The interface
   * for entering the value is platform specific, but it will usually be a text field, list, or
   * combo box.  This method blocks until the user clicks "OK" or "Cancel", dismissing the dialog.
   *
   * @param parent       the dialog's parent Widget (usually a WindowWidget).  This may be null.
   * @param options      the list of allowed values.  This may be null, in which case the user is
   *                     free to enter any value.
   * @param defaultVal   the default value when the dialog first appears
   * @return the value entered by the user, or null if they clicked "Cancel"
   */
  
  public String showInputDialog(Widget parent, String options[], String defaultVal)
  {
    return (String) JOptionPane.showInputDialog(parent == null ? null : parent.getComponent(), buildMessage(message), title,
        style.value, null, options, defaultVal);
  }
  
  /**
   * Build the "message" object that should be passed to JOptionPane.  If the message is a Widget,
   * it is replaced by its Component.
   */
  
  private static Object buildMessage(Object message)
  {
    if (message.getClass().isArray())
    {
      Object msg[] = (Object []) message;
      Object a[] = new Object [msg.length];
      for (int i = 0; i < msg.length; i++)
        a[i] = buildMessage(msg[i]);
      return a;
    }
    if (message instanceof Widget)
    {
      Widget widget = (Widget) message;
      if (widget.getParent() != null)
        widget.getParent().remove(widget);
      return new BuoyComponent(widget);
    }
    return message;
  }
  
  /**
   * This inner class represents a style for the dialog.
   */
  
  public static class Style
  {
    public int value;
    
    private Style(int value)
    {
      this.value = value;
    }
  }
}
