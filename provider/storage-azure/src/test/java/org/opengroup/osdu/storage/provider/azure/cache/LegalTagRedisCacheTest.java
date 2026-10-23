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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.lettuce.core.codec.RedisCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opengroup.osdu.azure.cache.IRedisClientFactory;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.storage.provider.azure.di.RedisConfig;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("LegalTagRedisCache Tests")
class LegalTagRedisCacheTest extends AbstractRedisCacheTest<LegalTagRedisCache> {

    @Override
    protected LegalTagRedisCache createCache(RedisConfig config) {
        return new LegalTagRedisCache(config);
    }

    @Test
    void usesItsOwnFactoryClientSoCursorTtlDoesNotApply() {
        RedisConfig config = new RedisConfig(0, 5, 6380, 5, "host-key", "password-key", null, "hostname", 3600, 30, 390, 3600);

        Class<?> legalTagCodec = codecRequestedFromFactory(new LegalTagRedisCache(config));
        Class<?> cursorCodec = codecRequestedFromFactory(new CursorRedisCache(config));

        assertNotEquals(cursorCodec, legalTagCodec);
    }

    @Test
    void encodesValuesTheSameWayAsTheDefaultCodec() {
        RedisConfig config = new RedisConfig(0, 5, 6380, 5, "host-key", "password-key", null, "hostname", 3600, 30, 390, 3600);
        RedisCodec<String, String> legalTagCodec = new LegalTagRedisCache(config).getCodec(String.class, String.class);
        RedisCodec<String, String> cursorCodec = new CursorRedisCache(config).getCodec(String.class, String.class);

        assertEquals("Valid LegalTag", cursorCodec.decodeValue(legalTagCodec.encodeValue("Valid LegalTag")));
        assertEquals("opendes:legalTag:tag", cursorCodec.decodeKey(legalTagCodec.encodeKey("opendes:legalTag:tag")));
    }

    @SuppressWarnings("unchecked")
    private static Class<?> codecRequestedFromFactory(RedisAzureCache<String> cache) {
        IRedisClientFactory<String> factory = mock(IRedisClientFactory.class);
        when(factory.getClient(any(), any(), any())).thenReturn(mock(IRedisCache.class));
        ReflectionTestUtils.setField(cache, "redisClientFactory", factory);

        cache.get("key");

        ArgumentCaptor<RedisCodec<String, String>> codec = ArgumentCaptor.forClass(RedisCodec.class);
        verify(factory).getClient(eq(String.class), any(RedisAzureConfiguration.class), codec.capture());
        return codec.getValue().getClass();
    }
}
