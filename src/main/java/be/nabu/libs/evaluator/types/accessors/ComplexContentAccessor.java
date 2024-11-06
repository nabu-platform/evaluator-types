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
