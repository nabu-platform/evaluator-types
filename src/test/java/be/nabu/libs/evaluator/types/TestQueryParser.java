package be.nabu.libs.evaluator.types;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.PathAnalyzer;
import be.nabu.libs.evaluator.QueryParser;
import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.evaluator.types.operations.TypesOperationProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.java.BeanInstance;

public class TestQueryParser extends TestCase {
	
	public void testQuerier() throws ParseException, EvaluationException {
		PathAnalyzer<ComplexContent> pathAnalyzer = new PathAnalyzer<ComplexContent>(new TypesOperationProvider());
		Operation<ComplexContent> operation = pathAnalyzer.analyze(QueryParser.getInstance().parse("(myDouble == 5.5)"));
		
		Test2 test2 = new Test2(new Test(5), 5.5);
		
		test2.getList().add(new Test(2));
		test2.getList().add(new Test(3));
		test2.getList().add(new Test(4));
		test2.getList().add(new Test(5));
		
		ComplexContent complex = new BeanInstance<Test2>(test2);
		
		assertTrue((Boolean) operation.evaluate(complex));
		
		operation = pathAnalyzer.analyze(QueryParser.getInstance().parse("test/myInteger + 2 - myDouble"));
		// because the left operand is integer, the result will be treated as an integer!
		assertEquals(
			2,
			operation.evaluate(complex)
		);
		
		// left most operand is double
		operation = pathAnalyzer.analyze(QueryParser.getInstance().parse("myDouble - test/myInteger + 2"));
		assertEquals(
			2.5,
			operation.evaluate(complex)
		);
		
		// precedence
		operation = pathAnalyzer.analyze(QueryParser.getInstance().parse("myDouble - (test/myInteger + 2)"));
		assertEquals(
			-1.5,
			operation.evaluate(complex)
		);

		operation = pathAnalyzer.analyze(QueryParser.getInstance().parse("list[myInteger > 3]/myInteger"));
		assertEquals(Arrays.asList(new Integer[] { 4, 5 }), operation.evaluate(complex));
		
		operation = pathAnalyzer.analyze(QueryParser.getInstance().parse("list/myInteger"));
		assertEquals(Arrays.asList(new Integer[] { 2, 3, 4, 5 }), operation.evaluate(complex));
		
		operation = pathAnalyzer.analyze(QueryParser.getInstance().parse("test/myInteger + 2 = 7 && (myDouble + test/myInteger)==10.5"));
		assertTrue((Boolean) operation.evaluate(complex));
		
		// check if test is in the list
		operation = pathAnalyzer.analyze(QueryParser.getInstance().parse("test#list"));
		assertTrue((Boolean) operation.evaluate(complex));
	}
	
	public void testIncrement() throws EvaluationException, ParseException {
		PathAnalyzer<ComplexContent> pathAnalyzer = new PathAnalyzer<ComplexContent>(new TypesOperationProvider());
		assertEquals(2, pathAnalyzer.analyze(QueryParser.getInstance().parse("1++")).evaluate(null));
		// in programming the increment is delayed until after the entire operation
		// however the query evaluator does not update any state so it would be useless to delay until after evaluation
		// instead it is evaluated first
		assertEquals(4, pathAnalyzer.analyze(QueryParser.getInstance().parse("1++ + 2")).evaluate(null));
	}
	
	public void testNot() throws EvaluationException, ParseException {
		PathAnalyzer<ComplexContent> pathAnalyzer = new PathAnalyzer<ComplexContent>(new TypesOperationProvider());
		assertEquals(false, pathAnalyzer.analyze(QueryParser.getInstance().parse("!true")).evaluate(null));
		assertEquals(false, pathAnalyzer.analyze(QueryParser.getInstance().parse("false || !true")).evaluate(null));
		assertEquals(true, pathAnalyzer.analyze(QueryParser.getInstance().parse("!false & true && !false")).evaluate(null));
	}
	
	public void testMatches() throws EvaluationException, ParseException {
		PathAnalyzer<ComplexContent> pathAnalyzer = new PathAnalyzer<ComplexContent>(new TypesOperationProvider());
		assertEquals(true, pathAnalyzer.analyze(QueryParser.getInstance().parse("'dude' ~ '[a-z]+'")).evaluate(null));
		assertEquals(false, pathAnalyzer.analyze(QueryParser.getInstance().parse("'dude'~ '[^\\w]+'")).evaluate(null));
	}
	
	public void testIncorrect() throws ParseException {
		testParseException("a + b + ");
//		testParseException("a + + c");
		
		// this should not pass validation as test is not a list
//		Operation operation = PathAnalyzer.analyze(QueryParser.getInstance().parse("test[myInteger > 3]/myInteger"));
//		System.out.println(operation.validate(new BeanType(Test2.class)));
	}
	
	public void testParseException(String rule) {
		try {
			new PathAnalyzer<ComplexContent>(new TypesOperationProvider()).analyze(QueryParser.getInstance().parse(rule));
			fail("A parse exception should be thrown for structurally incorrect rules");
		}
		catch(ParseException e) {
			// ok
		}
	}
	
	public static class Test {
		private int myInteger;

		public Test(int myInteger) {
			this.myInteger = myInteger;
		}
		
		public int getMyInteger() {
			return myInteger;
		}

		public void setMyInteger(int myInteger) {
			this.myInteger = myInteger;
		}
		@Override
		public boolean equals(Object object) {
			return object instanceof Test && ((Test) object).myInteger == myInteger;
		}
	}
	
	public static class Test2 {
		private double myDouble;
		private Test test;
		private List<Test> list = new ArrayList<Test>();
		
		public Test2(Test test, double myDouble) {
			this.test = test;
			this.myDouble = myDouble;
		}

		public double getMyDouble() {
			return myDouble;
		}

		public void setMyDouble(double myDouble) {
			this.myDouble = myDouble;
		}

		public Test getTest() {
			return test;
		}

		public void setTest(Test test) {
			this.test = test;
		}

		public List<Test> getList() {
			return list;
		}
	}
}
