package be.nabu.libs.evaluator.types.operations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import be.nabu.libs.evaluator.impl.MethodOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.types.CollectionHandlerFactory;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class TypeMethodOperation extends MethodOperation<ComplexContent> implements TypeOperation {

	@Override
	public Type getReturnType(ComplexType context) {
		try {
			Method method = getMethod(getParts().size() - 1);
			Class<?> returnType = method.getReturnType();
			CollectionHandlerProvider<?, ?> handler = CollectionHandlerFactory.getInstance().getHandler().getHandler(returnType);
			if (handler != null) {
				returnType = handler.getComponentType(returnType);
			}
			DefinedSimpleType<?> wrap = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(returnType);
			return wrap != null ? wrap : BeanResolver.getInstance().resolve(returnType);
		}
		catch (Exception e) {
			throw new RuntimeException("Could not find method: " + getParts().get(0).getContent());
		}
	}

	@Override
	public List<Validation<?>> validate(ComplexType context) {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		try {
			Method method = getMethod(getParts().size() - 1);
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
		}
		catch (Exception e) {
			messages.add(new ValidationMessage(Severity.ERROR, "Method '" + getParts().get(0).getContent() + "' could not be resolved: " + e.getMessage()));
		}
		return messages;
	}
	
	@Override
	public CollectionHandlerProvider<?, ?> getReturnCollectionHandler(ComplexType context) {
		Method method = (Method) getParts().get(0).getContent();
		Class<?> returnType = method.getReturnType();
		CollectionHandlerProvider<?, ?> handler = CollectionHandlerFactory.getInstance().getHandler().getHandler(returnType);
		return handler;
	}
}
