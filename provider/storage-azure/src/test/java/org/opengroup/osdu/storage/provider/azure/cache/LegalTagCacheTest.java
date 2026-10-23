// Copyright © Schlumberger
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import io.lettuce.core.RedisConnectionException;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

class LegalTagCacheTest {

    private static final String TAG = "opendes-storage-tag";
    private static final String VALID = "Valid LegalTag";

    private LegalTagVmCache sharedStore;
    private DpsHeaders headers;

    @BeforeEach
    void setUp() {
        sharedStore = new LegalTagVmCache();
        headers = mock(DpsHeaders.class);
        when(headers.getPartitionId()).thenReturn("opendes");
    }

    @Test
    void deleteOnOneReplica_isSeenByAnotherReplica() {
        LegalTagCache replicaA = new LegalTagCache(sharedStore, headers);
        LegalTagCache replicaB = new LegalTagCache(sharedStore, headers);

        replicaA.put(TAG, VALID);
        replicaB.delete(TAG);

        assertNull(replicaA.get(TAG));
    }

    @Test
    void entriesAreScopedToTheRequestPartition() {
        DpsHeaders otherHeaders = mock(DpsHeaders.class);
        when(otherHeaders.getPartitionId()).thenReturn("other");
        LegalTagCache opendes = new LegalTagCache(sharedStore, headers);
        LegalTagCache other = new LegalTagCache(sharedStore, otherHeaders);

        opendes.put(TAG, VALID);

        assertEquals(VALID, opendes.get(TAG));
        assertNull(other.get(TAG));
        assertEquals(VALID, sharedStore.get("opendes:legalTag:" + TAG));
    }

    @Test
    @SuppressWarnings("unchecked")
    void storeFailure_readsAsMissAndSkipsWrite() {
        ICache<String, String> failingStore = mock(ICache.class);
        when(failingStore.get(anyString())).thenThrow(new RedisConnectionException("unreachable"));
        doThrow(new RedisConnectionException("unreachable")).when(failingStore).put(anyString(), anyString());
        LegalTagCache cache = new LegalTagCache(failingStore, headers);

        assertNull(cache.get(TAG));
        cache.put(TAG, VALID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void storeFailure_onDeleteIsRaisedSoTheMessageIsRetried() {
        ICache<String, String> failingStore = mock(ICache.class);
        doThrow(new RedisConnectionException("unreachable")).when(failingStore).delete(anyString());
        LegalTagCache cache = new LegalTagCache(failingStore, headers);

        assertThrows(RedisConnectionException.class, () -> cache.delete(TAG));
    }

    @Test
    void clearAll_isRejectedSoTheSharedStoreIsNotFlushed() {
        LegalTagCache cache = new LegalTagCache(sharedStore, headers);
        cache.put(TAG, VALID);

        assertThrows(UnsupportedOperationException.class, cache::clearAll);
        assertEquals(VALID, cache.get(TAG));
    }
}
