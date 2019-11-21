package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

public final class ParserImpl extends Parser {

	private int errDist = 3;

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

	@Override
	public void error(Errors.Message msg, Object... msgParams) {
		if (errDist >= 3) {
			scanner.errors.error(la.line, la.col, msg, msgParams);
		}
		errDist = 0;
	}

	//Helper Methods

	//TODO: impl set Expr
	private boolean checkExpr() {
		return sym == Token.Kind.minus
				|| sym == Token.Kind.ident
				|| sym == Token.Kind.number
				|| sym == Token.Kind.charConst
				|| sym == Token.Kind.new_
				|| sym == Token.Kind.lpar;
	}


	//TODO: impl set Statement
	private boolean checkStatement() {
		return sym == Token.Kind.ident
				|| sym == Token.Kind.if_
				|| sym == Token.Kind.while_
				|| sym == Token.Kind.break_
				|| sym == Token.Kind.compare_
				|| sym == Token.Kind.return_
				|| sym == Token.Kind.read
				|| sym == Token.Kind.print
				|| sym == Token.Kind.lbrace
				|| sym == Token.Kind.semicolon;
	}

	private void recoverStat() {
		error(Errors.Message.INVALID_STAT);
		while ((!checkStatement() || sym == Token.Kind.ident) && sym != Token.Kind.rbrace && sym != Token.Kind.eof) {
			scan();
		}
		errDist = 0;
	}

	private void recoverDecl() {
		//follow: rbrace, block, lpar, ident
		while (/*sym!=Token.Kind.ident &&*/ sym != Token.Kind.eof) {
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
					VarDecl();
					break;
			}
		}

		if (tab.curScope.nVars() > MAX_GLOBALS) {
			error(Errors.Message.TOO_MANY_GLOBALS);
		}

		check(Token.Kind.lbrace);
		while (sym == Token.Kind.ident || sym == Token.Kind.void_) {
			MethodDecl();
		}
		if(sym!= Token.Kind.rbrace) {
			error(Errors.Message.INVALID_DECL);
			recoverDecl();
		}

