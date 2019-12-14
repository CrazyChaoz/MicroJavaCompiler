package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Label;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Tab;

import static ssw.mj.Errors.Message.NO_VAL;
import static ssw.mj.Errors.Message.NO_VAR;

public final class CodeImpl extends Code {



	public CodeImpl(Parser p) {
		super(p);
	}

	// TODO Exercise 5 - 6: implementation of code generation
	public void load(Operand x) {
		switch (x.kind) {
			case Con:
				switch (x.val) {
					case -1:
						put(OpCode.const_m1);
						break;
					case 0:
						put(OpCode.const_0);
						break;
					case 1:
						put(OpCode.const_1);
						break;
					case 2:
						put(OpCode.const_2);
						break;
					case 3:
						put(OpCode.const_3);
						break;
					case 4:
						put(OpCode.const_4);
						break;
					case 5:
						put(OpCode.const_5);
						break;
					default:
						put(OpCode.const_);
						put4(x.val);
						break;
				}
				break;
			case Local:
				switch (x.adr) {
					case 0:
						put(OpCode.load_0);
						break;
					case 1:
						put(OpCode.load_1);
						break;
					case 2:
						put(OpCode.load_2);
						break;
					case 3:
						put(OpCode.load_3);
						break;
					default:
						put(OpCode.load);
						put(x.adr);
						break;
				}
				break;
			case Static:
				put(OpCode.getstatic);
				put2(x.adr);
				break;
			case Stack:
				break; // nothing to do (already loaded)
			case Fld:
				put(OpCode.getfield);
				put2(x.adr);
				break;
			case Elem:
				if (x.type == Tab.charType) {
					put(OpCode.baload);
				} else {
					put(OpCode.aload);
				}
				break;
			default:
				parser.error(NO_VAL);
		}
		x.kind = Operand.Kind.Stack;
	}

	public void generalStoreOperations(Operand toStore) {
		switch (toStore.kind) {
			case Local:
				switch (toStore.adr) {
					case 0:
						put(OpCode.store_0);
						break;
					case 1:
						put(OpCode.store_1);
						break;
					case 2:
						put(OpCode.store_2);
						break;
					case 3:
						put(OpCode.store_3);
						break;
					default:
						put(OpCode.store);
						put(toStore.adr);
						break;
				}
				break;
			case Static:
				put(OpCode.putstatic);
				put2(toStore.adr);
				break;
			case Fld:
				put(OpCode.putfield);
				put2(toStore.adr);
				break;
			case Elem:
				if (toStore.type == Tab.charType) {
					put(OpCode.bastore);
				} else {
					put(OpCode.astore);
				}
				break;
			default:
				parser.error(NO_VAR);
		}
	}

	public void assign(Operand x, Operand y) {
		load(y);
		generalStoreOperations(x);
	}



	public void tJump (Operand x) {
		put(OpCode.get(OpCode.jmp.ordinal()+x.op.ordinal()));
		x.tLabel.putAdr();
	}


	public void fJump (Operand x) {
		put(OpCode.get(OpCode.jmp.ordinal()+CompOp.invert(x.op).ordinal())); // jne, jeq, jge, jgt, ...
		x.fLabel.putAdr();
	}
}
