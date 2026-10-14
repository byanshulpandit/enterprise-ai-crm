package com.crm.platform.frontend.client;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Robust typed HTTP client communicating exclusively with the backend via /api/v1/* REST endpoints.
 */
@Component
public class BackendApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BackendApiClient(RestClient backendRestClient, ObjectMapper frontendObjectMapper) {
        this.restClient = backendRestClient;
        this.objectMapper = frontendObjectMapper;
    }

    public <T> T get(String uri, Class<T> responseType, Object... uriVariables) {
        try {
            JsonNode root = restClient.get()
                    .uri(uri, uriVariables)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return null;
            }
            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                return null;
            }
            return objectMapper.treeToValue(dataNode, responseType);
        } catch (RestClientResponseException ex) {
            throw translateException(ex);
        } catch (Exception ex) {
            throw new ApiException(500, "ERR_INTERNAL", ex.getMessage());
        }
    }

    public <T> T get(String uri, ParameterizedTypeReference<ApiResponse<T>> typeRef, Object... uriVariables) {
        try {
            ApiResponse<T> response = restClient.get()
                    .uri(uri, uriVariables)
                    .retrieve()
                    .body(typeRef);

            return response != null ? response.getData() : null;
        } catch (RestClientResponseException ex) {
            throw translateException(ex);
        } catch (Exception ex) {
            throw new ApiException(500, "ERR_INTERNAL", ex.getMessage());
        }
    }

    public <T> PageResult<T> getPage(String uri, Class<T> itemType, Map<String, Object> queryParams, Object... uriVariables) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);
            if (queryParams != null) {
                queryParams.forEach((k, v) -> {
                    if (v != null) {
                        builder.queryParam(k, v);
                    }
                });
            }
            String resolvedUri = builder.buildAndExpand(uriVariables).toUriString();

            JsonNode root = restClient.get()
                    .uri(resolvedUri)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return new PageResult<>(Collections.emptyList(), null);
            }

            List<T> items = new ArrayList<>();
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
                for (JsonNode itemNode : dataNode) {
                    items.add(objectMapper.treeToValue(itemNode, itemType));
                }
            }

            PageMetadata metadata = null;
            JsonNode paginationNode = root.path("metadata").path("pagination");
            if (!paginationNode.isMissingNode() && !paginationNode.isNull()) {
                metadata = objectMapper.treeToValue(paginationNode, PageMetadata.class);
            }

            return new PageResult<>(items, metadata);
        } catch (RestClientResponseException ex) {
            throw translateException(ex);
        } catch (Exception ex) {
            throw new ApiException(500, "ERR_INTERNAL", ex.getMessage());
        }
    }

    public <T> T post(String uri, Object requestBody, Class<T> responseType, Object... uriVariables) {
        try {
            JsonNode root = restClient.post()
                    .uri(uri, uriVariables)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody != null ? requestBody : Collections.emptyMap())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return null;
            }
            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                return null;
            }
            return objectMapper.treeToValue(dataNode, responseType);
        } catch (RestClientResponseException ex) {
            throw translateException(ex);
        } catch (Exception ex) {
            throw new ApiException(500, "ERR_INTERNAL", ex.getMessage());
        }
    }

    public <T> T put(String uri, Object requestBody, Class<T> responseType, Object... uriVariables) {
        try {
            JsonNode root = restClient.put()
                    .uri(uri, uriVariables)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody != null ? requestBody : Collections.emptyMap())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return null;
            }
            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                return null;
            }
            return objectMapper.treeToValue(dataNode, responseType);
        } catch (RestClientResponseException ex) {
            throw translateException(ex);
        } catch (Exception ex) {
            throw new ApiException(500, "ERR_INTERNAL", ex.getMessage());
        }
    }

    public <T> T patch(String uri, Object requestBody, Class<T> responseType, Object... uriVariables) {
        try {
            JsonNode root = restClient.patch()
                    .uri(uri, uriVariables)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody != null ? requestBody : Collections.emptyMap())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return null;
            }
            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                return null;
            }
            return objectMapper.treeToValue(dataNode, responseType);
        } catch (RestClientResponseException ex) {
            throw translateException(ex);
        } catch (Exception ex) {
            throw new ApiException(500, "ERR_INTERNAL", ex.getMessage());
        }
    }

    public void delete(String uri, Object... uriVariables) {
        try {
            restClient.delete()
                    .uri(uri, uriVariables)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw translateException(ex);
        } catch (Exception ex) {
            throw new ApiException(500, "ERR_INTERNAL", ex.getMessage());
        }
    }

    public <T> T uploadMultipart(String uri, String paramName, String filename, byte[] content, Class<T> responseType) {
        try {
            ByteArrayResource resource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(paramName, resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            JsonNode root = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(requestEntity)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return null;
            }
            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                return null;
            }
            return objectMapper.treeToValue(dataNode, responseType);
        } catch (RestClientResponseException ex) {
            throw translateException(ex);
        } catch (Exception ex) {
            throw new ApiException(500, "ERR_INTERNAL", ex.getMessage());
        }
    }

    private ApiException translateException(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String responseBody = ex.getResponseBodyAsString();
        String code = "ERR_HTTP_" + status;
        String message = ex.getStatusText();
        List<String> detailsList = new ArrayList<>();

        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode errorRoot = objectMapper.readTree(responseBody);
                JsonNode errorNode = errorRoot.path("error");
                if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                    if (errorNode.has("code")) {
                        code = errorNode.path("code").asText(code);
                    }
                    if (errorNode.has("message")) {
                        message = errorNode.path("message").asText(message);
                    }
                    JsonNode detailsNode = errorNode.path("details");
                    if (detailsNode.isArray()) {
                        for (JsonNode detail : detailsNode) {
                            String field = detail.path("field").asText("");
                            String msg = detail.path("message").asText("");
                            if (!field.isBlank() && !msg.isBlank()) {
                                detailsList.add(field + ": " + msg);
                            } else if (!msg.isBlank()) {
                                detailsList.add(msg);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                message = responseBody;
            }
        }

        return new ApiException(status, code, message, detailsList);
    }
}
