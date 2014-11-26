package lang.lambda.module

import lang.lambda._
import lang.lambda.num._
import name._
import org.scalatest._

class ModuleTest extends FunSuite {
  val fixer: NameFix = NameFix.fixerModular

  val baseModule = NoPrecedenceModule("base", Set(),
    Map((Name("one"), true) -> Num(1)))
  val oneAdderModule = NoPrecedenceModule("oneAdder", Set(baseModule),
    Map((Name("two"), true) -> Add(Var("one"), Var("one"))))
  val multiAdderModuleIP = InternalPrecedenceModule("multiAdder", Set(baseModule, oneAdderModule),
    Map((Name("three"), true) -> Add(Var("two"), Var("one")),
        (Name("two"), true) -> Add(Var("one"), Var("one"))))
  val multiAdderModuleEP = ExternalPrecedenceModule("multiAdder", Set(baseModule, oneAdderModule),
    Map((Name("three"), true) -> Add(Var("two"), Var("one")),
        (Name("two"), true) -> Add(Var("one"), Var("one"))))
  val multiAdderModuleNP = NoPrecedenceModule("multiAdder", Set(baseModule, oneAdderModule),
    Map((Name("three"), true) -> Add(Var("two"), Var("one")),
        (Name("two"), true) -> Add(Var("one"), Var("one"))))

  val oneAdderModuleConflicting = NoPrecedenceModule("oneAdder2", Set(),
    Map((Name("two"), true) -> Add(Num(1), Num(1))))
  val multiAdderModuleIPconflict = InternalPrecedenceModule("multiAdder", Set(baseModule, oneAdderModuleConflicting, oneAdderModule),
    Map((Name("three"), true) -> Add(Var("two"), Var("one")),
        (Name("two"), true) -> Add(Var("one"), Var("one"))))
  val multiAdderModuleNPconflict = NoPrecedenceModule("multiAdder", Set(baseModule, oneAdderModuleConflicting, oneAdderModule),
    Map((Name("three"), true) -> Add(Var("two"), Var("one")),
        (Name("two"), true) -> Add(Var("one"), Var("one"))))

  def oneRef = (baseModule.id, baseModule.defs.head._1._1.id)
  def twoRef = (oneAdderModule.id, oneAdderModule.defs.head._1._1.id)
  def twoConflictingRef = (oneAdderModuleConflicting.id, oneAdderModuleConflicting.defs.head._1._1.id)
  def twoInternalRef(m: Module) = m.defs.map(_._1._1).find(_.name == "two").get.id

  test ("Internal precedence test") {
    val g = multiAdderModuleIP.resolveNames
    assert(g.V.size == 6, "There should be 6 nodes in the name graph of the multiAdderModuleIP!")
    assert(g.V.count(_._2) == 2, "There should be 2 exported nodes in the name graph of the multiAdderModuleIP!")

    assert(g.E.size == 1, "There should be 1 internal edge in the name graph of the multiAdderModuleIP!")
    assert(g.E.head._2 == twoInternalRef(multiAdderModuleIP), "The internal edge in the name graph of the multiAdderModuleIP should point to 'two'!")
    assert(g.EOut.size == 3, "There should be 3 external edges in the name graph of the multiAdderModuleIP!")
    assert(g.EOut.forall(_._2 == oneRef), "All external edges in the name graph of the multiAdderModuleIP should point to 'one'!")
    assert(g.C.size == 0, "There should be no declaration conflicts in the name graph of the multiAdderModuleIP")
  }

  test ("External precedence test") {
    val g = multiAdderModuleEP.resolveNames
    assert(g.V.size == 6, "There should be 6 nodes in the name graph of the multiAdderModuleEP!")
    assert(g.V.count(_._2) == 2, "There should be 2 exported nodes in the name graph of the multiAdderModuleEP!")

    assert(g.E.size == 0, "There should be no internal edges in the name graph of the multiAdderModuleEP!")
    assert(g.EOut.size == 4, "There should be 4 external edges in the name graph of the multiAdderModuleEP!")

    val oneRefs = g.EOut.filter(_._2 == oneRef)
    val twoRefs = g.EOut.filter(_._2 == twoRef)
    assert(oneRefs.size == 3, "There should be 3 external edges in the name graph of the multiAdderModuleEP that point to 'one'!")
    assert(twoRefs.size == 1, "There should be 1 external edges in the name graph of the multiAdderModuleEP that points to 'two'!")

    assert(g.C.size == 0, "There should be no declaration conflicts in the name graph of the multiAdderModuleEP")
  }

