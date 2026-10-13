// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.storage.provider.azure.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
/* This mixin is used to fix https://github.com/microsoft/spring-data-cosmosdb/issues/423
Upgrading cosmosdb spring library will also solve this. The fix is in a major release which needs lot of changes
This mixin is injected in QueryRepositoryImpl constructor. After upgrade these can be deleted.
*/
public interface QueryItemMixIn {
    @JsonIgnore
    abstract Logger getLogger();
}
