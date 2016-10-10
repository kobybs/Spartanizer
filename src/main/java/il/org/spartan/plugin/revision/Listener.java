package il.org.spartan.plugin.revision;

import java.util.*;

import il.org.spartan.spartanizer.utils.*;
import il.org.spartan.utils.*;

/** An abstract listener taking events that may have any number of parameters.
 * parameters; default implementation is empty, extend to specialize, or use
 * {@link Listener.S}
 * @author Ori Roth
 * @author Yossi Gil
 * @since 2016 */
public interface Listener {
  final Int id = new Int();

  default void tick(Object... os) {
    id.inner++;
    ___.unused(os);
  }

  /** A listener that records a long string of the message it got.
   * @author Yossi Gil
   * @since 2016 */
  public class Tracing implements Listener {
    private StringBuilder $ = new StringBuilder();

    public String $() {
      return $ + "";
    }

    @Override public void tick(Object... os) {
      $.append(id.inner() + ": ");
      for (Object o : os)
        pack(o);
      $.append('\n');
    }

    private void pack(Object o) {
      $.append("," + trim(o));
    }

    private static String trim(Object o) {
      return (o + "").substring(1, 35);
    }
  }

  /** An aggregating kind of {@link Listener} that dispatches the event it
   * receives to the multiple {@link Listener}s it stores internally.
   * @author Yossi Gil
   * @since 2.6 */
  public class S extends ArrayList<Listener> implements Listener {
    private static final long serialVersionUID = 1L;

    @Override public void tick(Object... os) {
      for (final Listener l : this)
        l.tick(os);
    }

    /** for fluent API use, i.e., <code>
     * 
     * <pre>
     * <b>public final</b> {@link Listener} listeners = {@link Listener.S}.{@link #empty()}
     * </pre>
     * 
     * <code>
     * @return an empty new instance */
    public static S empty() {
      return new S();
    }

    /** To be used in the following nano <code><pre>
       *  public interface Applicator {
       *    public class Settings extends Listeners {
       *      public class Action extends Setting {
       *         action1();
       *         action2();
       *      }
       *    } 
       *  }
        * </pre></code> parameterized solely by the name <code>Applicator</code>
     * and the action in <code>Action</code>
     * @return <code><b>this</b></code> */
    public List<Listener> listeners() {
      return this;
    }
  }
}