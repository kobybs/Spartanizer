package org.spartan.refactoring.wring;

import static org.spartan.refactoring.utils.Funcs.elze;
import static org.spartan.refactoring.utils.Funcs.makeThrowStatement;
import static org.spartan.refactoring.utils.Funcs.then;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.spartan.refactoring.utils.Extract;
import org.spartan.refactoring.utils.Subject;

/**
 * A {@link Wring} to convert <code>if (x)
 *   throw b;
 * else
 *   throw c;</code> into <code>throw x? b : c</code>
 *
 * @author Yossi Gil
 * @since 2015-07-29
 */
public final class IfThrowFooElseThrowBar extends Wring.ReplaceCurrentNode<IfStatement> {
  @Override Statement replacement(final IfStatement s) {
    final Expression condition = s.getExpression();
    final Expression then = Extract.throwExpression(then(s));
    final Expression elze = Extract.throwExpression(elze(s));
    return then == null || elze == null ? null : makeThrowStatement(Subject.pair(then, elze).toCondition(condition));
  }
  @Override boolean scopeIncludes(final IfStatement s) {
    return s != null && Extract.throwExpression(then(s)) != null && Extract.throwExpression(elze(s)) != null;
  }
  @Override String description(@SuppressWarnings("unused") final IfStatement _) {
    return "Consolidate 'if' into a 'throw' statement of a conditional expression";
  }
}