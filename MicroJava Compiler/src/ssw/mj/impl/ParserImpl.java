package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import java.util.EnumSet;
import java.util.Map;


/**
 * Disclaimer
 * <p>
 * This code is awfully ugly, because I stopped worrying and started loving the Trial&Error approach
 * <p>
 * <p>
 * Please feel free to tell me <EVERYTHING> that doesn't look liked it is supposed to be.
 */


//TODO: FIX THE UGLY VARIABLE NAMES

public final class ParserImpl extends Parser {

	private int errDist = 3;

	private EnumSet<Token.Kind> exprSet = EnumSet.of(Token.Kind.minus, Token.Kind.ident, Token.Kind.number, Token.Kind.charConst, Token.Kind.new_, Token.Kind.lpar);
	private EnumSet<Token.Kind> statementSet = EnumSet.of(Token.Kind.ident, Token.Kind.if_, Token.Kind.while_, Token.Kind.break_, Token.Kind.compare_, Token.Kind.return_, Token.Kind.read, Token.Kind.print, Token.Kind.lbrace, Token.Kind.semicolon);


	public ParserImpl(Scanner scanner) {
		super(scanner);
	}


	@Override
	public void parse() {
		scan();
		MicroJava();
		check(Token.Kind.eof);
	}


	private void scan() {
		t = la;
		la = scanner.next();
		sym = la.kind;

		errDist++;
	}

	private void check(Token.Kind expected) {
		if (sym == expected) {
			scan();
		} else {
			error(Errors.Message.TOKEN_EXPECTED, expected.label());
		}
	}

	private boolean checkExpr() {
		return exprSet.contains(sym);
	}


	private boolean checkStatement() {
		return statementSet.contains(sym);
	}


	@Override
	public void error(Errors.Message msg, Object... msgParams) {
		if (errDist >= 3) {
			scanner.errors.error(la.line, la.col, msg, msgParams);
		}
		errDist = 0;
	}


	private void recoverStat() {
		error(Errors.Message.INVALID_STAT);

		while ((!checkStatement() || sym == Token.Kind.ident) && sym != Token.Kind.rbrace && sym != Token.Kind.semicolon && sym != Token.Kind.eof) {
			scan();
		}
		errDist = 0;
	}

	private void recoverTillRBrace() {
		while (sym != Token.Kind.rbrace && sym != Token.Kind.eof) {
			scan();
		}
		errDist = 0;
	}

	private void recoverAfterMessedUpVarDecl() {
		while (t.kind != Token.Kind.semicolon && sym != Token.Kind.eof) {
			scan();
		}
		errDist = 0;
	}

	private void recoverTillLBrace() {
		while (sym != Token.Kind.lbrace && sym != Token.Kind.eof) {
			scan();
		}
		errDist = 0;
	}

	private void MicroJava() {
		check(Token.Kind.program);
		check(Token.Kind.ident);
		Obj prog = tab.insert(Obj.Kind.Prog, t.str, Tab.noType);
		tab.openScope();
		while (sym == Token.Kind.final_ || sym == Token.Kind.class_ || sym == Token.Kind.ident) {
			switch (sym) {
				case final_:
					ConstDecl();
					break;
				case class_:
					ClassDecl();
					break;
				case ident:
					code.dataSize += VarDecl();
					break;
			}
		}

		if (tab.curScope.nVars() > MAX_GLOBALS) {
			error(Errors.Message.TOO_MANY_GLOBALS);
		}


		if (sym != Token.Kind.lbrace) {
			error(Errors.Message.INVALID_DECL);
			recoverTillLBrace();
		}


		check(Token.Kind.lbrace);

		while (sym != Token.Kind.rbrace && sym != Token.Kind.eof && t.kind != Token.Kind.eof) {
			MethodDecl();
		}

		prog.locals = tab.curScope.locals();

		Obj main = tab.find("main");
		if (main == tab.noObj || main.kind != Obj.Kind.Meth)
			error(Errors.Message.METH_NOT_FOUND, "main");

		tab.closeScope();

		check(Token.Kind.rbrace);
	}