  test ("No precedence test") {
    val g = multiAdderModuleNP.resolveNames
    assert(g.V.size == 6, "There should be 6 nodes in the name graph of the multiAdderModuleNP!")
    assert(g.V.count(_._2) == 2, "There should be 2 exported nodes in the name graph of the multiAdderModuleNP!")

    assert(g.E.size + g.EOut.size == 4, "There should be a total number of 4 edges in the name graph of the multiAdderModuleNP!")

    val oneRefs = g.EOut.filter(_._2 == oneRef)
    val twoRefs = g.EOut.filter(_._2 == twoRef)
    val twoRefsInternal = g.E.filter(_._2 == twoInternalRef(multiAdderModuleNP))
    assert(oneRefs.size == 3, "There should be 3 external edges in the name graph of the multiAdderModuleNP that point to 'one'!")
    assert(twoRefs.size + twoRefsInternal.size == 1, "There should be 1 edge in the name graph of the multiAdderModuleNP that points to either the internal or external 'two'!")

    assert(g.C.size == 1, "There should be 1 declaration conflict in the name graph of the multiAdderModuleNP")

    val conflict = Set(twoRef._2, twoInternalRef(multiAdderModuleNP))
    assert(g.C.head == conflict, "The declaration conflict in the name graph of the multiAdderModuleNP should be between the internal and the external declaration of 'two'")
  }

  test ("Internal precedence import conflict test") {
    val g = multiAdderModuleIPconflict.resolveNames
    assert(g.V.size == 6, "There should be 6 nodes in the name graph of the multiAdderModuleIPconflict!")
    assert(g.V.count(_._2) == 2, "There should be 2 exported nodes in the name graph of the multiAdderModuleIPconflict!")

    assert(g.E.size == 1, "There should be 1 internal edge in the name graph of the multiAdderModuleIPconflict!")
    assert(g.E.head._2 == twoInternalRef(multiAdderModuleIPconflict), "The internal edge in the name graph of the multiAdderModuleIPconflict should point to 'two'!")
    assert(g.EOut.size == 3, "There should be 3 external edges in the name graph of the multiAdderModuleIPconflict!")
    assert(g.EOut.forall(_._2 == oneRef), "All external edges in the name graph of the multiAdderModuleIPconflict should point to 'one'!")
    assert(g.C.size == 1, "There should be an declaration conflict in the name graph of the multiAdderModuleIPconflict")

    val conflict = Set(twoRef._2, twoConflictingRef._2)
    assert(g.C.head == conflict, "The declaration conflict in the name graph of the multiAdderModuleIPconflict should be between the declarations of 'two' in the oneAdderModules")
  }

  test ("No precedence import conflict test") {
    val g = multiAdderModuleNPconflict.resolveNames
    assert(g.V.size == 6, "There should be 6 nodes in the name graph of the multiAdderModuleNPconflict!")
    assert(g.V.count(_._2) == 2, "There should be 2 exported nodes in the name graph of the multiAdderModuleNPconflict!")

    assert(g.E.size + g.EOut.size == 4, "There should be a total number of 4 edges in the name graph of the multiAdderModuleNPconflict!")

    val oneRefs = g.EOut.filter(_._2 == oneRef)
    val twoRefs = g.EOut.filter(_._2 == twoRef)
    val twoRefsInternal = g.E.filter(_._2 == twoInternalRef(multiAdderModuleNPconflict))
    assert(oneRefs.size == 3, "There should be 3 external edges in the name graph of the multiAdderModuleNPconflict that point to 'one'!")
    assert(twoRefs.size + twoRefsInternal.size == 1, "There should be 1 edge in the name graph of the multiAdderModuleNPconflict that points to either the internal or external 'two'!")

    assert(g.C.size == 1, "There should be 1 declaration conflict in the name graph of the multiAdderModuleNPconflict")

    val declConflict = Set(twoRef._2, twoInternalRef(multiAdderModuleNPconflict), twoConflictingRef._2)
    assert(g.C.head == declConflict, "The declaration conflict in the name graph of the multiAdderModuleNPconflict should be between the internal and both external declarations of 'two'")
  }
}
