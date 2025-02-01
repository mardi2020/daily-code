package org.mardi2020.tomcat.server;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class MyResponseFactory {

    private static MyResponse createResponse(int status, String body) {
        return MyResponse.builder()
                .statusCode(status)
                .body(new StringBuilder(body))
                .headers(new HashMap<>())
                .build();
    }

    private static MyResponse createResponse(int status, String body, Map<String, String> headers) {
        return MyResponse.builder()
                .statusCode(status)
                .body(new StringBuilder(body))
                .headers(headers)
                .build();
    }

    public static MyResponse create(String body, String path, boolean isExistServlet) {
        if (path == null || path.isEmpty() || !isExistServlet) {
            return createResponse(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase());
        }
        return createResponse(HttpStatus.OK.value(), body);
    }
}
