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

package be.nabu.libs.evaluator.types.operations;

import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.evaluator.api.OperationProvider;
import be.nabu.libs.types.api.ComplexContent;

public class TypesOperationProvider implements OperationProvider<ComplexContent> {

	private boolean allowOperatorOverloading;
	
	public TypesOperationProvider() {
		this(true);
	}
	public TypesOperationProvider(boolean allowOperatorOverloading) {
		this.allowOperatorOverloading = allowOperatorOverloading;
	}
	
	@Override
	public Operation<ComplexContent> newOperation(OperationType type) {
		switch(type) {
			case CLASSIC: return new TypeClassicOperation(allowOperatorOverloading);
			case METHOD: return new TypeMethodOperation();
			case VARIABLE: return new TypeVariableOperation();
			case NATIVE: return new TypeNativeOperation();
		}
		throw new RuntimeException("Unknown operation type: " + type);
	}

}
