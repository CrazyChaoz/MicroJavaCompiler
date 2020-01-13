package ssw.mj.test;

import org.junit.Test;

public class KempingerTest extends CompilerTestCaseSupport {

	@Test
	public void kemp_bsp1() {
		init("program A\n" +
				"final int max = 12; \n" +
				"char c; int i;\n" +
				"class B { int x, y; } \n" +
				"{ void main () int[] iarr; B b; int n; {" +
				"n = 3;\n" +
				"}}");

		parseAndVerify();
	}


	@Test
	public void kemp_bsp2() {
		init("program A\n" +
				"final int max = 12; \n" +
				"char c; int i;\n" +
				"class B { int x, y; } \n" +
				"{ void main () int[] iarr; B b; int n; {" +
				"i = 10;\n" +
				"}}");

		parseAndVerify();
	}


	@Test
	public void kemp_bsp3() {
		init("program A\n" +
				"final int max = 12; \n" +
				"char c; int i;\n" +
				"class B { int x, y; } \n" +
				"{ void main () int[] iarr; B b; int n; {" +
				"n = 3 + i;\n" +
				"}}");

		parseAndVerify();
	}


	@Test
	public void kemp_bsp4() {
		init("program A\n" +
				"final int max = 12; \n" +
				"char c; int i;\n" +
				"class B { int x, y; } \n" +
				"{ void main () int[] iarr; B b; int n; {" +
				"n = 3 + i * max - n;\n" +
				"}}");

		parseAndVerify();
	}


	@Test
	public void kemp_bsp5() {
		init("program A\n" +
				"final int max = 12; \n" +
				"char c; int i;\n" +
				"class B { int x, y; } \n" +
				"{ void main () int[] iarr; B b; int n; {" +
				"iarr = new int[10];" +
				"iarr[5] = 10;" +
				"iarr[5]--;" +
				"print(iarr[5]);" +
				"}}");

		addExpectedRun("9");
		parseAndVerify();
	}


	@Test
	public void kemp_bsp6() {
		init("program A\n" +
				"final int max = 12; \n" +
				"char c; int i;\n" +
				"class B { int x, y; } \n" +
				"{ void main () int[] iarr; B b; int n; {" +
				"iarr = new int[10];" +
				"b = new B;" +
				"iarr[5] = 10;" +
				"b.y = iarr[5] * 3;\n" +
				"b.y--;\n" +
				"print(b.y);" +
				"}}");

		addExpectedRun("29");
		parseAndVerify();
	}

	@Test
	public void kemp_bsp7() {
		init("program A\n" +
				"final int max = 12; \n" +
				"char c; int i;\n" +
				"class B { int x, y; } \n" +
				"{ void main () int[] iarr; B b; int n; {" +
				"iarr = new int[10];" +
				"b = new B;" +
				"iarr[5] = 10;" +
				"b.y = iarr[5];\n" +
				"b.y += iarr[5];\n" +
				"print(b.y);" +
				"}}");

		addExpectedRun("20");
		parseAndVerify();
	}

	@Test
	public void compareConstants1() {
		init("program Test {" + LF + //
				"  void main() {  " + LF + //
				"    compare(1, 2) " + LF + //
				"      { print(-1); }" + LF + //
				"      { print(0); }" + LF + //
				"      { print(1); }" + LF + //
				"  }" + LF + //
				"}");

		addExpectedRun("-1");
		parseAndVerify();
	}

	@Test
	public void compareConstants2() {
		init("program Test {" + LF + //
				"  void main() {  " + LF + //
				"    compare(1, 1) " + LF + //
				"      { print(-1); }" + LF + //
				"      { print(0); }" + LF + //
				"      { print(1); }" + LF + //
				"  }" + LF + //
				"}");

		addExpectedRun("0");
		parseAndVerify();
	}

	@Test
	public void compareConstants3() {
		init("program Test {" + LF + //
				"  void main() {  " + LF + //
				"    compare(3, 2) " + LF + //
				"      { print(-1); }" + LF + //
				"      { print(0); }" + LF + //
				"      { print(1); }" + LF + //
				"  }" + LF + //
				"}");

		addExpectedRun("1");
		parseAndVerify();
	}
}