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
package org.opengroup.osdu.storage.provider.azure.cache;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

// Backed by Redis so a tag removed on the pod that consumes the legal tag change event is removed for every replica.
@Component("LegalTagCache")
public class LegalTagCache implements ICache<String, String> {

    private final ICache<String, String> store;
    private final DpsHeaders dpsHeaders;

    public LegalTagCache(@Qualifier("LegalTagStore") ICache<String, String> store, DpsHeaders dpsHeaders) {
        this.store = store;
        this.dpsHeaders = dpsHeaders;
    }

    @Override
    public void put(String key, String val) {
        this.store.put(this.partitionKey(key), val);
    }

    @Override
    public String get(String key) {
        return this.store.get(this.partitionKey(key));
    }

    @Override
    public void delete(String key) {
        this.store.delete(this.partitionKey(key));
    }

    // The store is shared with other caches, so clearing it would drop their entries too.
    @Override
    public void clearAll() {
        throw new UnsupportedOperationException("LegalTagCache does not support clearAll");
    }

    private String partitionKey(String key) {
        return String.format("%s:legalTag:%s", this.dpsHeaders.getPartitionId(), key);
    }
}
