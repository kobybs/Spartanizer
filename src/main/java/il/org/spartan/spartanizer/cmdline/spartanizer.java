package il.org.spartan.spartanizer.cmdline;

import static il.org.spartan.spartanizer.cmdline.system.*;
import static il.org.spartan.tide.*;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.jface.text.*;
import org.eclipse.text.edits.*;

import il.org.spartan.*;
import il.org.spartan.bench.*;
import il.org.spartan.collections.*;
import il.org.spartan.plugin.*;
import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.spartanizer.tippers.*;
import il.org.spartan.spartanizer.tipping.*;
import il.org.spartan.spartanizer.utils.*;
import il.org.spartan.utils.*;

import static il.org.spartan.plugin.eclipse.*;

/** Scans files named by folder, ignore test files, and collect statistics, on
 * classes, methods, etc.
 * @author Yossi Gil
 * @year 2015 */
public final class spartanizer {
  private static final String folder = "/tmp/";
  private final Toolbox toolbox = new Toolbox();
  private int done;
  private final String inputPath;
  private final String beforeFileName;
  private final String afterFileName;
  private PrintWriter befores;
  private PrintWriter afters;
  private CSVStatistics report;
  private final String reportFileName;
  private File currentFile;

  public static void main(final String[] args) {
    
    for (final String ¢ : args.length != 0 ? args : new String[] { "." })
      new spartanizer(¢).fire();
  }

  static double d(final double n1, final double n2) {
    return 1 - n2 / n1;
  }

  static String p(final int n1, final int n2) {
    return Unit.formatRelative(d(n1, n2));
  }

  static double ratio(final double n1, final double n2) {
    return n2 / n1;
  }

  private spartanizer(final String path) {
    this(path, system.folder2File(path));
  }

  private spartanizer(final String inputPath, final String name) {
    this.inputPath = inputPath;
    beforeFileName = folder + name + ".before.java";
    afterFileName = folder + name + ".after.java";
    reportFileName = folder + name + ".CSV";
  }

  public Process bash(final String shellCommand) {
    final String[] command = { "/bin/bash", "-c", shellCommand };
    try {
      final Process p = Runtime.getRuntime().exec(command);
      if (p != null)
        return system.dumpOutput(p);
    } catch (final IOException x) {
      monitor.logProbableBug(this, x);
    }
    return null;
  }

  public Process shellEssenceMetrics(final String fileName) {
    return bash("./essence < " + fileName + " >" + essenced(fileName));
  }

  boolean collect(final BodyDeclaration ¢) {
    final int length = ¢.getLength();
    final int tokens = metrics.tokens(¢ + "");
    final int nodes = metrics.nodesCount(¢);
    final int body = metrics.bodySize(¢);
    final int tide = clean(¢ + "").length();
    final int essence = Essence.of(¢ + "").length();
    // perform spartanization
    final String out = fixedPoint(¢);
    final int length2 = out.length();
    final int tokens2 = metrics.tokens(out);
    final int tide2 = clean(out + "").length();
    final int essence2 = Essence.of(out + "").length();
    final int wordCount = code.wc(il.org.spartan.spartanizer.cmdline.Essence.of(out + ""));
    final ASTNode from = makeAST.COMPILATION_UNIT.from(out);
    final int nodes2 = metrics.nodesCount(from);
    final int body2 = metrics.bodySize(from);
    System.err.println(++done + " " + extract.category(¢) + " " + extract.name(¢));
    // printout
    befores.print(¢);
    afters.print(out);
    // generate csv with statistics
    report.summaryFileName();
    report//
        .put("File", currentFile)//
        .put("TipperCategory", extract.category(¢))//
        .put("Name", extract.name(¢))//
        .put("Nodes1", nodes)//
        .put("Nodes2", nodes2)//
        .put("Δ Nodes", nodes - nodes2)//
        .put("δ Nodes", d(nodes, nodes2))//
        .put("δ Nodes %", Double.parseDouble(removePercentChar(p(nodes, nodes2))))//
        .put("Body", body)//
        .put("Body2", body2)//
        .put("Δ Body", body - body2)//
        .put("δ Body", d(body, body2))//
        .put("% Body", Double.parseDouble(removePercentChar(p(body, body2))))//
        .put("Length1", length)//
        .put("Tokens1", tokens)//
        .put("Tokens2", tokens2)//
        .put("Δ Tokens", tokens - tokens2)//
        .put("δ Tokens", d(tokens, tokens2))//
        .put("% Tokens", Double.parseDouble(removePercentChar(p(tokens, tokens2))))//
        .put("Length1", length)//
        .put("Length2", length2)//
        .put("Δ Length", length - length2)//
        .put("δ Length", d(length, length2))//
        .put("% Length", Double.parseDouble(removePercentChar(p(length, length2))))//
        .put("Tide1", tide)//
        .put("Tide2", tide2)//
        .put("Δ Tide2", tide - tide2)//
        .put("δ Tide2", d(tide, tide2))//
        .put("δ Tide2", Double.parseDouble(removePercentChar(p(tide, tide2))))//
        .put("Essence1", essence)//
        .put("Essence2", essence2)//
        .put("Δ Essence", essence - essence2)//
        .put("δ Essence", d(essence, essence2))//
        .put("% Essence", Double.parseDouble(removePercentChar(p(essence, essence2))))//
        .put("Words)", wordCount).put("R(T/L)", ratio(length, tide)) //
        .put("R(E/L)", ratio(length, essence)) //
        .put("R(E/T)", ratio(tide, essence)) //
        .put("R(B/S)", ratio(nodes, body)) //
    ;
    report.nl();
    return false;
  }

