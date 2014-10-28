package lang.lightweightjava.configuration

import lang.lightweightjava.ast.statement.Statement
import lang.lightweightjava.ast.{AST, Heap, Program, State}
import name.Name

case class NormalConfiguration(program: Program, state: State, heap: Heap, programFlow: Statement*) extends Configuration {
  override def freshName(usedNames: Set[String], oldName: Name) =
    AST.genFreshName(program.allNames.map(_.name) ++ state.keys.flatMap(_.allNames).map(_.name) ++ programFlow.flatMap(_.allNames).map(_.name) ++ usedNames, oldName)

  override def toString = program.toString + programFlow.fold("")(_ + _.toString + "\n") + "\n\n" +
    "(State: " + state.toString + ",\nHeap: " + heap.toString + ")"
}