package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;
import buoy.event.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * A BScrollPane is a WidgetContainer with up to five children: an arbitrary "content" Widget that
 * fills most of the BScrollPane, optional "row header" and "column header" Widgets along the left
 * and top edges, respectively, and optional BScrollBars along the right and bottom edges.  It
 * displays only a portion of the content, row header, and column header Widgets, and allows the user
 * to scroll through them by means of the two BScrollBars.
 * <p>
 * If the BScrollPane does not have a horizontal and/or vertical scrollbar, then by default it will
 * force the corresponding dimension of the content and header Widgets to exactly match the size of the
 * visible area.  For example, if the content is a BTextArea with word wrap enabled, you would normally
 * only have a vertical scrollbar.  The width of the BTextArea should then be forced to exactly
 * match the width of the visible area, so that words will wrap at the correct place.  You can
 * override this behavior by calling setForceWidth() and setForceHeight().  For example, you might
 * want to control the scroll position through some external means, in which case the BScrollPane
 * would not provide scrollbars but should still allow the content Widget to be its preferred size.
 *
 * @author Peter Eastman
 */

public class BScrollPane extends WidgetContainer
{
  private ContentViewport contentPort;
  private JViewport rowHeaderPort, colHeaderPort;
  private Widget content, rowHeader, colHeader;
  private BScrollBar hscroll, vscroll;
  private ScrollbarPolicy hPolicy, vPolicy;
  private Dimension preferredViewSize, prefSize, minSize;
  private boolean forceWidth, forceHeight;
  
  public static final ScrollbarPolicy SCROLLBAR_NEVER = new ScrollbarPolicy();
  public static final ScrollbarPolicy SCROLLBAR_AS_NEEDED = new ScrollbarPolicy();
  public static final ScrollbarPolicy SCROLLBAR_ALWAYS = new ScrollbarPolicy();

  private static final boolean IS_MACINTOSH;

  static
  {
    WidgetEncoder.setPersistenceDelegate(ScrollbarPolicy.class, new StaticFieldDelegate(BScrollPane.class));
    String os = System.getProperty("os.name", "").toLowerCase();
    IS_MACINTOSH = os.startsWith("mac os x");
  }
  
  /**
   * Create a new BScrollPane with no content or header Widgets.  The horizontal and vertical
   * scrollbar policies default to SCROLLBAR_AS_NEEDED.
   */
  
  public BScrollPane()
  {
    this(SCROLLBAR_AS_NEEDED, SCROLLBAR_AS_NEEDED);
  }

  /**
   * Create a new BScrollPane with the specified Widget as its content.  The horizontal and vertical
   * scrollbar policies default to SCROLLBAR_AS_NEEDED.
   *
   * @param contentWidget    the Widget to use as the content of the BScrollPane
   */
  
  public BScrollPane(Widget contentWidget)
  {
    this(SCROLLBAR_AS_NEEDED, SCROLLBAR_AS_NEEDED);
    setContent(contentWidget);
  }

  /**
   * Create a new BScrollPane with no content or header Widgets.
   *
   * @param horizontalPolicy    specifies when the horizontal scrollbar should be displayed.  This should
   *                            be equal to SCROLLBAR_ALWAYS, SCROLLBAR_AS_NEEDED, or SCROLLBAR_NEVER.
   * @param verticalPolicy      specifies when the vertical scrollbar should be displayed.  This should
   *                            be equal to SCROLLBAR_ALWAYS, SCROLLBAR_AS_NEEDED, or SCROLLBAR_NEVER.
   */
  
