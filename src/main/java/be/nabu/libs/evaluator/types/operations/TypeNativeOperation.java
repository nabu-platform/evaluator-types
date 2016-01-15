package be.nabu.libs.evaluator.types.operations;

import java.util.ArrayList;
import java.util.List;

import be.nabu.libs.evaluator.QueryPart;
import be.nabu.libs.evaluator.impl.NativeOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.SimpleTypeWrapper;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;


/**
 * Can return the type of the operation
 */
public class TypeNativeOperation extends NativeOperation<ComplexContent> implements TypeOperation {
	
	@Override
	public Type getReturnType(ComplexType context) {
		return getType(getParts().get(0).getType());
	}

	@Override
	public List<Validation<?>> validate(ComplexType context) {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		if (getParts().size() != 1 || !getParts().get(0).getType().isNative())
			messages.add(new ValidationMessage(Severity.ERROR, "The native operation must have exactly one native type"));
		return messages;
	}

	static Type getType(QueryPart.Type type) {
		SimpleTypeWrapper wrapper = SimpleTypeWrapperFactory.getInstance().getWrapper();
		switch(type) {
			case BOOLEAN_FALSE: return wrapper.wrap(Boolean.class);
			case BOOLEAN_TRUE: return wrapper.wrap(Boolean.class);
			case STRING: return wrapper.wrap(String.class);
			case NUMBER_DECIMAL: return wrapper.wrap(Double.class);
			case NUMBER_INTEGER: return wrapper.wrap(Long.class);
			default: throw new RuntimeException(type + " is not a recognized native type");
		}
	}
	
	/**
	 * Never a collection
	 */
	@Override
	public CollectionHandlerProvider<?, ?> getReturnCollectionHandler(ComplexType context) {
		return null;
	}
}
