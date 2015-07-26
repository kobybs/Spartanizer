package org.spartan.refactoring.spartanizations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.spartan.refactoring.spartanizations.TESTUtils.apply;
import static org.spartan.refactoring.spartanizations.TESTUtils.assertSimilar;
import static org.spartan.refactoring.spartanizations.TESTUtils.collect;
import static org.spartan.refactoring.spartanizations.TESTUtils.compressSpaces;
import static org.spartan.refactoring.spartanizations.TESTUtils.p;
import static org.spartan.refactoring.spartanizations.TESTUtils.peel;
import static org.spartan.refactoring.spartanizations.TESTUtils.wrap;
import static org.spartan.refactoring.spartanizations.Wrings.PUSHDOWN_NOT;

import java.util.Collection;

import org.eclipse.jdt.core.dom.PrefixExpression;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.spartan.utils.Utils;

/**
 * Unit tests for {@link Ternarize}.
 *
 * @author Yossi Gil
 * @since 2014-07-13
 */
@RunWith(Parameterized.class) //
@FixMethodOrder(MethodSorters.NAME_ASCENDING) //
@SuppressWarnings({ "javadoc" }) //
public class TernarizeTest {
  /** The name of the specific test for this transformation */
  @Parameter(0) public String name;
  /** Where the input text can be found */
  @Parameter(1) public String input;
  /** Where the expected output can be found? */
  @Parameter(2) public String output;
  @Test public void inputNotNull() {
    assertNotNull(input);
  }
  @Test public void applicable() {
    final PrefixExpression p = p(input);
    assertNotNull(p);
  }
  @Test public void withinScopeOfNegation() {
    assertTrue(PUSHDOWN_NOT.inner.scopeIncludes(p(input)));
  }
  @Test public void eligibleForNegation() {
    assertTrue(PUSHDOWN_NOT.inner.eligible(p(input)));
  }
  @Test public void hasReplacement() {
    assertNotNull(PUSHDOWN_NOT.inner.replacement(p(input)));
  }
  @Test public void simiplifies() {
    final String from = input;
    final String expected = output;
    final String wrap = wrap(from);
    assertEquals(from, peel(wrap));
    final String unpeeled = apply(new Trimmer(), wrap);
    final String peeled = peel(unpeeled);
    if (wrap.equals(unpeeled))
      fail("Nothing done on " + from);
    if (peeled.equals(from))
      assertNotEquals("No similification of " + from, from, peeled);
    if (compressSpaces(peeled).equals(compressSpaces(from)))
      assertNotEquals("Simpification of " + from + " is just reformatting", compressSpaces(peeled), compressSpaces(from));
    assertSimilar(expected, peeled);
  }
  /**
   * Generate test cases for this parameterized class.
   *
   * @return a collection of cases, where each case is an array of three
   *         objects, the test case name, the input, and the file.
   */
  @Parameters(name = "{index}: {0} {1}") //
  public static Collection<Object[]> cases() {
    return collect(cases);
  }
  static String[][] cases = Utils.asArray(//
      Utils.asArray("not of AND", "!(f() && f(5))", "(!f() || !f(5))"), //
      Utils.asArray("not of EQ", "!(3 == 5)", "3 != 5"), //
      Utils.asArray("not of AND nested", "!(f() && (f(5)))", "(!f() || !f(5))"), //
      Utils.asArray("not of EQ nested", "!((((3 == 5))))", "3 != 5"), //
      Utils.asArray("not of GE", "!(3 >= 5)", "3 < 5"), //
      Utils.asArray("not of GT", "!(3 > 5)", "3 <= 5"), //
      Utils.asArray("not of NE", "!(3 != 5)", "3 == 5"), //
      Utils.asArray("not of LE", "!(3 <= 5)", "3 > 5"), //
      Utils.asArray("not of LT", "!(3 < 5)", "3 >= 5"), //
      Utils.asArray("not of AND", "!(a && b && c)", "(!a || !b || !c)"), //
      Utils.asArray("not of OR", "!(a || b || c)", "(!a && !b && !c)"), //
      Utils.asArray("double NOT", "!!f()", "f()"), //
      Utils.asArray("not of OR 2", "!(f() || f(5))", "(!f() && !f(5))"), //
      null);
}