	private void ConstDecl() {
		check(Token.Kind.final_);
		StructImpl type = Type();
		check(Token.Kind.ident);
		Obj con = new Obj(Obj.Kind.Con, t.str, type);
		check(Token.Kind.assign);
		if (sym == Token.Kind.number) {
			if (!type.kind.equals(Tab.intType.kind)) {
				error(Errors.Message.CONST_TYPE);
			}
			scan();
			con.val = t.val;
		} else if (sym == Token.Kind.charConst) {
			if (!type.kind.equals(Tab.charType.kind)) {
				error(Errors.Message.CONST_TYPE);
			}
			scan();
			con.val = t.val;
		} else {
			error(Errors.Message.CONST_DECL);
		}
		tab.curScope.insert(con);
		check(Token.Kind.semicolon);
	}

	private int VarDecl() {
		StructImpl type = Type();
		check(Token.Kind.ident);
		tab.insert(Obj.Kind.Var, t.str, type);
		int size =
				type == Tab.charType ? 1 : type == Tab.intType ? 4 : 0;//TODO: wrong
		while (sym == Token.Kind.comma) {
			scan();
			check(Token.Kind.ident);
			tab.insert(Obj.Kind.Var, t.str, type);

			size +=
					type == Tab.charType ? 1 : type == Tab.intType ? 4 : 0;
		}
		check(Token.Kind.semicolon);
		return size;
	}

	private void ClassDecl() {
		check(Token.Kind.class_);
		check(Token.Kind.ident);
		Obj clazz = tab.insert(Obj.Kind.Type, t.str, new StructImpl(Struct.Kind.Class));
		check(Token.Kind.lbrace);
		tab.openScope();
		while (sym == Token.Kind.ident) {
			VarDecl();
		}
		if (t.kind != Token.Kind.semicolon && tab.curScope.nVars() > 0) {
			recoverAfterMessedUpVarDecl();
		}
		if (tab.curScope.nVars() > MAX_FIELDS) {
			error(Errors.Message.TOO_MANY_FIELDS);
		}
		clazz.type.fields = tab.curScope.locals();
		tab.closeScope();
		check(Token.Kind.rbrace);
	}

	private Obj MethodDecl() {
		StructImpl type;
		if (sym == Token.Kind.void_ || sym == Token.Kind.ident) {
			if (sym == Token.Kind.void_) {
				scan();
				type = Tab.noType;
			} else {
				type = Type();
			}
		} else {
			error(Errors.Message.METH_DECL);
			recoverTillRBrace();
			scan();
			return tab.noObj;
		}
		check(Token.Kind.ident);
		Obj meth = tab.insert(Obj.Kind.Meth, t.str, type);
		meth.adr = code.pc;
		check(Token.Kind.lpar);

		tab.openScope();

		if (sym == Token.Kind.ident) {
			FormPars();
		}

		meth.nPars = tab.curScope.nVars();
		check(Token.Kind.rpar);
		if (meth.name.equals("main")) {
			if (meth.nPars != 0) {
				error(Errors.Message.MAIN_WITH_PARAMS);
			}
			if (!meth.type.equals(Tab.noType)) {
				error(Errors.Message.MAIN_NOT_VOID);
			}
			code.mainpc = code.pc;
		}
		while (sym == Token.Kind.ident) {
			VarDecl();
			if (t.kind != Token.Kind.semicolon)
				recoverAfterMessedUpVarDecl();
		}
		if (tab.curScope.nVars() > MAX_LOCALS) {
			error(Errors.Message.TOO_MANY_LOCALS);
		}

		code.put(Code.OpCode.enter);
		code.put(meth.nPars);
		code.put(tab.curScope.nVars());

		meth.locals = tab.curScope.locals();
		Block();


		if (meth.type == Tab.noType) {
			code.put(Code.OpCode.exit);
			code.put(Code.OpCode.return_);
		} else { // end of function reached without a return statement
			code.put(Code.OpCode.trap);
			code.put(1);
		}
		tab.closeScope();

		return meth;
	}

