package buoy.event;

/**
 * This class allows you to use anonymous inner classes as event handlers in unsigned applets and
 * other situations where a SecurityManager prevents use of the AccessibleObject API.  If you are
 * writing an application, a signed applet, or any other type of program which is not restricted by
 * a security manager, you can ignore this class.
 * <p>
 * The Buoy event handling mechanism uses reflection to invoke methods on event listeners.  Normally,
 * this can only be done if the method and the class which defines it are both public.  Since anonymous
 * inner classes are never public, their methods cannot be directly used for handling events.
 * <p>
 * These access restrictions can be circumvented by using the AccessibleObject API.  This allows any
 * method to be invoked, regardless of whether it is public or not.  That is why Buoy applications are
 * free to use private methods, as well as methods of non-public classes, for handling events.
 * Unfortunately, the AccessibleObject API cannot be used when a SecurityManager is in place.
 * <p>
 * This class provides an alternate method for handling events with anonymous inner classes.  To use it,
 * your inner class extends EventProcessor and overrides <code>handleEvent()</code>:
 * <p>
 * <pre>
 * widget.addEventLink(CommandEvent.class, new EventProcessor() {
 *   public void handleEvent(Object event)
 *   {
 *     // Handle the event
 *   }
 * });
 * </pre>
 * <p>
 * Because <code>processEvent()</code> is defined by the public class <code>EventProcessor</code>,
 * Buoy is free to invoke it by reflection.  It then invokes your <code>handleEvent()</code> method.
 * This arrangement has only two minor disadvantages compared to the standard one.  First, your
 * <code>handleEvent()</code> method must be public, which is usually not an issue for an
 * anonymous inner class.  Second, because its signature is fixed by the parent class, it must always
 * take an argument of type <code>Object</code> rather than a more specific event type.
 * 
 * @author Peter Eastman
 */

public abstract class EventProcessor
{
  /**
   * This simply calls handleEvent().
   */

  public void processEvent(Object event)
  {
    handleEvent(event);
  }

  public abstract void handleEvent(Object event);
}
