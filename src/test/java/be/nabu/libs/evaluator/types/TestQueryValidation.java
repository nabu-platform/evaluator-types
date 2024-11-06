/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.libs.evaluator.types;

import java.text.ParseException;

import be.nabu.libs.evaluator.PathAnalyzer;
import be.nabu.libs.evaluator.QueryParser;
import be.nabu.libs.evaluator.types.TestQueryParser.Test2;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.evaluator.types.operations.TypesOperationProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.java.BeanType;
import junit.framework.TestCase;

public class TestQueryValidation extends TestCase {
	public void testSimple() throws ParseException {
		TypeOperation operation = (TypeOperation) new PathAnalyzer<ComplexContent>(new TypesOperationProvider()).analyze(QueryParser.getInstance().parse("1 == true"));
		// expecting one validation error: the boolean on the right side can not be cast to the number on the left
		assertEquals(0, operation.validate(null).size());
	}
	
	public void testVariable() throws ParseException {
		PathAnalyzer<ComplexContent> pathAnalyzer = new PathAnalyzer<ComplexContent>(new TypesOperationProvider());
		TypeOperation operation = (TypeOperation) pathAnalyzer.analyze(QueryParser.getInstance().parse("list[myInteger > 3]/myInteger"));
		assertEquals(0, operation.validate(new BeanType<Test2>(Test2.class)).size());
		
		// the path myInteger2 does not exist & test is not a list
		operation = (TypeOperation) pathAnalyzer.analyze(QueryParser.getInstance().parse("test[myInteger > 3]/myInteger2"));
		assertEquals(2, operation.validate(new BeanType<Test2>(Test2.class)).size());
		
		operation = (TypeOperation) pathAnalyzer.analyze(QueryParser.getInstance().parse("test[myInteger3 > 3]/myInteger2"));
		assertEquals(3, operation.validate(new BeanType<Test2>(Test2.class)).size());
		
		// boolean casts are now possible for integers...
		operation = (TypeOperation) pathAnalyzer.analyze(QueryParser.getInstance().parse("list[myInteger > true]/myInteger"));
		assertEquals(0, operation.validate(new BeanType<Test2>(Test2.class)).size());
		
		// the myInteger > true will be an error because integer is not comparable with boolean
		// additionally myNonExistentRecord will obviously not exist
		// however the validator stops there, it will not add a validation error for "something"
		operation = (TypeOperation) pathAnalyzer.analyze(QueryParser.getInstance().parse("list[myInteger > true]/myNonExistentRecord/something"));
		assertEquals(1, operation.validate(new BeanType<Test2>(Test2.class)).size());
	}
}