  public BScrollPane(ScrollbarPolicy horizontalPolicy, ScrollbarPolicy verticalPolicy)
  {
    JScrollPane panel = createComponent();
    component = panel;
    hPolicy = horizontalPolicy;
    vPolicy = verticalPolicy;
    ChangeListener scrollListener = new ChangeListener() {
      public void stateChanged(ChangeEvent e)
      {
        Point pos = contentPort.getViewPosition();
        if (hPolicy != SCROLLBAR_NEVER)
          pos.x = hscroll.getValue();
        if (vPolicy != SCROLLBAR_NEVER)
          pos.y = vscroll.getValue();
        contentPort.setViewPositionInternal(pos);
        rowHeaderPort.setViewPosition(new Point(0, pos.y));
        colHeaderPort.setViewPosition(new Point(pos.x, 0));
      }
    };
    MouseWheelListener wheelListener = new MouseWheelListener() {
      public void mouseWheelMoved(MouseWheelEvent ev)
      {
        BScrollBar bar = null;
        if (ev.getSource() == vscroll.getComponent())
          bar = vscroll;
        else if (ev.getSource() == hscroll.getComponent())
          bar = hscroll;
        else if (vscroll.isVisible() && vscroll.isEnabled() && vscroll.getExtent() < vscroll.getMaximum())
          bar = vscroll;
        else if (hscroll.isVisible() && hscroll.isEnabled() && hscroll.getExtent() < hscroll.getMaximum())
          bar = hscroll;
        else
          return;
        int direction = (ev.getWheelRotation() > 0 ? 1 : -1);
        if (ev.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL)
          bar.setValue(bar.getValue()+bar.getBlockIncrement(direction)*ev.getWheelRotation());
        else
          bar.setValue(bar.getValue()+bar.getUnitIncrement(direction)*ev.getScrollAmount()*direction);
      }
    };
    vscroll = new ScrollPaneScrollBar(0, 1, 0, 100, BScrollBar.VERTICAL);
    panel.add(vscroll.getComponent());
    setAsParent(vscroll);
    vscroll.getComponent().getModel().addChangeListener(scrollListener);
    vscroll.getComponent().addMouseWheelListener(wheelListener);
    hscroll = new ScrollPaneScrollBar(0, 1, 0, 100, BScrollBar.HORIZONTAL);
    panel.add(hscroll.getComponent());
    setAsParent(hscroll);
    hscroll.getComponent().getModel().addChangeListener(scrollListener);
    hscroll.getComponent().addMouseWheelListener(wheelListener);
    panel.setViewport(contentPort = new ContentViewport());
    panel.setRowHeader(rowHeaderPort = new JViewport());
    panel.setColumnHeader(colHeaderPort = new JViewport());
    rowHeaderPort.setLayout(null);
    colHeaderPort.setLayout(null);
    rowHeaderPort.setOpaque(false);
    colHeaderPort.setOpaque(false);
    forceWidth = (hPolicy == SCROLLBAR_NEVER);
    forceHeight = (vPolicy == SCROLLBAR_NEVER);
    contentPort.addMouseWheelListener(wheelListener);
  }
  
  /**
   * Create a new BScrollPane with the specified Widget as its content.
   *
   * @param contentWidget       the Widget to use as the content of the BScrollPane
   * @param horizontalPolicy    specifies when the horizontal scrollbar should be displayed.  This should
   *                            be equal to SCROLLBAR_ALWAYS, SCROLLBAR_AS_NEEDED, or SCROLLBAR_NEVER.
   * @param verticalPolicy      specifies when the vertical scrollbar should be displayed.  This should
   *                            be equal to SCROLLBAR_ALWAYS, SCROLLBAR_AS_NEEDED, or SCROLLBAR_NEVER.
   */
  
  public BScrollPane(Widget contentWidget, ScrollbarPolicy horizontalPolicy, ScrollbarPolicy verticalPolicy)
  {
    this(horizontalPolicy, verticalPolicy);
    setContent(contentWidget);
  }

  /**
   * Create the JScrollPane which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   */

  protected JScrollPane createComponent()
  {
    return new ScrollPaneComponent();
  }

  public JScrollPane getComponent()
  {
    return (JScrollPane) component;
  }

  /**
   * Get the content Widget.
   */
  
  public Widget getContent()
  {
    return content;
  }
  
  /**
   * Set the content Widget.
   */
  
