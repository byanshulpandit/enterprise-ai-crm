package com.crm.platform.common.controller;

import com.crm.platform.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.PathContainer;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Catch-all controller for unmapped /api/** endpoints to guarantee that unhandled API requests
 * return a uniform 404 RESOURCE_NOT_FOUND error response instead of forwarding to Vaadin or container error pages.
 *
 * If a request hits this fallback controller but an existing endpoint matches the path pattern
 * with different HTTP methods, it raises HttpRequestMethodNotSupportedException so that
 * GlobalExceptionHandler can return 405 METHOD_NOT_ALLOWED.
 */
@RestController
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiFallbackController {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public ApiFallbackController(@Lazy RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    @RequestMapping("/api/**")
    public void handleUnmappedApi(HttpServletRequest request) throws HttpRequestMethodNotSupportedException {
        String path = request.getRequestURI();
        PathContainer pathContainer = PathContainer.parsePath(path);

        Set<String> supportedMethods = new HashSet<>();
        if (requestMappingHandlerMapping != null) {
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
                if (entry.getValue().getBeanType() == ApiFallbackController.class) {
                    continue;
                }
                RequestMappingInfo info = entry.getKey();
                if (info.getPathPatternsCondition() != null) {
                    for (PathPattern pattern : info.getPathPatternsCondition().getPatterns()) {
                        if (pattern.matches(pathContainer)) {
                            if (info.getMethodsCondition() != null && !info.getMethodsCondition().getMethods().isEmpty()) {
                                for (RequestMethod rm : info.getMethodsCondition().getMethods()) {
                                    supportedMethods.add(rm.name());
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!supportedMethods.isEmpty() && !supportedMethods.contains(request.getMethod())) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), supportedMethods);
        }

        throw new ResourceNotFoundException("Endpoint '" + path + "' was not found.");
    }
}
