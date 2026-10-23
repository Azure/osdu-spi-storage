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

import io.lettuce.core.codec.RedisCodec;
import java.nio.ByteBuffer;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.storage.provider.azure.di.RedisConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("LegalTagStore")
@ConditionalOnProperty(value = "runtime.env.local", havingValue = "false", matchIfMissing = true)
public class LegalTagRedisCache extends RedisAzureCache<String> {

    public LegalTagRedisCache(final RedisConfig config) {
        super(String.class, config.createLegalTagConfiguration());
    }

    // RedisClientFactory shares one client, and its TTL, per value and codec class; a codec class of our own keeps this TTL apart from CursorRedisCache.
    @Override
    public RedisCodec<String, String> getCodec(final Class<String> classOfK, final Class<String> classOfV) {
        return new LegalTagCodec(super.getCodec(classOfK, classOfV));
    }

    static final class LegalTagCodec implements RedisCodec<String, String> {

        private final RedisCodec<String, String> delegate;

        LegalTagCodec(RedisCodec<String, String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return delegate.decodeKey(bytes);
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return delegate.decodeValue(bytes);
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return delegate.encodeKey(key);
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            return delegate.encodeValue(value);
        }
    }
}
