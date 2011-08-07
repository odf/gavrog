package buoy.internal;

import buoy.widget.*;
import buoy.event.*;
import java.awt.*;
import javax.swing.JPanel;

/**
 * This is a JPanel subclass, which is used as the Component for many different WidgetContainers.  When
 * paintComponent() is called, it optionally fills itself with its background color then sends out a
 * RepaintEvent.
 *
 * @author Peter Eastman
 */

public class WidgetContainerPanel extends JPanel
{
  private WidgetContainer container;
  
  /**
   * Create a new WidgetContainerPanel.
   *
   * @param container    the WidgetContainer this will be the component for
   */
  
  public WidgetContainerPanel(WidgetContainer container)
  {
    this.container = container;
    setLayout(null);
  }

  /**
   * Optionally fill the component with its background color, then send out a RepaintEvent.
   */

  public void paintComponent(Graphics g)
  {
    if (container.isOpaque())
    {
      Dimension size = getSize();
      g.setColor(getBackground());
      g.fillRect(0, 0, size.width, size.height);
      g.setColor(getForeground());
    }
    container.dispatchEvent(new RepaintEvent(container, (Graphics2D) g));
  }
  
  /**
   * This component is opaque if its WidgetContainer is set to be opaque.
   */
  
  public boolean isOpaque()
  {
    return container.isOpaque();
  }
}