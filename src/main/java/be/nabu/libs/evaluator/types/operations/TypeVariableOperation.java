package be.nabu.libs.evaluator.types.operations;

import java.util.ArrayList;
import java.util.List;

import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.evaluator.impl.VariableOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.QueryPart;

/**
 * Note that because we don't support multi-type returns (like xpath pipe operator '|') the 
 * return result of a variable operation can always be deduced from the path plain and simple
 * Any intermediate indexed access is merely more specific access to a certain context but does not
 * change the resulting type
 * 
 * Note that resolving variable operations for the purposes of setting new values has (severe) limits on the querying possibilities!
 * Read the comments for more but in a nutshell:
 * - you can NOT use boolean queries because they have to be evaluated against a context that may not yet exist, so only numeric indexes are allowed
 * - you can only use "local" variables sparingly, the context is passed along on a best effort basis but if a non-existent path is found, the last known context is used for evaluation
 * 			> only use variables you KNOW exist or are on the root
 * 			> if this turns out to be too weird, we can just throw errors but then you can only define indexes with absolute paths
 * 
 * TODO: need to update the primitive index casting with the centralized casting system once in place
 * 
 * @author alex
 *
 */
public class TypeVariableOperation extends VariableOperation<ComplexContent> implements TypeOperation {

	/**
	 * This indicates whether or not the return type is a list
	 */
	private Boolean isList = null;
	
	/**
	 * This can be used to resolve a variable operation into an indexed path, e.g.
	 * my/path[1]/to[20]/something
	 * @param context
	 * @return
	 * @throws EvaluationException 
	 */
	public String resolve(ComplexContent context) throws EvaluationException {
		getContextStack().add(context);
		try {
			return resolve(context, 0, true);
		}
		finally {
			getContextStack().pop();
		}
	}
	
	@Override
	public boolean returnsList(ComplexType context) {
		if (isList == null)
			getReturnType(context);
		return isList;
	}

	/**
	 * 
	 * @param context
	 * @param offset
	 * @param exists This parameter indicates whether the previous resolved object existed, so basically it indicates up until where the context is updated to match the query
	 * @return
	 * @throws EvaluationException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String resolve(ComplexContent context, int offset, boolean exists) throws EvaluationException {
		String path = getParts().get(offset).getContent().toString();
		// remove any leading "/"
		if (path.startsWith("/"))
			path = path.substring(1);
		
		// try best effort to get the object, if it is null, we use the current context
		// this is because you can for example set "my/path[2]/variable[1]/value" while even the "path" variable does not exist
		// this does limit the use of local variables in your expressions though!
		// by ignoring "null" objects and passing the parent context we allow you to use it best effort instead of immediately throwing errors but it might exhibit odd behavior where e.g. "[myIndex]" may refer to another context than you think
		Object object = exists ? context.get(path) : null;
		
		// if it's the last part, just return as is
		if (offset == getParts().size() - 1)
			return path;
		// otherwise, we need to resolve the next element, it is either an operation (which MUST be numeric) or a variable
		else if (getParts().get(offset + 1).getType() == QueryPart.Type.OPERATION) {
			Object value = ((Operation) getParts().get(offset + 1).getContent()).evaluate(context);
			if (!(value instanceof Number))
				throw new EvaluationException("The part " + getParts().get(offset + 1) + " is not numeric, it resolves to: " + value);
			Number index = (Number) value;
			if (index.intValue() < 0)
				throw new EvaluationException("Can not have an index below 0");
			path += "[" + index.intValue() + "]";
			// if there is a part after the index, resolve that, it must be a variable!
			if (offset < getParts().size() - 2) {
				// the path you are referencing exists in the list, use it as a context
				if (object != null && ((List<?>) object).size() > index.intValue()) {
					object = ((List<?>) object).get(index.intValue());
					path += "/" + resolve(object == null ? context : (object instanceof ComplexContent ? (ComplexContent) object : new BeanInstance(object)), offset + 2, object != null);
				}
				// it does not exist, resolve with whatever context you have
				else
					path += "/" + resolve(context, offset + 2, false);
			}
		}
		// simply append it to the string
		else if (getParts().get(offset + 1).getType() == QueryPart.Type.VARIABLE)
			path += "/" + resolve(object == null ? context : (object instanceof ComplexContent ? (ComplexContent) object : new BeanInstance(object)), offset + 1, object != null);
		else
			throw new EvaluationException("Not expecting part " + getParts().get(offset + 1).getType() + " at this point");
		return path;
	}
	
	/**
	 * For calculating the return type we simply need to return the type of the last field in the variable
	 * If the last variable has an index and it is a numeric return type, we can leave the maxOccurs, otherwise we will have to make a list out of it
	 */
	@Override
	public Type getReturnType(ComplexType context) {
		// initialize
		if (isList == null)
			isList = false;
		return getReturnType(context, 0);
	}
	
