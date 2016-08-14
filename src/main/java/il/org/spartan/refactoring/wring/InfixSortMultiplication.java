package il.org.spartan.refactoring.wring;

import static il.org.spartan.Utils.*;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Assignment.*;

import il.org.spartan.refactoring.preferences.*;
import il.org.spartan.refactoring.utils.*;

/**
 * A {@link Wring} that sorts the arguments of a {@link Operator#TIMES}
 * expression.
 *
 * @author Yossi Gil
 * @since 2015-07-17
 */
public final class InfixSortMultiplication extends Wring.InfixSorting implements Kind.ReorganizeExpression {
  @Override boolean sort(final List<Expression> es) {
    return ExpressionComparator.MULTIPLICATION.sort(es);
  }
  @Override boolean scopeIncludes(final InfixExpression e) {
    return in(e.getOperator(), TIMES);
  }
}