package lang.lightweightjava.ast.statement

import lang.lightweightjava.ast._
import name.namegraph.{NameGraphExtended, NameGraph}
import name.{Identifier, Renaming}

case class MethodCall(target: VariableName, sourceObject: TermVariable, methodName: Identifier, methodParameters: TermVariable*) extends Statement {
  require(AST.isLegalName(methodName.name), "Method name '" + methodName + "' is no legal Java method name")

  override def allNames = target.allNames ++ sourceObject.allNames ++ methodParameters.foldLeft(Set[Identifier]())(_ ++ _.allNames) + methodName

  override def rename(renaming: Renaming) =
    MethodCall(target.rename(renaming).asInstanceOf[VariableName], sourceObject.rename(renaming), renaming(methodName), methodParameters.map(_.rename(renaming)): _*)

  override def typeCheckForTypeEnvironment(program: Program, typeEnvironment: TypeEnvironment) = {
    typeEnvironment(sourceObject) match {
      case className@ClassName(_) => program.findMethod(program.getClassDefinition(className).get, methodName.name) match {
        case Some(method) => require(methodParameters.size == method.signature.parameters.size,
          "Method '" + methodName + "' is called with an invalid number of parameters in class '" + typeEnvironment(This).asInstanceOf[ClassName].name + "'")
          methodParameters.zip(method.signature.parameters).map(param => require(param._1 == Null || program.checkSubclass(typeEnvironment(param._1), param._2.variableType),
            "Method '" + methodName + "' is called with an incompatible value for parameter '" + param._2.name + "' in class '" + typeEnvironment(This).asInstanceOf[ClassName].name + "'"))
          require(className.name == typeEnvironment(This).name || method.signature.accessModifier == AccessModifier.PUBLIC,
            "Trying to call private method '" + method.signature.methodName + "' of class '" + typeEnvironment(sourceObject).asInstanceOf[ClassName].name + "' externally!")
          require(program.checkSubclass(method.signature.returnType, typeEnvironment(target)),
          "Variable and the method return value it is assigned in class '" + typeEnvironment(This).asInstanceOf[ClassName].name + "' are incompatible!")
          typeEnvironment
        case None =>
          throw new IllegalArgumentException("Class '" + className.name + "' doesn't have method '" + methodName + "' called in class '" + typeEnvironment(This).asInstanceOf[ClassName].name + "'")
      }
      case _ => throw new IllegalArgumentException("Class 'Object' doesn't have method '" + methodName + "' called in class '" + typeEnvironment(This).asInstanceOf[ClassName].name + "'")
    }
  }

  override def resolveNames(nameEnvironment: ClassNameEnvironment, methodEnvironment: VariableNameEnvironment, typeEnvironment : TypeEnvironment) = {
    val variablesGraph = target.resolveVariableNames(methodEnvironment) + sourceObject.resolveVariableNames(methodEnvironment) +
      methodParameters.foldLeft(NameGraph(Set(), Map()))(_ + _.resolveVariableNames(methodEnvironment))

    if (typeEnvironment.contains(sourceObject) && nameEnvironment.contains(typeEnvironment(sourceObject).name)) {
      val fieldMap = nameEnvironment(typeEnvironment(sourceObject).name).map(_._3).filter(_.contains(methodName.name))

      (variablesGraph + NameGraphExtended(Set(methodName), Map(methodName -> fieldMap.flatMap(_(methodName.name)))), (methodEnvironment, typeEnvironment))
    }
    else {
      (variablesGraph + NameGraph(Set(methodName), Map()), (methodEnvironment, typeEnvironment))
    }
  }

  override def toString = target.toString + " = " + sourceObject.toString + "." + methodName.toString + "(" + methodParameters.mkString(", ") + ");"
}
