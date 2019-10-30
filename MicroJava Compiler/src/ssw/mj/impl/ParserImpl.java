package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;

public final class ParserImpl extends Parser {
	private Token t; // zuletzt erkanntes Token
	private Token la; // look ahead token
	private Token.Kind sym;
	private Scanner scanner;


	private void scan() {
		t = la;
		la = scanner.next();
		sym = la.kind;
	}

	private void check(Token.Kind expected) {
		if (sym == expected) scan(); // erkannt, daher weiterlesen
		else error(expected.label() + " expected");
	}

	private void error(String msg) {
		System.out.println("line " + la.line + ", col " + la.col + ": " + msg);
		throw new Errors.PanicMode();
	}

	public ParserImpl(Scanner scanner) {
		super(scanner);
		this.scanner = scanner;
	}


	@Override
	public void parse() {
		scan();
		MicroJava();
		check(Token.Kind.eof);
	}

	private void MicroJava() {
		check(Token.Kind.program);
		check(Token.Kind.ident);
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
				default:
					error("error during program decleration (program should not reach this)");
			}
		}
		check(Token.Kind.lbrace);
		while (sym == Token.Kind.ident || sym == Token.Kind.void_) {
			MethodDecl();
		}
		check(Token.Kind.rbrace);
	}

	private void ConstDecl() {
		check(Token.Kind.final_);
		Type();
		check(Token.Kind.ident);
		check(Token.Kind.assign);
		if (sym == Token.Kind.number) {
			scan();
		} else if (sym == Token.Kind.charConst) {
			scan();
		} else error("assignment error");
		check(Token.Kind.semicolon);
	}

	private void VarDecl() {
		Type();
		check(Token.Kind.ident);
		while (sym == Token.Kind.comma) {
			scan();
			check(Token.Kind.ident);
		}
		check(Token.Kind.semicolon);
	}

	private void ClassDecl() {
		check(Token.Kind.class_);
		check(Token.Kind.ident);
		check(Token.Kind.lbrace);
		while (sym == Token.Kind.ident) {
			VarDecl();
		}
		check(Token.Kind.rbrace);
	}

	private void MethodDecl() {
		if (sym == Token.Kind.void_) {
			scan();
			//void method -> no return
		} else if (sym == Token.Kind.ident) {
			Type();
			//typed
		} else error("method type error");

		check(Token.Kind.ident);
		check(Token.Kind.lpar);

		if (sym == Token.Kind.ident)
			FormPars();

		check(Token.Kind.rpar);

		while (sym == Token.Kind.ident) {
			VarDecl();
		}

		Block();
	}

	private void FormPars() {
		Type();
		check(Token.Kind.ident);
		while (sym == Token.Kind.comma) {
			scan();
			Type();
			check(Token.Kind.ident);
		}

	}

	private void Type() {
		check(Token.Kind.ident);
		if (sym == Token.Kind.lbrack) {
			scan();
			check(Token.Kind.rbrack);
		}

	}

	private void Block() {
		check(Token.Kind.lbrace);
		//TODO: impl set Statement
		while (sym == Token.Kind.ident
				|| sym == Token.Kind.if_
				|| sym == Token.Kind.while_
				|| sym == Token.Kind.break_
				|| sym == Token.Kind.compare_
				|| sym == Token.Kind.return_
				|| sym == Token.Kind.read
				|| sym == Token.Kind.print
				|| sym == Token.Kind.lbrace
				|| sym == Token.Kind.semicolon)
			Statement();

		check(Token.Kind.rbrace);
	}

	private void Statement() {
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
				//TODO: impl set Expr
				//if(Expr)
				//Expr();
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
			default:
				error("wrong block opening");
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
		//TODO: impl set Expr
//		if(Expr){
//		Expr();
//		while (sym == Token.Kind.comma) {
//			scan();
//			Expr();
//		}
//		}
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
				throw new Errors.PanicMode();
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
				error("invalid start of factor: identifier, number, character constant, new or ( expected");
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
				error("ADDOP");
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
				throw new Errors.PanicMode();
		}
	}

}
