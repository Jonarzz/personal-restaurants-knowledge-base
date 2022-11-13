package springfox.documentation.spring.web.plugins;/*
 *
 *  Copyright 2015-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

import static java.util.stream.Collectors.*;
import static springfox.documentation.builders.BuilderDefaults.*;
import static springfox.documentation.spi.service.contexts.Orderings.*;
import static springfox.documentation.spring.web.paths.Paths.*;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.core.*;
import org.springframework.core.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.web.method.*;
import org.springframework.web.servlet.mvc.method.*;
import springfox.documentation.*;
import springfox.documentation.spi.service.*;
import springfox.documentation.spring.web.*;
import springfox.documentation.spring.web.readers.operation.*;

import javax.servlet.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Conditional(OnServletBasedWebApplication.class)
public class WebMvcRequestHandlerProvider implements RequestHandlerProvider {

  // SpringFox compatibility hack: https://github.com/springfox/springfox/issues/3462#issuecomment-978707909

  private final List<RequestMappingInfoHandlerMapping> handlerMappings;
  private final HandlerMethodResolver methodResolver;
  private final String contextPath;

  public WebMvcRequestHandlerProvider(Optional<ServletContext> servletContext, HandlerMethodResolver methodResolver,
                                      List<RequestMappingInfoHandlerMapping> handlerMappings) {
    this.handlerMappings = handlerMappings.stream()
                                          .filter(mapping -> mapping.getPatternParser() == null)
                                          .collect(toList());
    this.methodResolver = methodResolver;
    contextPath = servletContext
            .map(ServletContext::getContextPath)
            .orElse(ROOT);
  }

  @Override
  public List<RequestHandler> requestHandlers() {
    return nullToEmptyList(handlerMappings).stream()
        .filter(requestMappingInfoHandlerMapping ->
            !("org.springframework.integration.http.inbound.IntegrationRequestMappingHandlerMapping"
                  .equals(requestMappingInfoHandlerMapping.getClass()
                      .getName())))
        .map(toMappingEntries())
        .flatMap((entries -> StreamSupport.stream(entries.spliterator(), false)))
        .map(toRequestHandler())
        .sorted(byPatternsCondition())
        .collect(toList());
  }

  private Function<RequestMappingInfoHandlerMapping,
      Iterable<Map.Entry<RequestMappingInfo, HandlerMethod>>> toMappingEntries() {
    return input -> input.getHandlerMethods()
        .entrySet();
  }

  private Function<Map.Entry<RequestMappingInfo, HandlerMethod>, RequestHandler> toRequestHandler() {
    return input -> new WebMvcRequestHandler(
        contextPath,
        methodResolver,
        input.getKey(),
        input.getValue());
  }
}
