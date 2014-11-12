package lang.lightweightjava

import lang.lightweightjava.ast.statement.VariableName
import lang.lightweightjava.configuration.NormalConfiguration
import lang.lightweightjava.localdeclaration.LocalDeclarationTransformation
import name.{NameFix, Nominal}
import org.scalatest.{FlatSpec, Matchers}

class NameFixTest extends FlatSpec with Matchers {
  val p1 =
    "class A {\n" +
    "  A method_ldt(A test) {\n" +
    "   return test;\n" +
    "  }\n" +
    "}\n" +
    "class B extends A {\n" +
    "   A method() {\n" +
    "     A test;\n" +
    "     return this.method_ldt(this);\n" +
    "   }\n" +
    "}\n" +
    "\n"
  val st1 = "x = new B();\n" +
      "y = x.method();"
  val p2 = "class B {\n" +
      "   B method_ldt(B test) {\n" +
      "     return test;\n" +
      "   }\n" +
      "   B method() {\n" +
      "     B test;\n" +
      "     return this.method_ldt(this);\n" +
      "   }\n" +
      "}\n" +
      "\n"
  "Name Fix" should "fix the LDT test program without declaration conflicts" in (Parser.parseAll(Parser.configuration, p1 + st1) match {
    case Parser.Success(p, _) =>
      val pNameGraph = p.program.asInstanceOf[Nominal].resolveNames
      val pTransformed = LocalDeclarationTransformation.transform(p.program)
      val pTransformedNameGraph = pTransformed.asInstanceOf[Nominal].resolveNames
      val pFixed = NameFix.nameFix(pNameGraph, pTransformed)
      val pFixedNameGraph = pFixed.asInstanceOf[Nominal].resolveNames

      pNameGraph.C.size should be (0)
      pTransformedNameGraph.C.size should be (0)
      pFixedNameGraph.C.size should be (0)

      // If NameFix did not fix the program, type checking or interpretation will fail
      val result = Interpreter.interpret(NormalConfiguration(pFixed, p.state, p.heap, p.asInstanceOf[NormalConfiguration].programFlow:_*))
      result.state(VariableName("x")) should be (result.state(VariableName("y")))

      info("Name graph stats for P1 before transformation: " + pNameGraph.V.size + " nodes, " + pNameGraph.E.size + " edges, " + pNameGraph.C.size + " errors")
      info("Name graph stats for P1 after transformation: " + pTransformedNameGraph.V.size + " nodes, " + pTransformedNameGraph.E.size + " edges, " + pTransformedNameGraph.C.size + " errors")
      info("Name graph stats for P1 after NameFix: " + pFixedNameGraph.V.size + " nodes, " + pFixedNameGraph.E.size + " edges, " + pFixedNameGraph.C.size + " errors")
      info("NameFix P1 result: " + pFixed.toString);
    case Parser.NoSuccess(msg, _) => fail(msg)
  })
  "Name Fix" should "fix the LDT test program with declaration conflicts" in (Parser.parseAll(Parser.configuration, p2 + st1) match {
    case Parser.Success(p, _) =>
      val pNameGraph = p.program.asInstanceOf[Nominal].resolveNames
      val pTransformed = LocalDeclarationTransformation.transform(p.program)
      val pTransformedNameGraph = pTransformed.asInstanceOf[Nominal].resolveNames
      val pFixed = NameFix.nameFix(pNameGraph, pTransformed)
      val pFixedNameGraph = pFixed.asInstanceOf[Nominal].resolveNames

      pNameGraph.C.size should be (0)
      pTransformedNameGraph.C.size should be (1)
      pFixedNameGraph.C.size should be (0)

      // If NameFix did not fix the program, type checking or interpretation will fail
      val result = Interpreter.interpret(NormalConfiguration(pFixed, p.state, p.heap, p.asInstanceOf[NormalConfiguration].programFlow:_*))
      result.state(VariableName("x")) should be (result.state(VariableName("y")))

      info("Name graph stats for P2 before transformation: " + pNameGraph.V.size + " nodes, " + pNameGraph.E.size + " edges, " + pNameGraph.C.size + " errors")
      info("Name graph stats for P2 after transformation: " + pTransformedNameGraph.V.size + " nodes, " + pTransformedNameGraph.E.size + " edges, " + pTransformedNameGraph.C.size + " errors")
      info("Name graph stats for P2 after NameFix: " + pFixedNameGraph.V.size + " nodes, " + pFixedNameGraph.E.size + " edges, " + pFixedNameGraph.C.size + " errors")
      info("NameFix P2 result: " + pFixed.toString);
    case Parser.NoSuccess(msg, _) => fail(msg)
  })
}
