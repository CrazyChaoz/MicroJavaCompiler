package ssw.mj.impl;

import ssw.mj.codegen.Code;
import ssw.mj.codegen.Label;

import java.util.ArrayList;

public final class LabelImpl extends Label {

	// TODO Exercise 6: Implementation of Labels for management of jump targets
	private int adr;                // adr >= 0: address already defined
	// adr < 0: label is undefined
	private ArrayList<Integer> fixupList;    // fixupaddresses

	public LabelImpl(Code code) {
		super(code);
		adr = -1;
		fixupList = new ArrayList();
	}

	public void putAdr() {
		fixupList.add(code.pc);
	}

	public void here() {
		if (adr >= 0) {
			System.err.println("label defined twice, this isn't cool, stop, seriously, stop, please");
		}
		for (Integer pos : fixupList) {
			code.put2(pos, code.pc - (pos - 1));
		}
		adr = code.pc;
	}
}
