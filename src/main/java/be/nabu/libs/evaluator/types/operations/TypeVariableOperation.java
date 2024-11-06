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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.evaluator.impl.VariableOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ListCollectionHandlerProvider;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanType;
import be.nabu.libs.types.properties.CollectionHandlerProviderProperty;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.QueryPart;

/**
 * Note that because we don't support multi-type returns (like xpath pipe operator '|') the return result of a variable operation can always be deduced from the path
 * Any intermediate indexed access is merely more specific access to a certain context but does not change the resulting type
 * 
 * Note that resolving variable operations for the purposes of setting new values has (severe) limits on the querying possibilities!
 * Read the comments for more but in a nutshell:
 * - you can NOT use boolean queries because they have to be evaluated against a context that may not yet exist, so only numeric indexes are allowed
 * - you can only use "local" variables sparingly, the context is passed along on a best effort basis but if a non-existent path is found, the last known context is used for evaluation
 * 			- only use variables you KNOW exist or are on the root
 * 			- if this turns out to be too weird, we can just throw errors but then you can only define indexes with absolute paths
 */
public class TypeVariableOperation extends VariableOperation<ComplexContent> implements TypeOperation {

	/**
	 * The collection handler for the return parameter
	 */
	private CollectionHandlerProvider<?, ?> collectionHandler;
	