  public void setContent(Widget widget)
  {
    if (content != null)
      removeAsParent(content);
    content = widget;
    if (content == null)
      contentPort.setView(null);
    else
    {
      setAsParent(content);
      contentPort.setView(content.getComponent());
    }
    invalidateSize();
  }
  
  /**
   * Get the row header Widget.
   */
  
  public Widget getRowHeader()
  {
    return rowHeader;
  }
  
  /**
   * Set the row header Widget.
   */
  
  public void setRowHeader(Widget widget)
  {
    if (rowHeader != null)
      removeAsParent(rowHeader);
    rowHeader = widget;
    if (rowHeader != null)
      setAsParent(rowHeader);
    rowHeaderPort.setView(rowHeader == null ? null : rowHeader.getComponent());
    invalidateSize();
  }
  
  /**
   * Get the column header Widget.
   */
  
  public Widget getColHeader()
  {
    return colHeader;
  }
  
  /**
   * Set the column header Widget.
   */
  
  public void setColHeader(Widget widget)
  {
    if (colHeader != null)
      removeAsParent(colHeader);
    colHeader = widget;
    if (colHeader != null)
      setAsParent(colHeader);
    colHeaderPort.setView(colHeader == null ? null : colHeader.getComponent());
    invalidateSize();
  }
  
  /**
   * Get the horizontal BScrollBar.
   */
  
  public BScrollBar getHorizontalScrollBar()
  {
    return hscroll;
  }
  
  /**
   * Get the vertical BScrollBar.
   */
  
  public BScrollBar getVerticalScrollBar()
  {
    return vscroll;
  }
  
  /**
   * Get the horizontal scrollbar policy.  This will be either SCROLLBAR_ALWAYS, SCROLLBAR_AS_NEEDED,
   * or SCROLLBAR_NEVER.
   */
  
  public ScrollbarPolicy getHorizontalScrollbarPolicy()
  {
    return hPolicy;
  }
  
  /**
   * Set the horizontal scrollbar policy.  This should be either SCROLLBAR_ALWAYS, SCROLLBAR_AS_NEEDED,
   * or SCROLLBAR_NEVER.
   */
  
  public void setHorizontalScrollbarPolicy(ScrollbarPolicy policy)
  {
    hPolicy = policy;
    invalidateSize();
    layoutChildren();
  }

  /**
   * Get the vertical scrollbar policy.  This will be either SCROLLBAR_ALWAYS, SCROLLBAR_AS_NEEDED,
   * or SCROLLBAR_NEVER.
   */
  
  public ScrollbarPolicy getVerticalScrollbarPolicy()
  {
    return vPolicy;
  }
  
  /**
   * Set the vertical scrollbar policy.  This should be either SCROLLBAR_ALWAYS, SCROLLBAR_AS_NEEDED,
   * or SCROLLBAR_NEVER.
   */
  
  public void setVerticalScrollbarPolicy(ScrollbarPolicy policy)
  {
    vPolicy = policy;
    invalidateSize();
    layoutChildren();
  }
  
  /**
   * Get the preferred size for the content's view area.  This may be null, in which case the preferred
   * view size is equal to the preferred size of the content Widget.
   */
  
  public Dimension getPreferredViewSize()
  {
    return preferredViewSize;
  }
  
  /**
   * Set the preferred size for the content's view area.  This may be null, in which case the preferred
   * view size is equal to the preferred size of the content Widget.
   */
  
  public void setPreferredViewSize(Dimension size)
  {
    preferredViewSize = size;
    invalidateSize();
  }
  
  /**
   * Get the current size of the content's view area.
   */
  
  public Dimension getViewSize()
  {
    return contentPort.getSize();
  }

  /**
   * Get whether the BScrollPane should force the width of the content and column header Widgets to
   * exactly match the width of the visible area.  By default, this is true if the horizontal scrollbar
   * policy was initialized to SCROLLBAR_NEVER, and false otherwise.
   * <p>
   * Note that even if this option is enabled, the BScrollPane will never make the content Widget
   * smaller than its minimum size or larger than its maximum size.
   */
  
  public boolean getForceWidth()
  {
    return forceWidth;
  }