  /** @param ¢
   * @return */
  private String fixedPoint(final BodyDeclaration ¢) {
      return fixedPoint((¢ + "")); 
  }

  void collect(final CompilationUnit u) {
    u.accept(new ASTVisitor() {
      @Override public boolean visit(final MethodDeclaration ¢) {
        // TODO Marco: Check that this method does not have a <code>@Test</code>
        // annotation
        return collect(¢);
      }
      
      @Override public boolean visit(final TypeDeclaration ¢) {
        return collect(¢);
      }

    });
  }
  
  public String fixedPoint(final String from) {
    for (final Document $ = new Document(from);;) {
      final CompilationUnit u = (CompilationUnit) makeAST.COMPILATION_UNIT.from($.get());
      final ASTRewrite r = createRewrite(u);
      final TextEdit e = r.rewriteAST($, null);
      try {
        e.apply($);
      } catch (final MalformedTreeException | IllegalArgumentException | BadLocationException x) {
        monitor.logEvaluationError(this, x);
        throw new AssertionError(x);
      }
      if (!e.hasChildren())
        return $.get();
    }
  }
    
  public ASTRewrite createRewrite(final CompilationUnit ¢) {
    final ASTRewrite $ = ASTRewrite.create(¢.getAST());
    consolidateTips($, ¢);
    return $;
  }
  
  public ASTRewrite rewriterOf(final CompilationUnit ¢){ 
    final ASTRewrite $ = ASTRewrite.create(¢.getAST());
    consolidateTips($, ¢); 
    return $;
  }
  
  public void consolidateTips(final ASTRewrite r, final CompilationUnit u){ 
    Toolbox.refresh(); // leave this?
    u.accept(new DispatchingVisitor() {
      @Override protected <N extends ASTNode> boolean go(final N n) {
        TrimmerLog.visitation(n);
        if (!check(n) || disabling.on(n)) // removed !inRange(m, n) || !check(n) is always false
          return true;
        final Tipper<N> w = getTipper(n);
        if (w == null)
          return true;
        System.out.println(w.description(n));
        Tip s = null;
        try {
          s = w.tip(n, exclude);
          TrimmerLog.tip(w, n);
        } catch (final TipperFailure f) {
          monitor.debug(this, f);
        }
        if (s != null)
          TrimmerLog.application(r, s);
        return true;
      }

      @Override protected void initialization(final ASTNode ¢) {
        disabling.scan(¢);
      }
    });
  }
  
  @SuppressWarnings("static-method") protected <N extends ASTNode> Tipper<N> getTipper(N ¢) {
    return Toolbox.defaultInstance().firstTipper(¢);
  }
  
  /**
   * [[SuppressWarningsSpartan]]
   */
  @SuppressWarnings("static-method") protected <N extends ASTNode> boolean check(@SuppressWarnings("unused") N ¢) {
    return true;
  }

  void collect(final File f) {
    if (!f.getPath().contains("src/test"))
      try {
        currentFile = f;
        collect(FileUtils.read(f));
      } catch (final IOException e) {
        monitor.infoIOException(e, "File = " + f);
      }
  }

  void collect(final String javaCode) {
    collect((CompilationUnit) makeAST.COMPILATION_UNIT.from(javaCode));
  }

  void fire() {
    System.out.println(Toolbox.defaultInstance().hooksCount());
    collect();
    runEssence();
    runWordCount();
  }

  void runEssence() {
    shellEssenceMetrics(beforeFileName);
    shellEssenceMetrics(afterFileName);
  }

  private void collect() {
    System.err.printf( //
        " Input path=%s\n" + //
            "Before path=%s\n" + //
            " After path=%s\n" + //
            "Report path=%s\n" + //
            "\n", //
        inputPath, //
        beforeFileName, //
        afterFileName, //
        reportFileName);
    try (PrintWriter b = new PrintWriter(new FileWriter(beforeFileName)); //
        PrintWriter a = new PrintWriter(new FileWriter(afterFileName))) {
      befores = b;
      afters = a;
      report = new CSVStatistics(reportFileName, "property");
      for (final File ¢ : new FilesGenerator(".java").from(inputPath))
        collect(¢);
    } catch (final IOException x) {
      x.printStackTrace();
      System.err.println(done + " files processed; processing of " + inputPath + " failed for some I/O reason");
    }
    System.err.print("\n Done: " + done + " files processed.");
    System.err.print("\n Summary: " + report.close());
  }

  private void runWordCount() {
    bash("wc " + separate.these(beforeFileName, afterFileName, essenced(beforeFileName), essenced(afterFileName)));
  }
}