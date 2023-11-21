package be.nabu.libs.evaluator.types.accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.api.ListableContextAccessor;
import be.nabu.libs.evaluator.api.WritableContextAccessor;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.Element;

public class ComplexContentAccessor implements ListableContextAccessor<ComplexContent>, WritableContextAccessor<ComplexContent> {

	@Override
	public Class<ComplexContent> getContextType() {
		return ComplexContent.class;
	}

	@Override
	public boolean has(ComplexContent context, String name) throws EvaluationException {
		return context.getType().get(name) != null;
	}
	

	@Override
	public boolean hasValue(ComplexContent context, String name) throws EvaluationException {
		return context.has(name);
	}

	@Override
	public Object get(ComplexContent context, String name) throws EvaluationException {
		return context.get(name);
	}

	@Override
	public Collection<String> list(ComplexContent object) {
		List<String> names = new ArrayList<String>();
		if (object.getType() != null) {
			for (Element<?> element : TypeUtils.getAllChildren(object.getType())) {
				names.add(element.getName());
			}
		}
		return names;
	}

	@Override
	public void set(ComplexContent context, String name, Object value) throws EvaluationException {
		context.set(name, value);
	}

}
