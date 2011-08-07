package buoy.event;

import buoy.widget.*;
import java.net.*;
import java.util.*;
import javax.swing.event.*;

/**
 * A DocumentLinkEvents is generated when the user clicks on a hyperlink inside a BDocumentViewer.
 * It is a simple wrapper around a javax.swing.event.HyperlinkEvent object.
 *
 * @author Peter Eastman
 */

public class DocumentLinkEvent extends EventObject implements WidgetEvent
{
  private Widget widget;
  private HyperlinkEvent event;

  /**
   * Create a DocumentLinkEvent.
   *
   * @param widget     the Widget containing the link that was selected
   * @param event      the original HyperlinkEvent
   */
  
  public DocumentLinkEvent(Widget widget, HyperlinkEvent event)
  {
    super(widget);
    this.widget = widget;
    this.event = event;
  }

  /**
   * Get the Widget which generated this event.
   */
  
  public Widget getWidget()
  {
    return widget;
  }
  
  /**
   * Get the URL of the link which was clicked.
   */
  
  public URL getURL()
  {
    return event.getURL();
  }
  
  /**
   * Get a description of the link which was clicked.
   */
  
  public String getDescription()
  {
    return event.getDescription();
  }
  
  /**
   * Get the original HyperlinkEvent.
   */
  
  public HyperlinkEvent getEvent()
  {
    return event;
  }
}