		prog.locals = tab.curScope.locals();
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
			if (!type.kind.equals(Tab.intType.kind))
				error(Errors.Message.CONST_TYPE);
			scan();
			con.val = t.val;
		} else if (sym == Token.Kind.charConst) {
			if (!type.kind.equals(Tab.charType.kind))
				error(Errors.Message.CONST_TYPE);
			scan();
			con.val = t.val;
		} else {
			error(Errors.Message.CONST_DECL);
		}
		tab.curScope.insert(con);
		check(Token.Kind.semicolon);
	}

	private void VarDecl() {
		StructImpl type = Type();
		check(Token.Kind.ident);
		tab.insert(Obj.Kind.Var, t.str, type);
		while (sym == Token.Kind.comma) {
			scan();
			check(Token.Kind.ident);
			tab.insert(Obj.Kind.Var, t.str, type);
		}
		check(Token.Kind.semicolon);
	}

	private void ClassDecl() {
		check(Token.Kind.class_);
		check(Token.Kind.ident);
		Obj clazz = tab.insert(Obj.Kind.Type, t.str, new StructImpl(Struct.Kind.Class));
		check(Token.Kind.lbrace);
		tab.openScope();
		while (sym == Token.Kind.ident) {
			VarDecl();
			if (t.kind != Token.Kind.semicolon)
				recoverDecl();
		}

//		System.out.println("nvars = " + tab.curScope.nVars());
//		System.out.println("maxfields = " + MAX_FIELDS);

		if (tab.curScope.nVars() > MAX_FIELDS) {
			error(Errors.Message.TOO_MANY_FIELDS);
		}
		clazz.type.fields = tab.curScope.locals();
		tab.closeScope();
		check(Token.Kind.rbrace);
	}

	private Obj MethodDecl() {
		StructImpl type = Tab.noType;

		if (sym == Token.Kind.void_ || sym == Token.Kind.ident) {
			if (sym == Token.Kind.void_) {
				scan();
				type = Tab.noType;
				//void method -> no return
			} else {
				type = Type();
				//typed
			}
		} else {
			recoverDecl();
		}

		check(Token.Kind.ident);
		Obj meth = tab.insert(Obj.Kind.Meth, t.str, type);
		meth.adr = code.pc;

		check(Token.Kind.lpar);


		tab.openScope();


		if (sym == Token.Kind.ident)
			FormPars();

		meth.nPars = tab.curScope.nVars();

		check(Token.Kind.rpar);
		if (meth.name.equals("main")) {
			if (meth.nPars != 0) {
				error(Errors.Message.MAIN_WITH_PARAMS);
			}

			if (!meth.type.equals(Tab.noType)) {
				error(Errors.Message.MAIN_NOT_VOID);
			}
		}
		while (sym == Token.Kind.ident) {
			VarDecl();
			if (t.kind != Token.Kind.semicolon)
				recoverDecl();
		}

		if (tab.curScope.nVars() > MAX_LOCALS) {
			error(Errors.Message.TOO_MANY_LOCALS);
		}

		meth.locals = tab.curScope.locals();

		Block();

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

		while (checkStatement())
			Statement();

		check(Token.Kind.rbrace);
	}


	private void Statement() {
		if (!checkStatement()) {
			recoverStat();
		}

		switch (sym) {
			case ident:
				//Designator ( Assignop Expr | ActPars | "++" | "--" ) ";"
				Designator();
				switch (sym) {
					case assign:
					case plusas:
					case minusas:
					case timesas:
					case slashas:
					case remas:
						Assignop();
						Expr();
						break;
					case lpar:
						ActPars();
						break;
					case pplus:
						scan();
						break;
					case mminus:
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
				Condition();
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
				Expr();
				check(Token.Kind.comma);
				Expr();
				check(Token.Kind.rpar);
				Block();
				Block();
				Block();
				break;
			case return_:
				scan();
				if (checkExpr())
					Expr();
				check(Token.Kind.semicolon);
				break;
			case read:
				scan();
				check(Token.Kind.lpar);
				Designator();
				check(Token.Kind.rpar);
				break;
			case print:
				scan();
				check(Token.Kind.lpar);
				Expr();
				if (sym == Token.Kind.comma) {
					scan();
					check(Token.Kind.number);
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

	private void Assignop() {
		switch (sym) {
			case assign:
				scan();
				break;
			case plusas:
				scan();
				break;
			case minusas:
				scan();
				break;
			case timesas:
				scan();
				break;
			case slashas:
				scan();
				break;
			case remas:
				scan();
				break;
			default:
				throw new Errors.PanicMode();

		}
	}

	private void ActPars() {
		check(Token.Kind.lpar);
		if (checkExpr()) {
			Expr();
			while (sym == Token.Kind.comma) {
				scan();
				Expr();
			}
		}
		check(Token.Kind.rpar);
	}

	private void Condition() {
		CondTerm();
		while (sym == Token.Kind.or) {
			scan();
			CondTerm();
		}
	}

	private void CondTerm() {
		CondFact();
		while (sym == Token.Kind.and) {
			scan();
			CondFact();
		}
	}

	private void CondFact() {
		Expr();
		Relop();
		Expr();
	}

	private void Relop() {
		switch (sym) {
			case eql:
				scan();
				break;
			case neq:
				scan();
				break;
			case gtr:
				scan();
				break;
			case geq:
				scan();
				break;
			case lss:
				scan();
				break;
			case leq:
				scan();
				break;
			default:
				error(Errors.Message.REL_OP);
		}
	}

	private void Expr() {
		if (sym == Token.Kind.minus)
			scan();
		Term();
		while (sym == Token.Kind.plus || sym == Token.Kind.minus) {
			Addop();
			Term();
		}
	}

	private void Term() {
		Factor();
		while (sym == Token.Kind.times || sym == Token.Kind.slash || sym == Token.Kind.rem) {
			Mulop();
			Factor();
		}
	}

	private void Factor() {
		switch (sym) {
			case ident:
				Designator();
				if (sym == Token.Kind.lpar)
					ActPars();
				break;
			case number:
				scan();
				break;
			case charConst:
				scan();
				break;
			case new_:
				scan();
				check(Token.Kind.ident);
				if (sym == Token.Kind.lbrack) {
					scan();
					Expr();
					check(Token.Kind.rbrack);
				}
				break;
			case lpar:
				scan();
				Expr();
				check(Token.Kind.rpar);
				break;
			default:
				error(Errors.Message.INVALID_FACT);
		}

	}

	private void Designator() {
		check(Token.Kind.ident);
		while (sym == Token.Kind.period || sym == Token.Kind.lbrack) {
			if (sym == Token.Kind.period) {
				scan();
				check(Token.Kind.ident);
			} else {
				scan();
				Expr();
				check(Token.Kind.rbrack);
			}
		}
	}

	private void Addop() {
		switch (sym) {
			case plus:
				scan();
				break;
			case minus:
				scan();
				break;
			default:
				error(Errors.Message.ADD_OP);
		}
	}

	private void Mulop() {
		switch (sym) {
			case times:
				scan();
				break;
			case slash:
				scan();
				break;
			case rem:
				scan();
				break;
			default:
				error(Errors.Message.MUL_OP);
		}
	}

}
