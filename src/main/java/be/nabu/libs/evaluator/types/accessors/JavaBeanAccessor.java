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