	private Type getReturnType(ComplexType context, int offset) {
		Element<?> item = context.get(getParts().get(offset).getContent().toString());
		// if it's the last item in the list, return it
		if (offset == getParts().size() - 1)
			return item.getType();
		// if it's an operation we need to check which one: index leaves the list variable alone, boolean makes it a list
		if (getParts().get(offset + 1).getType() == QueryPart.Type.OPERATION) {
			if (((TypeOperation) getParts().get(offset + 1).getContent()).getReturnType(context) instanceof be.nabu.libs.types.simple.Boolean)
				isList = true;
			// jump past index
			offset++;
		}
		// if the index was the last one, return it
		if (offset == getParts().size() - 1)
			return item.getType();
		else
			return getReturnType((ComplexType) item.getType(), offset + 1);
	}

	@Override
	public List<Validation<?>> validate(ComplexType context) {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		for (int i = 0; i < getParts().size(); i++) {
			if (getParts().get(i).getType() != QueryPart.Type.VARIABLE) {
				messages.add(new ValidationMessage(Severity.ERROR, "The child " + getParts().get(i).getContent() + " can not be used in a path"));
				break;
			}
			String path = (String) getParts().get(i).getContent();
			// strip leading
			if (path.startsWith("/"))
				path = path.substring(1);
			Element<?> childContext = context.get(path);
			if (childContext == null) {
				messages.add(new ValidationMessage(Severity.ERROR, "The child " + path + " does not exist in the context"));
				break;
			}
			if (i < getParts().size() - 1) {
				if (childContext.getType() instanceof ComplexType) {
					context = (ComplexType) childContext.getType();
					// check if the next part is an operation
					// note that an operation can only be conducted on a list
					if (getParts().get(i + 1).getType() == QueryPart.Type.OPERATION) {
						if (!childContext.getType().isList(childContext.getProperties()))
							messages.add(new ValidationMessage(Severity.ERROR, "The element " + childContext.getName() + " is not a collection"));
						messages.addAll(((TypeOperation) getParts().get(++i).getContent()).validate(context));
					}
				}
				else {
					messages.add(new ValidationMessage(Severity.ERROR, "The child " + childContext.getName() + " is not complex, the subquery can not be run on it"));
					break;
				}
			}
		}
		return messages;
	}
	
	/**
	 * If you use a boolean result set in an index, you will always return a list
	 * Otherwise it depends on the state of the last variable (it might also be a list)
	 */
	@Override
	public boolean isList(ComplexType context) {
		for (int i = 0; i < getParts().size(); i++) {
			// get the return type of this variable
			Type type = context.get(getParts().get(i).getContent().toString()).getType();
			// if it's a list, check if the next is an operation (indexed access)
			if (type.isList()) {
				// if it's not indexed, you always get a list
				if (i == getParts().size() - 1 || getParts().get(i + 1).getType() != QueryPart.Type.OPERATION)
					return true;
				// if the return type of the operation is a boolean, you still get a list, it's a query, not an index
				else if (((TypeOperation) getParts().get(i + 1).getContent()).getReturnType(context) instanceof be.nabu.libs.types.simple.Boolean)
					return true;
				// otherwise you have selected a single element in the list, increase i to skip the index
				else
					i++;
			}
			// narrow the context for further loops
			if (type instanceof ComplexType)
				context = (ComplexType) type;
		}
		return false;
	}
}