  /**
   * Set whether the BScrollPane should force the width of the content and column header Widgets to
   * exactly match the width of the visible area.  By default, this is true if the horizontal scrollbar
   * policy was initialized to SCROLLBAR_NEVER, and false otherwise.
   * <p>
   * Note that even if this option is enabled, the BScrollPane will never make the content Widget
   * smaller than its minimum size or larger than its maximum size.
   */
  
  public void setForceWidth(boolean force)
  {
    forceWidth = force;
  }

  /**
   * Get whether the BScrollPane should force the height of the content and row header Widgets to
   * exactly match the height of the visible area.  By default, this is true if the vertical scrollbar
   * policy was initialized to SCROLLBAR_NEVER, and false otherwise.
   * <p>
   * Note that even if this option is enabled, the BScrollPane will never make the content Widget
   * smaller than its minimum size or larger than its maximum size.
   */
  
  public boolean getForceHeight()
  {
    return forceHeight;
  }

  /**
   * Set whether the BScrollPane should force the height of the content and row header Widgets to
   * exactly match the height of the visible area.  By default, this is true if the vertical scrollbar
   * policy was initialized to SCROLLBAR_NEVER, and false otherwise.
   * <p>
   * Note that even if this option is enabled, the BScrollPane will never make the content Widget
   * smaller than its minimum size or larger than its maximum size.
   */
  
  public void setForceHeight(boolean force)
  {
    forceHeight = force;
  }

  /**
   * Get the number of children in this container.
   */
  
  public int getChildCount()
  {
    int count = 2;
    if (content != null)
      count++;
    if (rowHeader != null)
      count++;
    if (colHeader != null)
      count++;
    return count;
  }
  
  /**
   * Get a Collection containing all child Widgets of this container.
   */
  
  public Collection<Widget> getChildren()
  {
    ArrayList<Widget> ls = new ArrayList<Widget>(5);
    if (content != null)
      ls.add(content);
    if (rowHeader != null)
      ls.add(rowHeader);
    if (colHeader != null)
      ls.add(colHeader);
    ls.add(hscroll);
    ls.add(vscroll);
    return ls;
  }
  
  /**
   * Remove a child Widget from this container.  The scrollbars are built into the scroll pane and
   * may never be removed, although they can be hidden by setting the appropriate scrollbar policy
   * to SCROLLBAR_NEVER.
   */
  
  public void remove(Widget widget)
  {
    if (content == widget)
      setContent(null);
    if (rowHeader == widget)
      setRowHeader(null);
    if (colHeader == widget)
      setColHeader(null);
    invalidateSize();
  }
  
  /**
   * Remove the content, row header, and column header Widgets from this container.  The scrollbars
   * are built into the scroll pane and may never be removed, although they can be hidden by setting
   * the appropriate scrollbar policy to SCROLLBAR_NEVER.
   */
  
  public void removeAll()
  {
    setContent(null);
    setRowHeader(null);
    setColHeader(null);
  }
  
  /**
   * Layout the child Widgets.  This may be invoked whenever something has changed (the size of this
   * WidgetContainer, the preferred size of one of its children, etc.) that causes the layout to no
   * longer be correct.  If a child is itself a WidgetContainer, its layoutChildren() method will be
   * called in turn.
   */
  
