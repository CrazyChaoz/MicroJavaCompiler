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


	public ParserImpl(Scanner scanner) {
		super(scanner);
		this.scanner = scanner;
	}

	@Override
	public void parse() {
		// TODO
	}

	private void MicroJava() {
	}

	private void ConstDecl() {
	}

	private void VarDecl() {
	}

	private void ClassDecl() {
	}

	private void MethodDecl() {
	}

	private void FormPars() {
	}

	private void Type() {
	}

	private void Block() {
	}

	private void Statement() {
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
	}

	private void Condition() {
	}

	private void CondTerm() {
	}

	private void CondFact() {
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
	}

	private void Term() {
	}

	private void Factor() {
	}

	private void Designator() {
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
				throw new Errors.PanicMode();
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