	private void FormPars() {
		StructImpl type = Type();

		check(Token.Kind.ident);

		tab.insert(Obj.Kind.Var, t.str, type);

		while (sym == Token.Kind.comma) {
			scan();
			type = Type();
			check(Token.Kind.ident);
			tab.insert(Obj.Kind.Var, t.str, type);
		}
	}

	private StructImpl Type() {
		check(Token.Kind.ident);

		Obj o = tab.find(t.str);

		if (o.kind != Obj.Kind.Type) {
			if (o == tab.noObj) {
				error(Errors.Message.NOT_FOUND, "type");
			} else {
				error(Errors.Message.NO_TYPE);
			}
		}

		StructImpl type = o.type;

		if (sym == Token.Kind.lbrack) {
			scan();
			//retval is array type
			check(Token.Kind.rbrack);
			type = new StructImpl(type);
		}

		return type;
	}

	private void Block() {
		check(Token.Kind.lbrace);

		if (t.kind == Token.Kind.lbrace) {
			while (sym != Token.Kind.eof && sym != Token.Kind.rbrace) {
				Statement();
			}
		}

		check(Token.Kind.rbrace);
	}

	private void Statement() {
		if (!checkStatement()) {
			recoverStat();
		}
		switch (sym) {
			case ident:
				//Designator ( Assignop Expr | ActPars | "++" | "--" ) ";"
				Operand x = Designator();
				switch (sym) {
					case plusas:
					case minusas:
					case timesas:
					case slashas:
					case remas:
						Code.OpCode code_ = SpecialAssignmentStuff();
						if (!x.kind.equals(Operand.Kind.Local) && !x.kind.equals(Operand.Kind.Stack))
							error(Errors.Message.NO_VAR);

						Operand y = Expr();
						code.load(y);
						code.put(code_);
						if (!y.type.assignableTo(x.type))
							error(Errors.Message.NO_INT_OP);
						code.assign(x, y);
						break;
					case assign:
						scan();
						Operand yass = Expr();

						if (!yass.type.assignableTo(x.type)) {
							error(Errors.Message.INCOMP_TYPES);
						} else {
							code.assign(x, yass);
						}
						break;
					case lpar:
						ActPars(x);
						code.put(Code.OpCode.call);
						code.put2(x.adr - (code.pc - 1));
						if (x.type != Tab.noType)
							code.put(Code.OpCode.pop);
						break;
					case pplus:
						if (x.type != Tab.intType)
							error(Errors.Message.NO_INT);

						switch (x.kind) {
							case Local:
								code.put(Code.OpCode.inc);
								code.put(x.adr);
								code.put(1);
								break;
							case Static://
								code.load(x);
								code.put(Code.OpCode.const_1);
								code.put(Code.OpCode.add);
								x.kind = Operand.Kind.Static;
								code.generalStoreOperations(x);
								break;
							case Elem://arr
								code.load(x);
								code.put(Code.OpCode.const_1);
								code.put(Code.OpCode.add);
								x.kind = Operand.Kind.Elem;
								code.generalStoreOperations(x);
								break;
							case Fld://class
								code.load(x);
								code.put(Code.OpCode.const_1);
								code.put(Code.OpCode.add);
								x.kind = Operand.Kind.Fld;
								code.generalStoreOperations(x);
								break;
							default:
								error(Errors.Message.NO_VAR);
						}
						scan();
						break;
					case mminus:
						if (x.type != Tab.intType)
							error(Errors.Message.NO_INT);
						switch (x.kind) {
							case Local:
								code.put(Code.OpCode.inc);
								code.put(x.adr);
								code.put(-1);
								break;
							case Static://
								code.load(x);
								code.put(Code.OpCode.const_m1);
								code.put(Code.OpCode.add);
								x.kind = Operand.Kind.Static;
								code.generalStoreOperations(x);
								break;
							case Elem://arr
								code.load(x);
								code.put(Code.OpCode.const_m1);
								code.put(Code.OpCode.add);
								x.kind = Operand.Kind.Elem;
								code.generalStoreOperations(x);
								break;
							case Fld://class
								code.load(x);
								code.put(Code.OpCode.const_m1);
								code.put(Code.OpCode.add);
								x.kind = Operand.Kind.Fld;
								code.generalStoreOperations(x);
								break;
							default:
								error(Errors.Message.NO_VAR);
						}
						scan();
						break;
					default:
						error(Errors.Message.DESIGN_FOLLOW);
				}
				check(Token.Kind.semicolon);
				break;
			case if_:
				scan();
				check(Token.Kind.lpar);
				Condition();
				check(Token.Kind.rpar);
				Statement();

				if (sym == Token.Kind.else_) {
					scan();
					Statement();
				}
				break;
			case while_:
				scan();
				check(Token.Kind.lpar);
				Operand cond = Condition();
				check(Token.Kind.rpar);
				Statement();
				break;
			case break_:
				scan();
				check(Token.Kind.semicolon);
				break;
			case compare_:
				scan();
				check(Token.Kind.lpar);
				Operand xcomp = Expr();
				if (xcomp.type != Tab.intType) {
					error(Errors.Message.NO_INT_OP);
				}
				check(Token.Kind.comma);
				Operand ycomp = Expr();
				if (ycomp.type != Tab.intType) {
					error(Errors.Message.NO_INT_OP);
				}
				check(Token.Kind.rpar);
				Block();
				Block();
				Block();
				break;
			case return_:
				scan();
				if (checkExpr()) {
					Operand ret = Expr();
					code.load(ret);
					code.put(Code.OpCode.return_);
				}

				check(Token.Kind.semicolon);
				break;
			case read:
				scan();
				check(Token.Kind.lpar);
				Operand y = Designator();
				if (y.type != Tab.intType && y.type != Tab.charType)
					error(Errors.Message.READ_VALUE);
				code.put(Code.OpCode.read);
				code.generalStoreOperations(y);
				check(Token.Kind.rpar);
				break;
			case print:
				scan();
				check(Token.Kind.lpar);
				Operand y1 = Expr();

				if (y1.type != Tab.intType && y1.type != Tab.charType)
					error(Errors.Message.PRINT_VALUE);


				int length = 0;

				if (sym == Token.Kind.comma) {
					scan();
					check(Token.Kind.number);
					length = Integer.parseInt(t.str);
				}


				code.load(y1);
				code.load(new Operand(length));
				if (y1.type == Tab.intType) {
					code.put(Code.OpCode.print);
				} else {
					code.put(Code.OpCode.bprint);
				}

				check(Token.Kind.rpar);
				check(Token.Kind.semicolon);
				break;
			case lbrace:
				Block();
				break;
			case semicolon:
				scan();
				break;
		}
	}

