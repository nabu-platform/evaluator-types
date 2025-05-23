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

package be.nabu.libs.evaluator.types.api;

import java.util.List;

import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validation;

public interface TypeOperation extends Operation<ComplexContent> {
	public List<Validation<?>> validate(ComplexType context);
	public Type getReturnType(ComplexType context);
	public CollectionHandlerProvider<?, ?> getReturnCollectionHandler(ComplexType context);
	public default Value<?>[] getReturnProperties(ComplexType context) { 
		return null;
	};
}
