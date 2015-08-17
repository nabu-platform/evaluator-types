package be.nabu.libs.evaluator.types.accessors;

import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.api.ContextAccessor;
import be.nabu.libs.types.api.ComplexContent;

public class ComplexContentAccessor implements ContextAccessor<ComplexContent> {

	@Override
	public Class<ComplexContent> getContextType() {
		return ComplexContent.class;
	}

	@Override
	public boolean has(ComplexContent context, String name) throws EvaluationException {
		return context.getType().get(name) != null;
	}

	@Override
	public Object get(ComplexContent context, String name) throws EvaluationException {
		return context.get(name);
	}

}
