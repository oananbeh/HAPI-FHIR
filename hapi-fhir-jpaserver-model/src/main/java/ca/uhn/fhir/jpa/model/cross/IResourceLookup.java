/*-
 * #%L
 * HAPI FHIR JPA Model
 * %%
 * Copyright (C) 2014 - 2024 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.model.cross;

import ca.uhn.fhir.rest.api.server.storage.IResourcePersistentId;

import java.util.Date;

public interface IResourceLookup<T extends IResourcePersistentId<?>> {
	String getResourceType();

	/**
	 * If the resource is deleted, returns the date/time that the resource was deleted at. Otherwise, returns <code>null</code>
	 */
	Date getDeleted();

	T getPersistentId();
}