  public void layoutChildren()
  {
    int topMargin = 0, leftMargin = 0, bottomMargin = 0, rightMargin = 0;
    Dimension colHeaderSize = (colHeader == null ? null : colHeader.getPreferredSize());
    Dimension rowHeaderSize = (rowHeader == null ? null : rowHeader.getPreferredSize());
    Dimension hScrollSize = hscroll.getPreferredSize();
    Dimension vScrollSize = vscroll.getPreferredSize();
    Dimension contentSize = (content == null ? new Dimension() : content.getPreferredSize());
    Rectangle bounds = getBounds();
    
    // Find the margins.
    
    if (colHeaderSize != null)
      topMargin = colHeaderSize.height;
    if (rowHeaderSize != null)
      leftMargin = rowHeaderSize.width;
    boolean hasHScroll = (hPolicy == SCROLLBAR_ALWAYS || (hPolicy == SCROLLBAR_AS_NEEDED && bounds.width-leftMargin-vScrollSize.width < contentSize.width));
    boolean hasVScroll = (vPolicy == SCROLLBAR_ALWAYS || (vPolicy == SCROLLBAR_AS_NEEDED && bounds.height-topMargin-hScrollSize.height < contentSize.height));
    if (hasHScroll)
      bottomMargin = hScrollSize.height;
    if (hasVScroll)
      rightMargin = vScrollSize.width;
    if (hPolicy == SCROLLBAR_AS_NEEDED && vPolicy == SCROLLBAR_AS_NEEDED && bounds.width-leftMargin >= contentSize.width && bounds.height-topMargin >= contentSize.height)
    {
      hasHScroll = false;
      hasVScroll = false;
      bottomMargin = 0;
      rightMargin = 0;
    }
    Rectangle viewBounds = new Rectangle(leftMargin, topMargin, bounds.width-leftMargin-rightMargin, bounds.height-topMargin-bottomMargin);
    
    // Find the size to force the content Widget to (if forceWidth or forceHeight is enabled).
    
    Dimension forceSize = new Dimension(viewBounds.width, viewBounds.height);
    if (content != null)
    {
      Dimension minSize = content.getMinimumSize();
      Dimension maxSize = content.getMaximumSize();
      forceSize.width = Math.max(minSize.width, Math.min(maxSize.width, forceSize.width));
      forceSize.height = Math.max(minSize.height, Math.min(maxSize.height, forceSize.height));
    }
    
    // Set the size of the headers.

    colHeaderPort.setBounds(new Rectangle(leftMargin, 0, viewBounds.width, topMargin));
    if (colHeader != null)
    {
      Dimension size = new Dimension(colHeaderSize);
      if (forceWidth)
        size.width = forceSize.width;
      colHeader.getComponent().setSize(size);
    }
    rowHeaderPort.setBounds(new Rectangle(0, topMargin, leftMargin, viewBounds.height));
    if (rowHeader != null)
    {
      Dimension size = new Dimension(rowHeaderSize);
      if (forceHeight)
        size.height = forceSize.height;
      rowHeader.getComponent().setSize(size);
    }
    
    // Set the size of the content Widget.

    contentPort.setBounds(viewBounds);
    if (content != null)
    {
      Dimension size = new Dimension(contentSize);
      if (forceWidth)
        size.width = forceSize.width;
      if (forceHeight)
        size.height = forceSize.height;
      content.getComponent().setSize(size);
    }

    // On a Macintosh, if the scroll pane is in the lower right corner of a resizable window,
    // leave room for the grow box.

    int hScrollLength = viewBounds.width;
    int vScrollLength = viewBounds.height;
    if (IS_MACINTOSH && hasHScroll != hasVScroll)
    {
      Widget parent = getParent();
      while (parent != null && !(parent instanceof WindowWidget))
        parent = parent.getParent();
      if ((parent instanceof BFrame && ((BFrame) parent).isResizable()) ||
          (parent instanceof BDialog && ((BDialog) parent).isResizable()))
      {
        Rectangle parentBounds = parent.getBounds();
        Point offset = SwingUtilities.convertPoint(getComponent(), 0, 0, parent.getComponent());
        if (bounds.x+bounds.width+offset.x == parentBounds.width &&
            bounds.y+bounds.height+offset.y == parentBounds.height)
        {
          if (!hasHScroll)
            vScrollLength -= hScrollSize.height;
          if (!hasVScroll)
            hScrollLength -= vScrollSize.width;
        }
      }
    }
    
    // Set up the scrollbars.
    
    hscroll.getComponent().setBounds(new Rectangle(leftMargin, viewBounds.y+viewBounds.height, hScrollLength, bottomMargin));
    if (content == null)
      hscroll.setEnabled(false);
    else
    {
      hscroll.setEnabled(true);
      int width = content.getComponent().getWidth();
      hscroll.setMaximum(width);
      if (hscroll.getValue()+viewBounds.width > width)
        hscroll.setValue(width-viewBounds.width);
    }
    hscroll.setExtent(viewBounds.width);
    vscroll.getComponent().setBounds(new Rectangle(viewBounds.x+viewBounds.width, topMargin, rightMargin, vScrollLength));
    if (content == null)
      vscroll.setEnabled(false);
    else
    {
      vscroll.setEnabled(true);
      int height = content.getComponent().getHeight();
      vscroll.setMaximum(height);
      if (vscroll.getValue()+viewBounds.height > height)
        vscroll.setValue(height-viewBounds.height);
    }
    vscroll.setExtent(viewBounds.height);
    
    // Layout any child containers.
    
    if (content instanceof WidgetContainer)
      ((WidgetContainer) content).layoutChildren();
    if (colHeader instanceof WidgetContainer)
      ((WidgetContainer) colHeader).layoutChildren();
    if (rowHeader instanceof WidgetContainer)
      ((WidgetContainer) rowHeader).layoutChildren();
  }

