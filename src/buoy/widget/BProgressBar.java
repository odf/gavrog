package buoy.widget;

import buoy.xml.*;
import buoy.xml.delegate.*;
import javax.swing.*;

/**
 * BProgressBar is a Widget that displays the status of some operation.  It is a horizontal or vertical
 * bar that gradually fills up to indicate what fraction of the operation is complete.  It optionally
 * can also display a line of text describing the current state of the operation.
 * <p>
 * You can specify minimum and maximum progress values for the bar, to reflect the number of steps in
 * the operation being monitored.  When the current progress value is equal to the minimum value, the
 * bar is shown completely empty.  When it is equal to the maximum value, the bar is shown completely
 * full.
 * <p>
 * If you do not know how long an operation will take, the progress bar can be put into an
 * "indeterminate" state.  This causes the entire bar to animate continuously to show that work
 * is being done.
 *
 * @author Peter Eastman
 */

public class BProgressBar extends Widget
{
  public static final Orientation HORIZONTAL = new Orientation(JProgressBar.HORIZONTAL);
  public static final Orientation VERTICAL = new Orientation(JProgressBar.VERTICAL);

  static
  {
    WidgetEncoder.setPersistenceDelegate(Orientation.class, new StaticFieldDelegate(BProgressBar.class));
  }
  
  /**
   * Create a horizontal BProgressBar which does not display text.  The minimum and maximum progress
   * values are 0 and 100, respectively.
   */
  
  public BProgressBar()
  {
    this(HORIZONTAL, 0, 100);
  }
  
  /**
   * Create a horizontal BProgressBar which does not display text.  The initial progress value is
   * set to min.
   *
   * @param min      the minimum value on the progress bar
   * @param max      the maximum value on the progress bar
   */
  
  public BProgressBar(int min, int max)
  {
    this(HORIZONTAL, min, max);
  }

  /**
   * Create a new BProgressBar.  The initial progress value is set to min.
   *
   * @param orient     the progress bar orientation (HORIZONTAL or VERTICAL)
   * @param min        the minimum value on the progress bar
   * @param max        the maximum value on the progress bar
   */
  
  public BProgressBar(Orientation orient, int min, int max)
  {
    component = createComponent();
    setOrientation(orient);
    setMinimum(min);
    setMaximum(max);
  }

  /**
   * Create the JProgressBar which serves as this Widget's Component.  This method is protected so that
   * subclasses can override it.
   */
  
  protected JProgressBar createComponent()
  {
    return new JProgressBar();
  }

  public JProgressBar getComponent()
  {
    return (JProgressBar) component;
  }

  /**
   * Get the progress bar's current progress value.
   */
  
  public int getValue()
  {
    return getComponent().getValue();
  }
  
  /**
   * Set the progress bar's current progress value.
   */
  
  public void setValue(int value)
  {
    getComponent().setValue(value);
  }
  
  /**
   * Get the progress bar's minimum progress value.
   */
  
  public int getMinimum()
  {
    return getComponent().getMinimum();
  }
  
  /**
   * Set the progress bar's minimum progress value.
   */
  
  public void setMinimum(int min)
  {
    getComponent().setMinimum(min);
  }
  
  /**
   * Get the progress bar's maximum progress value.
   */
  
  public int getMaximum()
  {
    return getComponent().getMaximum();
  }
  
  /**
   * Set the progress bar's maximum progress value.
   */
  
  public void setMaximum(int max)
  {
    getComponent().setMaximum(max);
  }
  
  /**
   * Get the progress bar's orientation, HORIZONTAL or VERTICAL.
   */
  
  public Orientation getOrientation()
  {
    return (getComponent().getOrientation() == JProgressBar.HORIZONTAL ? HORIZONTAL : VERTICAL);
  }
  
  /**
   * Set the progress bar's orientation, HORIZONTAL or VERTICAL.
   */
  
  public void setOrientation(Orientation orient)
  {
    getComponent().setOrientation(orient.value);
  }
  
  /**
   * Get whether this progress bar is in indeterminate mode.  In indeterminate mode, the entire
   * progress bar animates continuously to show that work is being done, but no indication is given
   * of how much remains.
   */
  
  public boolean isIndeterminate()
  {
    return getComponent().isIndeterminate();
  }
  
  /**
   * Set whether this progress bar is in indeterminate mode.  In indeterminate mode, the entire
   * progress bar animates continuously to show that work is being done, but no indication is given
   * of how much remains.
   */
  
  public void setIndeterminate(boolean indeterminate)
  {
    getComponent().setIndeterminate(indeterminate);
  }
  
  /**
   * Get whether the progress bar displays a line of text describing the operation whose progress
   * is being monitored.
   */
  
  public boolean getShowProgressText()
  {
    return getComponent().isStringPainted();
  }
  
  /**
   * Set whether the progress bar displays a line of text describing the operation whose progress
   * is being monitored.
   */
  
  public void setShowProgressText(boolean show)
  {
    getComponent().setStringPainted(show);
  }
  
  /**
   * Get the line of text displayed on the progress bar.  This text is only shown if
   * setShowProgessText(true) has been called.
   */
  
  public String getProgressText()
  {
    return getComponent().getString();
  }
  
  /**
   * Set the line of text displayed on the progress bar.  This text is only shown if
   * setShowProgessText(true) has been called.
   */
  
  public void setProgressText(String text)
  {
    getComponent().setString(text);
  }
  
  /**
   * This inner class represents an orientation (horizontal or vertical) for the split.
   */
  
  public static class Orientation
  {
    protected int value;
    
    private Orientation(int value)
    {
      this.value = value;
    }
  }
}
