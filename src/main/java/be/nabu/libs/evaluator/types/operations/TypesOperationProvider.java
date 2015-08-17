package be.nabu.libs.evaluator.types.operations;

import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.evaluator.api.OperationProvider;
import be.nabu.libs.types.api.ComplexContent;

public class TypesOperationProvider implements OperationProvider<ComplexContent> {

	@Override
	public Operation<ComplexContent> newOperation(OperationType type) {
		switch(type) {
			case CLASSIC: return new TypeClassicOperation();
			case METHOD: return new TypeMethodOperation();
			case VARIABLE: return new TypeVariableOperation();
			case NATIVE: return new TypeNativeOperation();
		}
		throw new RuntimeException("Unknown operation type: " + type);
	}

}
