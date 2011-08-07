package buoy.widget;

import buoy.internal.*;

import java.awt.event.*;

/**
 * This class is a JPanel which contains a Widget.  It is used for embedding Buoy-based user interfaces
 * into Swing-based windows.
 */

public class BuoyComponent extends SingleWidgetPanel
{
  /**
   * Create a BuoyComponent containing a Widget.
   */

  public BuoyComponent(Widget widget)
  {
    super(widget);
    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e)
      {
        if (BuoyComponent.this.widget instanceof WidgetContainer)
          ((WidgetContainer) BuoyComponent.this.widget).layoutChildren();
      }
    });
  }

  /**
   * Get the Widget contained in this Component.
   */

  public Widget getWidget()
  {
    return widget;
  }

  /**
   * If the contained Widget is a WidgetContainer, make sure its contents are layed out correctly.
   */

  public void validate()
  {
    super.validate();
    if (widget instanceof WidgetContainer)
      ((WidgetContainer) widget).layoutChildren();
  }
}