  /**
   * Get the smallest size at which this Widget can reasonably be drawn.  When a WidgetContainer lays out
   * its contents, it will attempt never to make this Widget smaller than its minimum size.
   */
  
  public Dimension getMinimumSize()
  {
    if (minSize == null)
    {
      // Add the row and column headers.
      
      minSize = new Dimension();
      if (colHeader != null)
        minSize.height += colHeader.getMinimumSize().height;
      if (rowHeader != null)
        minSize.width += rowHeader.getMinimumSize().width;
      
      // Add the scrollbars, if appropriate.
      
      Dimension contentDim = (content == null ? new Dimension() : content.getMinimumSize());
      if (hPolicy == SCROLLBAR_ALWAYS || (hPolicy == SCROLLBAR_AS_NEEDED && contentDim.width > 0))
      {
        Dimension hScrollDim = hscroll.getMinimumSize();
        minSize.width += hScrollDim.width;
        minSize.height += hScrollDim.height;
      }
      if (vPolicy == SCROLLBAR_ALWAYS || (vPolicy == SCROLLBAR_AS_NEEDED && contentDim.height > 0))
      {
        Dimension vScrollDim = vscroll.getMinimumSize();
        minSize.height += vScrollDim.height;
        minSize.width += vScrollDim.width;
      }
    }
    return new Dimension(minSize);
  }

  /**
   * Get the preferred size at which this Widget will look best.  When a WidgetContainer lays out
   * its contents, it will attempt to make this Widget as close as possible to its preferred size.
   */
  
  public Dimension getPreferredSize()
  {
    if (prefSize == null)
    {
      // Find the preferred size for the content viewport.
      
      Dimension contentPrefSize = (content == null ? new Dimension() : content.getPreferredSize());
      if (content != null && content.getComponent() instanceof Scrollable)
      {
        Dimension prefScrollSize = ((Scrollable) content.getComponent()).getPreferredScrollableViewportSize();
        contentPrefSize = new Dimension(Math.min(contentPrefSize.width, prefScrollSize.width), Math.min(contentPrefSize.height, prefScrollSize.height));
      }
      if (preferredViewSize != null)
        prefSize = new Dimension(preferredViewSize);
      else
        prefSize = new Dimension(contentPrefSize);
      
      // Add the row and column headers.
      
      if (colHeader != null)
      {
        Dimension colHeaderDim = colHeader.getPreferredSize();
        if (colHeaderDim.width > prefSize.width)
          prefSize.width = colHeaderDim.width;
        prefSize.height += colHeaderDim.height;
      }
      if (rowHeader != null)
      {
        Dimension rowHeaderDim = rowHeader.getPreferredSize();
        if (rowHeaderDim.height > prefSize.height)
          prefSize.height = rowHeaderDim.height;
        prefSize.width += rowHeaderDim.width;
      }
      
      // Add the scrollbars, if appropriate.
      
      if (hPolicy == SCROLLBAR_ALWAYS || (hPolicy == SCROLLBAR_AS_NEEDED && prefSize.width < contentPrefSize.width))
      {
        Dimension hScrollDim = hscroll.getPreferredSize();
        if (hScrollDim.width > prefSize.width)
          prefSize.width = hScrollDim.width;
        prefSize.height += hScrollDim.height;
      }
      if (vPolicy == SCROLLBAR_ALWAYS || (vPolicy == SCROLLBAR_AS_NEEDED && prefSize.height < contentPrefSize.height))
      {
        Dimension vScrollDim = vscroll.getPreferredSize();
        if (vScrollDim.height > prefSize.height)
          prefSize.height = vScrollDim.height;
        prefSize.width += vScrollDim.width;
      }
    }
    return new Dimension(prefSize);
  }