	private Code.OpCode SpecialAssignmentStuff() {
		switch (sym) {
			case plusas:
				scan();
				return Code.OpCode.add;
			case minusas:
				scan();
				return Code.OpCode.sub;
			case timesas:
				scan();
				return Code.OpCode.mul;
			case slashas:
				scan();
				return Code.OpCode.div;
			case remas:
				scan();
				return Code.OpCode.rem;
			default:
				error(Errors.Message.ASSIGN_OP);
				return null;
		}
	}

	private void ActPars(Operand m) {
		Operand ap;
		check(Token.Kind.lpar);
		if (m.kind != Operand.Kind.Meth) {
			error(Errors.Message.NO_METH);
			m.obj = tab.noObj;
		}
		int aPars = 0;
		int fPars = m.obj.nPars;

		if (checkExpr()) {
			ap = Expr();
			code.load(ap);
			aPars++;

			for (Map.Entry<String, Obj> tok : m.obj.locals.entrySet())
				if (!ap.type.assignableTo(tok.getValue().type))
					error(Errors.Message.INCOMP_TYPES);


			while (sym == Token.Kind.comma) {
				scan();
				ap = Expr();
				code.load(ap);
				aPars++;

				for (Map.Entry<String, Obj> tok : m.obj.locals.entrySet())
					if (!ap.type.assignableTo(tok.getValue().type))
						error(Errors.Message.INCOMP_TYPES);
			}
		}
		if (aPars > fPars)
			error(Errors.Message.MORE_ACTUAL_PARAMS);
		else if (aPars < fPars)
			error(Errors.Message.LESS_ACTUAL_PARAMS);
		check(Token.Kind.rpar);
	}

