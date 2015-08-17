package be.nabu.libs.evaluator.types.operations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import be.nabu.libs.evaluator.impl.MethodOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

/**
 * All the methods that can be used must be static
 * Due to type erasure, you can't use STL but must resort to arrays to return multiple values
 * Also note you can't address methods in the "default" package
 * Instead all methods with no package declaration are referred to be.nabu.modules.structure.query.Methods
 * 
 * There are a few ways to determine which method you want:
 * 1) find the method that matches the return types of the target operations. This is by far the best and most specific but has a huge downside: you need to know the return types
 * when you call the resolve(). The evaluation path analyzer does not have enough information and giving it that information might limit the usability of the analyzer
 * You could resolve at first call and use whatever context was available then but this might make it odd if you use the same operation in multiple contexts with different parameters. You would get unexpected exceptions
 * 
 * 2) method that matches the number of return types, so you can still overload methods with a different number of arguments
 * The downside here is varargs, i'm not yet entirely sure how it will be used
 * 
 * 3) (as is) the most restrictive way is to simply check by name, that means each method must have a unique name within its class
 * The upside of this method is that i can expand to either of the above if deemed necessary.
 * 
 * @author alex
 *
 */
public class TypeMethodOperation extends MethodOperation<ComplexContent> implements TypeOperation {

	@Override
	public Type getReturnType(ComplexType context) {
		Method method = (Method) getParts().get(0).getContent();
		Class<?> returnType = method.getReturnType();
		return SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(returnType);
	}

	@Override
	public List<Validation<?>> validate(ComplexType context) {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		Method method = (Method) getParts().get(0).getContent();
		Class<?> [] parameterTypes = method.getParameterTypes();
		for (int i = 1; i < getParts().size(); i++) {
			TypeOperation argumentOperation = (TypeOperation) getParts().get(i).getContent();
			Type returnType = argumentOperation.getReturnType(context);
			if (returnType instanceof ComplexType) {
				if (!parameterTypes[i - 1].isAssignableFrom(ComplexContent.class))
					messages.add(new ValidationMessage(Severity.ERROR, "Argument " + i + " expects a " + parameterTypes[i - 1] + " but will instead receive a ComplexContent instance"));
			}
			else {
				Class<?> returnClass = ((SimpleType<?>) returnType).getInstanceClass();
				if (!parameterTypes[i - 1].isAssignableFrom(returnClass))
					messages.add(new ValidationMessage(Severity.ERROR, "Argument " + i + " expects a " + parameterTypes[i - 1] + " but will instead receive a " + returnClass + " instance"));
			}
		}
		return messages;
	}

	@Override
	public boolean isList(ComplexType context) {
		return false;
	}

	public boolean returnsList(ComplexType context) {
		return getReturnType(context).isList();
	}
}
