package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Scope;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;


public final class TabImpl extends Tab {

	// TODO Exercise 4: implementation of symbol table

	public TabImpl(Parser p) {
		super(p);

		initGlobalScope();
	}


	public void initGlobalScope() {
		curScope = new Scope(null);

		curScope.insert(new Obj(Obj.Kind.Type, "int", intType));
		curScope.insert(new Obj(Obj.Kind.Type, "char", charType));


		Obj nullObj = new Obj(Obj.Kind.Con, "null", nullType);
		nullObj.val = 0;
		curScope.insert(nullObj);


		chrObj = new Obj(Obj.Kind.Meth, "chr", charType);
		chrObj.nPars = 1;
		openScope();
		Obj variable = new Obj(Obj.Kind.Var, "i", intType);
		variable.level = 1;
		curScope.insert(variable);
		chrObj.locals = curScope.locals();
		closeScope();
		curScope.insert(chrObj);


		ordObj = new Obj(Obj.Kind.Meth, "ord", intType);
		ordObj.nPars = 1;
		openScope();
		variable = new Obj(Obj.Kind.Var, "ch", charType);
		variable.level = 1;
		curScope.insert(variable);
		ordObj.locals = curScope.locals();
		closeScope();
		curScope.insert(ordObj);


		lenObj = new Obj(Obj.Kind.Meth, "len", intType);
		lenObj.nPars = 1;
		openScope();
		variable = new Obj(Obj.Kind.Var, "arr", new StructImpl(noType));
		variable.level = 1;
		curScope.insert(variable);
		lenObj.locals = curScope.locals();
		closeScope();
		curScope.insert(lenObj);

		noObj = new Obj(Obj.Kind.Var, "$noObj", noType);

		curLevel = -1;
	}


	public Obj find(String toFind) {
		Obj ret = curScope.findGlobal(toFind);
		if (ret != null) {
			return ret;
		} else {
			return noObj;
		}
	}


	public Obj findField(String toFind, Struct whereToFind) {
		if (whereToFind.fields.containsKey(toFind))
			return whereToFind.fields.get(whereToFind);
		return noObj;
	}

	public Obj insert(Obj.Kind kind, String name, StructImpl type) {

		Obj obj = new Obj(kind, name, type);
		if (kind == Obj.Kind.Var) {
			obj.adr = curScope.nVars();
			obj.level = curLevel;
		}
		if (curScope.findLocal(name) == null) {
			curScope.insert(obj);
		} else {
			parser.error(Errors.Message.DECL_NAME, name);
		}

		return obj;
	}

	public void openScope() {
		curScope = new Scope(curScope);
		curLevel += 1;
	}

	public void closeScope() {
		curScope = curScope.outer();
		curLevel -= 1;
	}
}
