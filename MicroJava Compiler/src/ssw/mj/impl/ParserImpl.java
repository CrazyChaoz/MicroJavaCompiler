package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import java.util.EnumSet;



/**
 * Disclaimer
 *
 * This code is awfully ugly, because I stopped worrying and started loving the Trial&Error approach
 *
 *
 * Please feel free to tell me <EVERYTHING> that doesn't look liked it is supposed to be.
 *
 *
 * */
public final class ParserImpl extends Parser {

	private int errDist = 3;

	private EnumSet<Token.Kind> exprSet = EnumSet.of(Token.Kind.minus, Token.Kind.ident, Token.Kind.number, Token.Kind.charConst, Token.Kind.new_, Token.Kind.lpar);
	private EnumSet<Token.Kind> statementSet = EnumSet.of(Token.Kind.ident, Token.Kind.if_, Token.Kind.while_,Token.Kind.break_,Token.Kind.compare_,Token.Kind.return_,Token.Kind.read,Token.Kind.print,Token.Kind.lbrace,Token.Kind.semicolon);


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
					VarDecl();
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
		}
		if (t.kind != Token.Kind.semicolon) {
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
		}
		while (sym == Token.Kind.ident) {
			VarDecl();
			if (t.kind != Token.Kind.semicolon)
				recoverAfterMessedUpVarDecl();
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
				error(Errors.Message.ASSIGN_OP);

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
