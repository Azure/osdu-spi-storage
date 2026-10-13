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

package org.opengroup.osdu.storage.provider.azure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThreadScopeTest {
    @Mock
    ObjectFactory<?> factory;
    @Mock
    Object obj;
    @InjectMocks
    ThreadScope sut;
    @Mock
    ThreadScopeContext context;
    @Mock
    ThreadDpsHeaders threadDpsHeaders;
    @Mock
    DpsHeaders dpsHeaders;
    @Mock
    MockHttpServletRequest request;
    @Mock
    MDC mdcContext;
    private final String name = "name";

    @Test
    void getObjectShouldCleanupContext_AndReturnNonNullObjectOfTypeDpsHeaders() {
        Map<String, String> headers = getSomeHeadersInAMap();
        Enumeration<String> headerNames = Collections.enumeration(headers.keySet());
        when(request.getHeaderNames()).thenReturn(headerNames);
        when(request.getHeader(any())).thenReturn("header");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        obj = sut.get("bean-name", factory);

        assertNotNull(obj);
        assertEquals(obj.getClass(), dpsHeaders.getClass());
    }

    @Test
    void get_shouldReturnObjectFromSuppliedFactory_ifRequestAttributesAreMissing() {
        RequestContextHolder.resetRequestAttributes();
        ObjectFactory<Object> objectFactory = mock(ObjectFactory.class);
        when(objectFactory.getObject()).thenReturn(obj);

        Object objOut = sut.get(name, objectFactory);

        assertEquals(obj, objOut);
    }

    private Map<String, String> getSomeHeadersInAMap() {
        Map<String, String> headers = new HashMap<>();
        headers.put("header1", "value1");
        headers.put("Content-Type", "text/html");
        return headers;
    }
}
