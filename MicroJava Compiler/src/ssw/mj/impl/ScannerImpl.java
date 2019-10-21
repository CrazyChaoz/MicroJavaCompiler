package ssw.mj.impl;

import ssw.mj.Scanner;
import ssw.mj.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.Token.Kind.*;

public final class ScannerImpl extends Scanner {

	// TODO Exercise 2: implementation of scanner

	final char eofCh = (char) -1;   // Zeichen, das bei eof geliefert wird
	Reader in;                      // Eingabestrom
	char ch;                        // naechstes noch unverarbeitetes Zeichen
	private int line, col;                  // Zeile und Spalte des Zeichens ch


	public ScannerImpl(Reader r) {
		super(r);
		init(r);
	}

	public void init(Reader r) {
		in = r;
		line = 1;
		col = 0;
		nextCh(); // liest erstes Zeichen, speichert es in ch und erhöht col auf 1
	}

	private void nextCh() {
		try {
			ch = (char) in.read();
			col++;
			if (ch == '\n') {
				line++;
				col = 0;
			}
		} catch (IOException e) {
			ch = eofCh;
		}
	}

	public Token next() {
		while (ch <= ' ' && ch != '\0')
			nextCh(); // skip blanks, tabs, eols


		Token t = new Token(null, line, col);

		switch (ch) {
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
			case 'g':
			case 'h':
			case 'i':
			case 'j':
			case 'k':
			case 'l':
			case 'm':
			case 'n':
			case 'o':
			case 'p':
			case 'q':
			case 'r':
			case 's':
			case 't':
			case 'u':
			case 'v':
			case 'w':
			case 'x':
			case 'y':
			case 'z':
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
			case 'R':
			case 'S':
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
				readName(t);
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				readNumber(t);
				break;
			case ';':
				nextCh();
				t.kind = semicolon;
				break;
			case '.':
				nextCh();
				t.kind = period;
				break;
			case '\'':
				readCharConst(t);
				break;
			case '{':
				nextCh();
				t.kind = lbrace;
				break;
			case '}':
				nextCh();
				t.kind = rbrace;
				break;
			case '[':
				nextCh();
				t.kind = lbrack;
				break;
			case ']':
				nextCh();
				t.kind = rbrack;
				break;
			case '(':
				nextCh();
				t.kind = lpar;
				break;
			case ')':
				nextCh();
				t.kind = rpar;
				break;
			case ',':
				nextCh();
				t.kind = comma;
				break;
			case eofCh:
				//#########
				//why isn't eof in a separate column ????
				return new Token(eof, line, col - 1);
				//#########

			case '!':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = neq;
				} else {
					t.kind = none;
					error(t, INVALID_CHAR, '!');
				}
				break;
			case '%':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = remas;
				} else t.kind = rem;
				break;
			case '=':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = eql;
				} else t.kind = assign;
				break;
			case '-':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = minusas;
				} else if (ch == '-') {
					nextCh();
					t.kind = mminus;
				} else t.kind = minus;
				break;
			case '+':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = plusas;
				} else if (ch == '+') {
					nextCh();
					t.kind = pplus;
				} else t.kind = plus;
				break;
			case '>':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = geq;
				} else t.kind = gtr;
				break;
			case '<':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = leq;
				} else t.kind = lss;
				break;
			case '&':
				nextCh();
				if (ch == '&') {
					nextCh();
					t.kind = and;
				} else {
					t.kind = none;
					error(t, INVALID_CHAR, '&');
				}
				break;
			case '|':
				nextCh();
				if (ch == '|') {
					nextCh();
					t.kind = or;
				} else {
					t.kind = none;
					error(t, INVALID_CHAR, "|");
				}
				break;
			case '*':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = timesas;
				} else t.kind = times;
				break;
			case '/':
				nextCh();
//				if (ch == '/') {
//					do
//						nextCh();
//					while (ch != '\n' && ch != eofCh);
//					t = next(); // call scanner recursively
//				} else
				if (ch == '*') {
					t = skipComment();
				} else if (ch == '=') {
					nextCh();
					t.kind = slashas;
				} else t.kind = slash;
				break;

			case '\0':
			default:
				t.kind = none;
				error(t, INVALID_CHAR, ch);
				nextCh();
				break;
		}
		return t;
	} // ch holds the next character that is still unprocessed

	private void readName(Token t) {
		t.kind = ident;

		StringBuilder sb = new StringBuilder();

		sb.append(ch);
		nextCh();
		while (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' || ch == '_' || ch == '-') {
			sb.append(ch);
			nextCh();
		}

		t.str = sb.toString();

		switch (t.str) {
			case "break":
				t.kind = break_;
				break;
			case "class":
				t.kind = class_;
				break;
			case "else":
				t.kind = else_;
				break;
			case "final":
				t.kind = final_;
				break;
			case "if":
				t.kind = if_;
				break;
			case "new":
				t.kind = new_;
				break;
			case "print":
				t.kind = print;
				break;
			case "program":
				t.kind = program;
				break;
			case "read":
				t.kind = read;
				break;
			case "return":
				t.kind = return_;
				break;
			case "void":
				t.kind = void_;
				break;
			case "while":
				t.kind = while_;
				break;
			case "compare":
				t.kind = compare_;
				break;
		}
	}

	private void readNumber(Token t) {
		t.kind = number;


		StringBuilder sb = new StringBuilder();
		sb.append(ch);

		nextCh();
		while (ch >= '0' && ch <= '9') {
			sb.append(ch);
			nextCh();
		}

		t.str = sb.toString();
		try {
			t.val = Integer.parseInt(t.str);
		} catch (NumberFormatException e) {
			error(t, BIG_NUM, t.str);
		}
	}

	private void readCharConst(Token t) {
		nextCh();
		t.kind = charConst;

		switch (ch) {
			case '\'':
				error(t, EMPTY_CHARCONST);
				nextCh();
				break;
			case '\\':
				nextCh();
				///////the best way to only convert '\r','\n','\'' and '\\'
				switch (ch) {
					case 'r':
						t.val = '\r';
						break;
					case 'n':
						t.val = '\n';
						break;
					case '\'':
						t.val = '\'';
						break;
					case '\\':
						t.val = '\\';
						break;
					default:
						error(t, UNDEFINED_ESCAPE, ch);
						break;
				}
				missingQuoteHelper(t);
				break;
			case LF:
			case '\r':
				error(t, ILLEGAL_LINE_END);
				break;
			case eofCh:
				error(t, MISSING_QUOTE);
				break;
			default:
				t.val = ch;
				missingQuoteHelper(t);
		}
	}
	private void missingQuoteHelper(Token t){
		nextCh();
		if (ch != '\'') {
			error(t, MISSING_QUOTE);
			return;
		}
		nextCh();
	}

	private Token skipComment() {
		int nestedCommentRememberCol = col;
		int nestedCommentLevel = 1;

		nextCh();

		for (; ; ) {
			switch (ch) {
				case '*':
					nextCh();
					if (ch == '/') {
						nextCh();
						nestedCommentLevel -= 1;
						if (nestedCommentLevel == 0) {
							return next();
						}
					}
					break;
				case '/':
					nextCh();
					if (ch == '*') {
						nextCh();
						nestedCommentLevel += 1;
					}
					break;
				case eofCh:
					Token t = new Token(eof, line, nestedCommentRememberCol - 1);
					error(t, EOF_IN_COMMENT);
					return new Token(eof, line, col - 1);
				default:
					nextCh();
			}
		}
	}
}