	private Operand Condition() {
		Operand x = CondTerm();
		while (sym == Token.Kind.or) {
			scan();
			code.tJump(x);
			x.fLabel.here();
			Operand y = CondTerm();
			x.op = y.op;
			x.fLabel = y.fLabel;
		}
		return x;
	}

	private Operand CondTerm() {
		Operand x = CondFact();
		while (sym == Token.Kind.and) {
			scan();
			code.fJump(x);
			Operand y = CondFact();
			x.op = y.op;
		}
		return x;
	}

	private Operand CondFact() {
		Operand x = Expr();
		code.load(x);
		Code.CompOp cmpOp = Relop();
		Operand y = Expr();
		code.load(y);
		if (!x.type.compatibleWith(y.type))
			error(Errors.Message.INCOMP_TYPES);
		if (x.type.isRefType() && cmpOp != Code.CompOp.eq && cmpOp != Code.CompOp.ne)
			error(Errors.Message.INCOMP_TYPES);
		return new Operand(cmpOp, code);
	}

	private Code.CompOp Relop() {
		switch (sym) {
			case eql:
				scan();
				return Code.CompOp.eq;
			case neq:
				scan();
				return Code.CompOp.ne;
			case gtr:
				scan();
				return Code.CompOp.gt;
			case geq:
				scan();
				return Code.CompOp.ge;
			case lss:
				scan();
				return Code.CompOp.lt;
			case leq:
				scan();
				return Code.CompOp.le;
			default:
				error(Errors.Message.REL_OP);
				return Code.CompOp.eq;
		}
	}

	private Operand Expr() {

		Operand x;
		if (sym == Token.Kind.minus) {
			scan();
			x = Term();
			if (x.type != Tab.intType)
				error(Errors.Message.NO_INT_OP);
			if (x.kind == Operand.Kind.Con)
				x.val = -x.val;
			else {
				code.load(x);
				code.put(Code.OpCode.neg);
			}
		} else {
			x = Term();
		}

		while (sym == Token.Kind.plus || sym == Token.Kind.minus) {
			code.load(x);
			Code.OpCode code_ = Addop();
			Operand y = Term();
			code.load(y);
			if (x.type != Tab.intType || y.type != Tab.intType)
				error(Errors.Message.NO_INT_OP);
			code.put(code_);
		}

		return x;
	}

	private Operand Term() {
		Operand x = Factor();
		while (sym == Token.Kind.times || sym == Token.Kind.slash || sym == Token.Kind.rem) {
			code.load(x);
			Code.OpCode code_ = Mulop();
			Operand y = Factor();
			code.load(y);
			if (x.type == Tab.intType && y.type == Tab.intType)
				code.put(code_);
			else
				error(Errors.Message.NO_INT_OP);
		}
		return x;
	}

