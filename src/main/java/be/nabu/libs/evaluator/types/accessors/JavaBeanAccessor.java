package be.nabu.libs.evaluator.types.accessors;

import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.api.ContextAccessor;
import be.nabu.libs.evaluator.api.WritableContextAccessor;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;

public class JavaBeanAccessor implements ContextAccessor<Object>, WritableContextAccessor<Object> {

	@Override
	public Class<Object> getContextType() {
		return Object.class;
	}

	@Override
	public boolean has(Object context, String name) throws EvaluationException {
		DefinedType type = BeanResolver.getInstance().resolve(context.getClass());
		return type instanceof ComplexType && ((ComplexType) type).get(name) != null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object get(Object context, String name) throws EvaluationException {
		BeanInstance beanInstance = new BeanInstance(context);
		// if it doesn't exist, don't try to resolve it, it will fail...
		if (beanInstance.getType().get(name) == null) {
			return null;
		}
		return beanInstance.get(name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void set(Object context, String name, Object value) throws EvaluationException {
		new BeanInstance(context).set(name, value);
	}

}
