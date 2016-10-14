package be.nabu.libs.evaluator.types.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.converter.api.Converter;
import be.nabu.libs.evaluator.QueryPart;
import be.nabu.libs.evaluator.impl.ClassicOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class TypeClassicOperation extends ClassicOperation<ComplexContent> implements TypeOperation {
	
	private Converter converter;
	
	private static List<QueryPart.Type> mathOperators = Arrays.asList(new QueryPart.Type[] {
		QueryPart.Type.ADD,
		QueryPart.Type.SUBSTRACT,
		QueryPart.Type.DIVIDE,
		QueryPart.Type.MULTIPLY,
		QueryPart.Type.MOD,
		QueryPart.Type.POWER
	});

	public Converter getConverter() {
		if (converter == null)
			converter = ConverterFactory.getInstance().getConverter();
		return converter;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}

	private Type getOperand(ComplexType context, int position, List<Validation<?>> messages) {
		QueryPart part = getParts().get(position);
		if (part.getType() == QueryPart.Type.NULL) {
			return null;
		}
		else if (part.getType().isNative())
			return TypeNativeOperation.getType(part.getType());
		else if (part.getType() == QueryPart.Type.OPERATION) {
			int size = messages.size();
			messages.addAll(((TypeOperation) part.getContent()).validate(context));
			// only resolve the operation if it is valid
			return size == messages.size() ? ((TypeOperation) part.getContent()).getReturnType(context) : null;
		}
		else
			return null;
	}

	@Override
	public Type getReturnType(ComplexType context) {
		// get the operator
		for (int i = 0; i < getParts().size(); i++) {
			// there's basically two kinds of operators: math & boolean
			// the result of a math operation depends entirely on the left operand
			if (getParts().get(i).getType().isOperator()) {
				if (mathOperators.contains(getParts().get(i).getType())) {
					QueryPart left = getParts().get(i - 1);
					// the left operand determines the result
					if (left.getType() == QueryPart.Type.OPERATION) {
						return ((TypeOperation) left.getContent()).getReturnType(context);
					}
					else if (left.getType().isNative()) {
						return TypeNativeOperation.getType(left.getType());
					}
					else {
						throw new RuntimeException("Unexpected left operand: " + left);
					}
				}
				else {
					return SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Boolean.class);
				}
			}
		}
		throw new RuntimeException("No operator found");
	}

	@Override
	public List<Validation<?>> validate(ComplexType context) {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		for (int i = 0; i < getParts().size(); i++) {
			QueryPart part = getParts().get(i);
			// only interested in operators
			if (part.getType().isOperator()) {
				// no left operand
				if (part.getType().hasLeftOperand() && i == 0)
					messages.add(new ValidationMessage(Severity.ERROR, "There is no left operand for the operator " + part));
				else if (part.getType().hasRightOperand() && i >= getParts().size() - 1)
					messages.add(new ValidationMessage(Severity.ERROR, "There is no right operand for the operator " + part));
				else {
					int size = messages.size();
					Type leftOperand = part.getType().hasLeftOperand() ? getOperand(context, i - 1, messages) : null;
					Type rightOperand = part.getType().hasRightOperand() ? getOperand(context, i + 1, messages) : null;
					
					// break out of the validation if validation messages were added by a child operation
					if (size != messages.size())
						break;
					
					// in the few cases you are doing null checks, it is not required to work with simple types, so check if either operand is null
					// the only comparisons possible with null are equals or not equals
					if ((leftOperand == null || rightOperand == null) && (part.getType() == QueryPart.Type.EQUALS || part.getType() == QueryPart.Type.NOT_EQUALS)) {
						break;
					}
					
					// otherwise you are doing more complex operations which require simple types
					if (leftOperand != null && !(leftOperand instanceof SimpleType))
						messages.add(new ValidationMessage(Severity.ERROR, "The left operand " + leftOperand + " is not compatible with the operator " + part));
					else if (rightOperand != null && !(rightOperand instanceof SimpleType))
						messages.add(new ValidationMessage(Severity.ERROR, "The right operand " + rightOperand + " is not compatible with the operator " + part));
					
					Class<?> leftClass = leftOperand != null ? ((SimpleType<?>) leftOperand).getInstanceClass() : null;
					Class<?> rightClass = rightOperand != null ? ((SimpleType<?>) rightOperand).getInstanceClass() : null;
					
					switch(part.getType()) {
						case NOT:
							if (!Boolean.class.isAssignableFrom(rightClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " expects a boolean right operand, not " + rightClass));
							if (leftOperand != null)
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " does not support a left operand"));
						break;
						case INCREASE:
						case DECREASE:
							if (!Number.class.isAssignableFrom(leftClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " expects a number left operand, not " + leftClass));
							if (rightOperand != null)
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " does not support a right operand"));
						break;
						case ADD:
							if (!Number.class.isAssignableFrom(leftClass) && !String.class.isAssignableFrom(leftClass) && !getConverter().canConvert(leftClass, String.class) && !getConverter().canConvert(leftClass, Double.class))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports numbers & strings, the left operand is however of type " + leftClass));
							if (!Number.class.isAssignableFrom(rightClass) && !String.class.isAssignableFrom(rightClass) && !getConverter().canConvert(rightClass, String.class) && !getConverter().canConvert(rightClass, Double.class))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports numbers & strings, the left operand is however of type " + rightClass));
							if (!getConverter().canConvert(rightClass, leftClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The right operand of type " + rightClass + " can not be cast to the type of the left operand " + leftClass));
						break;
						case POWER:
						case MULTIPLY:
						case DIVIDE:
						case MOD:
						case SUBSTRACT:
							if (!Number.class.isAssignableFrom(leftClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports numbers, the left operand is however of type " + leftClass));
							if (!Number.class.isAssignableFrom(rightClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports numbers, the left operand is however of type " + rightClass));
						case EQUALS:
						case NOT_EQUALS:
							if (!getConverter().canConvert(rightClass, leftClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The right operand of type " + rightClass + " can not be cast to the type of the left operand " + leftClass));
						break;
						case LESSER:
						case LESSER_OR_EQUALS:
						case GREATER:
						case GREATER_OR_EQUALS:
							if (!Comparable.class.isAssignableFrom(leftClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports comparable types, the left operand is however of type " + leftClass));
							if (!Comparable.class.isAssignableFrom(rightClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports comparable types, the right operand is however of type " + rightClass));
							if (!getConverter().canConvert(rightClass, leftClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The right operand of type " + rightClass + " can not be cast to the type of the left operand " + leftClass));
						break;
						case BITWISE_AND:
						case BITWISE_OR:
						case LOGICAL_AND:
						case LOGICAL_OR:
						case XOR:
						case NOT_XOR:
							if (!Boolean.class.isAssignableFrom(leftClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports boolean types, the left operand is however of type " + leftClass));
							if (!Boolean.class.isAssignableFrom(rightClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports boolean types, the right operand is however of type " + rightClass));
						break;
						case IN:
						case NOT_IN:
							if (!Collection.class.isAssignableFrom(rightClass) && !Object[].class.isAssignableFrom(rightClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports a collection as the right operand, it is however of type " + rightClass));
						break;
						case MATCHES:
						case NOT_MATCHES:
							if (!String.class.isAssignableFrom(leftClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports string types, the left operand is however of type " + leftClass));
							if (!String.class.isAssignableFrom(rightClass))
								messages.add(new ValidationMessage(Severity.ERROR, "The operator " + part + " only supports string types, the right operand is however of type " + rightClass));
					}
				}
			}
		}
		return messages;
	}

	/**
	 * Normally the operation does not return lists
	 */
	public boolean isList(ComplexType context) {
		return false;
	}
	
	/**
	 * Never a collection
	 */
	public CollectionHandlerProvider<?, ?> getReturnCollectionHandler(ComplexType context) {
		return null;
	}
}