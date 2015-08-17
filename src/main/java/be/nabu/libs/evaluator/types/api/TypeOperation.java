package be.nabu.libs.evaluator.types.api;

import java.util.List;

import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validation;

public interface TypeOperation extends Operation<ComplexContent> {
	public List<Validation<?>> validate(ComplexType context);
	public Type getReturnType(ComplexType context);
	public boolean isList(ComplexType context);
	public boolean returnsList(ComplexType context);
}