	private Operand Factor() {
		Operand x;
		switch (sym) {
			case ident:
				x = Designator();
				if (sym == Token.Kind.lpar) {
					ActPars(x);
					if (x.type == Tab.noType) {
						error(Errors.Message.INVALID_CALL);
					}
					if (x.obj == tab.ordObj || x.obj == tab.chrObj) ; // nothing
					else if (x.obj == tab.lenObj) {
						code.put(Code.OpCode.arraylength);
					} else {
						code.put(Code.OpCode.call);
						code.put2(x.adr - (code.pc - 1));
					}
					x.kind = Operand.Kind.Stack;
				}
				break;
			case number:
				x = new Operand(la.val);
				scan();
				break;
			case charConst:
				x = new Operand(la.val);
				x.type = Tab.charType;
				scan();
				break;
			case new_:
				scan();
				check(Token.Kind.ident);
				Obj obj = tab.find(t.str);
				StructImpl type = obj.type;


				if (sym == Token.Kind.lbrack) {
					if (obj.kind != Obj.Kind.Type)
						error(Errors.Message.NO_TYPE);
					scan();
					x = Expr();
					if (x.type != Tab.intType)
						error(Errors.Message.ARRAY_SIZE);
					check(Token.Kind.rbrack);
					code.load(x);
					code.put(Code.OpCode.newarray);
					code.put(type == Tab.charType ? 0 : 1);
					type = new StructImpl(Struct.Kind.Arr, type);
				} else {
					if (obj.kind != Obj.Kind.Type) {
						error(Errors.Message.NO_TYPE);
					} else if (type.kind != Struct.Kind.Class) {
						error(Errors.Message.NO_CLASS_TYPE);
					}
					code.put(Code.OpCode.new_);
					code.put2(type.fields.size());
				}

				x = new Operand(type);
				break;
			case lpar:
				scan();
				x = Expr();
				check(Token.Kind.rpar);
				break;
			default:
				error(Errors.Message.INVALID_FACT);
				x = new Operand(Tab.nullType);
		}
		return x;
	}

	private Operand Designator() {
		check(Token.Kind.ident);
		Obj o = tab.find(t.str);
		Operand x = new Operand(o, this);
		if (o == tab.noObj)
			error(Errors.Message.NOT_FOUND, t.str);


		while (sym == Token.Kind.period || sym == Token.Kind.lbrack) {
			if (sym == Token.Kind.period) {
				if (x.type.kind != Struct.Kind.Class)
					error(Errors.Message.NO_CLASS);

				scan();
				if (x.type.kind == Struct.Kind.Class) {
					code.load(x);
					check(Token.Kind.ident);
					Obj fld = tab.findField(t.str, x.type);
					if (fld == tab.noObj)
						error(Errors.Message.NO_FIELD, t.str);
					x.kind = Operand.Kind.Fld;
					x.adr = fld.adr;
					x.type = fld.type;
				}
			} else {
				code.load(x);
				scan();
				Operand y = Expr();
				if (x.type.kind != Struct.Kind.Arr)
					error(Errors.Message.NO_ARRAY);
				if (y.type != Tab.intType)
					error(Errors.Message.ARRAY_INDEX);
				code.load(y);
				x.kind = Operand.Kind.Elem;
				x.type = x.type.elemType;
				check(Token.Kind.rbrack);
			}
		}
		return x;
	}

	private Code.OpCode Addop() {
		switch (sym) {
			case plus:
				scan();
				return Code.OpCode.add;
			case minus:
				scan();
				return Code.OpCode.sub;
			default:
				error(Errors.Message.ADD_OP);
				return null;
		}
	}

	private Code.OpCode Mulop() {
		switch (sym) {
			case times:
				scan();
				return Code.OpCode.mul;
			case slash:
				scan();
				return Code.OpCode.div;
			case rem:
				scan();
				return Code.OpCode.rem;
			default:
				error(Errors.Message.MUL_OP);
				return null;
		}
	}

}
