package lang.java.trans

import javax.lang.model.`type`.TypeKind

import com.sun.source.tree._
import com.sun.tools.javac.code._
import com.sun.tools.javac.tree.JCTree._
import com.sun.tools.javac.tree.{JCTree, TreeMaker, TreeCopier}
import com.sun.tools.javac.util.{Name, ListBuffer, List}
import lang.java.Tree

object MakeFieldPrivate {
  def apply(sym: Symbol, tree: Tree): Tree = {
    val trans = new MakeFieldPrivate[Void](TreeMaker.instance(tree.context), sym)
    tree.transform(trans, null)
  }
}

class MakeFieldPrivate[P](tm: TreeMaker, sym: Symbol) extends TreeCopier[P](tm) {

  val getName = sym.name.table.fromString("get" + sym.name.toString.capitalize)
  val setName = sym.name.table.fromString("set" + sym.name.toString.capitalize)

  var foundVarDecl = false
  var isFinal: Boolean = _
  var oldVisibility: Long = _
  var vartype: JCExpression = _
  var varname: Name = _

  def makeGetter(p: P): JCMethodDecl = {
    val mods: JCModifiers = tm.Modifiers(oldVisibility)
    val name = getName
    val restype: JCExpression = this.copy(vartype, p)
    val typarams: List[JCTree.JCTypeParameter] = List.nil()
    val params: List[JCTree.JCVariableDecl] = List.nil()
    val thrown: List[JCTree.JCExpression] = List.nil()
    val body: JCTree.JCBlock = tm.Block(0l, List.of(tm.Return(tm.Ident(varname))))
    val defaultValue: JCTree.JCExpression = null
    tm.MethodDef(mods, name, restype, typarams, params, thrown, body, defaultValue)
  }
  
  def makeSetter(thisType: Type, p: P): JCMethodDecl = {
    val mods: JCModifiers = tm.Modifiers(oldVisibility)
    val name = setName
    val restype: JCExpression = tm.TypeIdent(TypeTags.VOID)
    val typarams: List[JCTree.JCTypeParameter] = List.nil()
    val params: List[JCTree.JCVariableDecl] = List.of(tm.VarDef(tm.Modifiers(0l), varname, this.copy(vartype, p), null))
    val thrown: List[JCTree.JCExpression] = List.nil()
    val thisName = tm.Select(tm.This(thisType), varname)
    val body: JCTree.JCBlock = tm.Block(0l, List.of(tm.Exec(tm.Assign(thisName, tm.Ident(varname)))))
    val defaultValue: JCTree.JCExpression = null
    tm.MethodDef(mods, name, restype, typarams, params, thrown, body, defaultValue)
  }

  // rewrite variable declaration to make it private
  override def visitVariable(node: VariableTree, p: P): JCTree = node match {
    case n: JCVariableDecl if n.sym == sym =>
      if (foundVarDecl)
        throw new IllegalStateException(s"Duplicate variable declaration $sym")
      foundVarDecl = true
      val modAnnos = this.copy(n.mods.annotations, p)
      val oldFlags = n.mods.flags
      isFinal = (oldFlags & Flags.FINAL) != 0
      oldVisibility = oldFlags & (Flags.PRIVATE | Flags.PROTECTED | Flags.PUBLIC)
      val newFlags = (oldFlags & ~Flags.PROTECTED & ~Flags.PUBLIC) | Flags.PRIVATE
      val mods = tm.at(n.mods.pos).Modifiers(newFlags, modAnnos)
      vartype = this.copy(n.vartype, p)
      varname = n.name
      val init = this.copy(n.init, p)
      tm.at(n.pos).VarDef(mods, varname, vartype, init)
    case _ =>
      super.visitVariable(node, p)
  }

  override def visitClass(node : ClassTree, p : P) : JCTree = node match {
    case n: JCClassDecl =>
      val mods = this.copy(n.mods, p)
      val typarams = this.copy(n.typarams, p)
      val extending = this.copy(n.extending, p)
      val implementing = this.copy(n.implementing, p)

      val defs = new ListBuffer[JCTree]()
      val it = n.defs.iterator()
      while (it.hasNext)
        defs.append(this.copy(it.next(), p))

      if (foundVarDecl) {
        foundVarDecl = false
        defs.add(makeGetter(p))
        if (!isFinal)
          defs.add(makeSetter(n.`type`, p))
      }
      tm.at(n.pos).ClassDef(mods, n.name, typarams, extending, implementing, defs.toList)
  }

  override def visitIdentifier(node: IdentifierTree, p: P): JCTree = node match {
    case n: JCIdent if n.sym == sym =>
      val m = tm.at(n.pos).Ident(getName)
      tm.at(n.pos).Apply(List.nil(), m, List.nil())
    case _ =>
      super.visitIdentifier(node, p)
  }

  override def visitExpressionStatement(node: ExpressionStatementTree, p: P): JCTree = node.getExpression match {
    case n: JCAssign =>
      val rewrite = n.lhs match {
        case l: JCIdent if l.sym == sym => true
        case l: JCFieldAccess if l.sym == sym => true
        case _ => false
      }
      if (rewrite) {
        val m = tm.at(n.pos).Ident(setName)
        val rhs = this.copy(n.rhs, p)
        tm.at(n.pos).Exec(tm.Apply(List.nil(), m, List.of(rhs)))
      }
      else
        super.visitExpressionStatement(node, p)
    case _ =>
      super.visitExpressionStatement(node, p)
  }
}