  /**
   * Set the background color of this Widget.  If this is set to null, the Widget will use the background
   * color of its parent WidgetContainer.
   */
  
  public void setBackground(Color background)
  {
    super.setBackground(background);
    contentPort.setBackground(background);
    rowHeaderPort.setBackground(background);
    colHeaderPort.setBackground(background);
  }

  /**
   * Discard the cached sizes when any child's size changes.
   */
  
  protected void invalidateSize()
  {
    prefSize = minSize = null;
    super.invalidateSize();
  }

  /**
   * This is a JScrollPane subclass which is the component for the BScrollPane.  It is nearly identical
   * to WidgetContainerPanel, except that it extends JScrollPane instead of JPanel.
   */

  private class ScrollPaneComponent extends JScrollPane
  {
    private ScrollPaneComponent()
    {
      setLayout(null);
    }

    public void paintComponent(Graphics g)
    {
      if (BScrollPane.this.isOpaque())
      {
        Dimension size = getSize();
        g.setColor(getBackground());
        g.fillRect(0, 0, size.width, size.height);
        g.setColor(getForeground());
      }
      BScrollPane.this.dispatchEvent(new RepaintEvent(BScrollPane.this, (Graphics2D) g));
    }

    public boolean isOpaque()
    {
      return BScrollPane.this.isOpaque();
    }
  }
  
  /**
   * This is a BScrollBar subclass which is used for the scrollbars of the BScrollPane.  If the content
   * Widget has a Component which implements Scrollable, that Component is used for setting the unit
   * and block increments of the scrollbars.
   */
  
  private class ScrollPaneScrollBar extends BScrollBar
  {
    public ScrollPaneScrollBar(int value, int extent, int minimum, int maximum, BScrollBar.Orientation orientation)
    {
      super(value, extent, minimum, maximum, orientation);
    }
    
    public int getUnitIncrement(int direction)
    {
      if (content != null && content.getComponent() instanceof Scrollable)
        return ((Scrollable) content.getComponent()).getScrollableUnitIncrement(contentPort.getViewRect(), getOrientation().value, direction);
      return super.getUnitIncrement(direction);
    }

    public int getBlockIncrement(int direction)
    {
      if (content != null && content.getComponent() instanceof Scrollable)
        return ((Scrollable) content.getComponent()).getScrollableBlockIncrement(contentPort.getViewRect(), getOrientation().value, direction);
      return super.getBlockIncrement(direction);
    }
  }
  
  /**
   * This inner class is the JViewport that holds the content Widget.
   */
  
  private class ContentViewport extends JViewport
  {
    public ContentViewport()
    {
    }
    
    public void setViewPosition(Point p)
    {
      hscroll.setValue(p.x);
      vscroll.setValue(p.y);
    }
    
    public void setViewPositionInternal(Point p)
    {
      super.setViewPosition(p);
    }
    
    public void doLayout()
    {
      layoutChildren();
    }
  }
  
  /**
   * This inner class represents a scrollbar policy for the horizontal or vertical scrollbar.
   */
  
  public static class ScrollbarPolicy
  {
    private ScrollbarPolicy()
    {
    }
  }
}