// Copyright 2017-2024, Schlumberger
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

package org.opengroup.osdu.storage.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.opengroup.osdu.storage.request.ReplayRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

  /**
   * Strict (fail on unknown properties) converter for ReplayRequest only. Deliberately not a bean:
   * Spring Boot 4 skips its general Jackson 2 JSON converter when any MappingJackson2HttpMessageConverter
   * bean exists, which would leave all other responses to Gson.
   */
  private final MappingJackson2HttpMessageConverter strictConverter = createStrictReplayRequestConverter();

  /**
   * Spring Framework 7 removed trailing slash path matching; this filter keeps "/path/" and "/path"
   * mapped to the same handler, ahead of security and request logging filters.
   */
  @Bean
  public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
    FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(
        UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
    return registration;
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(0, strictConverter);
  }

  static MappingJackson2HttpMessageConverter createStrictReplayRequestConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    return new MappingJackson2HttpMessageConverter(objectMapper) {
      @Override
      public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return ReplayRequest.class.isAssignableFrom(clazz) && MediaType.APPLICATION_JSON.includes(mediaType);
      }

      @Override
      public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return ReplayRequest.class.isAssignableFrom(clazz) && MediaType.APPLICATION_JSON.includes(mediaType);
      }
    };
  }

}