	/**
	 * This can be used to resolve a variable operation into an indexed path, e.g.
	 * my/path[1]/to[20]/something
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

	/**
	 * @param exists This parameter indicates whether the previous resolved object existed, so basically it indicates up until where the context is updated to match the query
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
		if (contextStack.get() == null) {
			contextStack.set(new Stack<ComplexType>());
		}
		boolean pushed = false;
		if (contextStack.get().isEmpty()) {
			pushed = true;
			contextStack.get().push(context);
		}
		try {
			Type returnType = getReturnType(context, 0);
			return returnType;
		}
		finally {
			if (pushed) {
				contextStack.get().pop();
			}
		}
	}
	
	private Type getReturnType(ComplexType context, int offset) {
		String path = getParts().get(offset).getContent().toString();
		Element<?> item;
		// if it starts with a "/", we could be looking at an absolute root access (in a subquery) or simply the leading "/" for a path at the root
		if (path.startsWith("/")) {
			item = offset == 0 ? contextStack.get().get(0).get(path.substring(1)) : context.get(path.substring(1));
		}
		else {
			item = context.get(path);
			if (item == null && path.startsWith("@")) {
				item = context.get(path.substring(1));
			}
		}
		if (item == null) {
			throw new IllegalArgumentException("Can not find '" + path + "' in: " + (offset == 0 && path.startsWith("/") ? contextStack.get().get(0) : context) + " (offset=" + offset + ")");
		}
		// if it's the last item in the list, return it
		if (offset == getParts().size() - 1) {
			CollectionHandlerProvider<?, ?> collectionHandler = ValueUtils.getValue(CollectionHandlerProviderProperty.getInstance(), item.getProperties());
			if (collectionHandler != null) {
				this.collectionHandler = collectionHandler;
			}
			else if (item.getType().isList(item.getProperties()) && this.collectionHandler == null) {
				this.collectionHandler = new ListCollectionHandlerProvider();
			}
			return item.getType();
		}
		// if it's an operation we need to check which one: index leaves the list variable alone, boolean makes it a list
		if (getParts().get(offset + 1).getType() == QueryPart.Type.OPERATION) {
			Type returnType = ((TypeOperation) getParts().get(offset + 1).getContent()).getReturnType(context);
			if (returnType instanceof SimpleType && Boolean.class.isAssignableFrom(((SimpleType<?>) returnType).getInstanceClass())) {
				this.collectionHandler = new ListCollectionHandlerProvider();
			}
			// jump past index
			offset++;
		}
		// if the item is a list and it is not followed by an operation, the result will automatically become a list as well
		else if (item.getType().isList(item.getProperties())) {
			CollectionHandlerProvider<?, ?> collectionHandler = ValueUtils.getValue(CollectionHandlerProviderProperty.getInstance(), item.getProperties());
			if (collectionHandler != null) {
				this.collectionHandler = collectionHandler;
			}
			else if (this.collectionHandler == null) {
				this.collectionHandler = new ListCollectionHandlerProvider();
			}
		}
		// if the index was the last one, return it
		if (offset == getParts().size() - 1) {
			// @2021-10-29: this condition is the exact same as line 150, we should've already jumped out there _unless_ there is an index
			// if there is an index however, whether or not the result was a collection, is already determined
			// by running the following code, we _always_ have a collection handler if we have an index (being that an index can only be on a collection)
//			CollectionHandlerProvider<?, ?> collectionHandler = ValueUtils.getValue(CollectionHandlerProviderProperty.getInstance(), item.getProperties());
//			if (collectionHandler != null) {
//				this.collectionHandler = collectionHandler;
//			}
//			else if (item.getType().isList(item.getProperties()) && this.collectionHandler == null) {
//				this.collectionHandler = new ListCollectionHandlerProvider();
//			}
			return item.getType();
		}
		else {
			return getReturnType((ComplexType) item.getType(), offset + 1);
		}
	}
	
	@Override
	public CollectionHandlerProvider<?, ?> getReturnCollectionHandler(ComplexType context) {
		getReturnType(context);
		return collectionHandler;
	}

	private static ThreadLocal<Stack<ComplexType>> contextStack = new ThreadLocal<Stack<ComplexType>>();
	
	@Override
	public List<Validation<?>> validate(ComplexType context) {
		if (contextStack.get() == null) {
			contextStack.set(new Stack<ComplexType>());
		}
		int pushes = 0;
		try {
			// whether or not we are evaluating against the root of this operation
			// if at the root, the usage of "/" is taken literally (e.g. for query operations that refer to the general root)
			// if not at the root, the usage of "/" is taken as a variable separator
			boolean isRootAccess = true;
			if (contextStack.get().isEmpty()) {
				contextStack.get().push(context);
				pushes++;
			}
			List<Validation<?>> messages = new ArrayList<Validation<?>>();
			for (int i = 0; i < getParts().size(); i++) {
				if (getParts().get(i).getType() != QueryPart.Type.VARIABLE) {
					messages.add(new ValidationMessage(Severity.ERROR, "The child " + getParts().get(i).getContent() + " can not be used in a path"));
					break;
				}
				String path = (String) getParts().get(i).getContent();
				// strip leading
				Element<?> childContext;
				if (path.startsWith("/")) {
					if (isRootAccess) {
						childContext = contextStack.get().get(0).get(path.substring(1));
					}
					else {
						childContext = context.get(path.substring(1));	
					}
				}
				else if ("$this".equals(path)) {
					if (contextStack.get().isEmpty()) {
						messages.add(new ValidationMessage(Severity.ERROR, "Can not use the $this reference without a valid context"));
						break;	
					}
					// a single instance of the current one
					childContext = new ComplexElementImpl("$this", contextStack.get().peek(), null);
				}
				else {
					// stop before because the next step will throw an exception if we try it on a beantype
					if (context instanceof BeanType && ((BeanType<?>) context).getBeanClass().equals(Object.class)) {
						break;
					}
					childContext = context.get(path);
					// if we can't find the context and its an attribute, it might be modelled as just an element
					if (childContext == null && path.startsWith("@")) {
						childContext = context.get(path.substring(1));
					}
					// we can not validate further if we have encountered an object
//					if (childContext == null && context instanceof BeanType && ((BeanType<?>) context).getBeanClass().equals(Object.class)) {
//						break;
//					}
				}
				if (childContext == null) {
					messages.add(new ValidationMessage(Severity.ERROR, "The child " + path + " does not exist in the context"));
					break;
				}
				if (i < getParts().size() - 1) {
					if (childContext.getType() instanceof ComplexType) {
						context = (ComplexType) childContext.getType();
						contextStack.get().push(context);
						pushes++;
						isRootAccess = false;
					}
					// check if the next part is an operation
					// note that an operation can only be conducted on a list
					if (getParts().get(i + 1).getType() == QueryPart.Type.OPERATION) {
						if (!childContext.getType().isList(childContext.getProperties())) {
							messages.add(new ValidationMessage(Severity.ERROR, "The element " + childContext.getName() + " is not a collection, the subquery can not be run on it"));
						}
						messages.addAll(((TypeOperation) getParts().get(++i).getContent()).validate(context));
					}
					// even after any possible list validation, there is still part of the query left
					if (!(childContext.getType() instanceof ComplexType) && i < getParts().size() - 1) {
						messages.add(new ValidationMessage(Severity.ERROR, "The child " + childContext.getName() + " is not complex, further variable access is impossible"));
						break;
					}
				}
			}
			return messages;
		}
		finally {
			for (int i = 0; i < pushes; i++) {
				contextStack.get().pop();
			}
		}
	}
	
}
