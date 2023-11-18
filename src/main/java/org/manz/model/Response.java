package org.manz.model;

import org.manz.model.Payload;

import java.util.Collections;
import java.util.Map;

public record Response(int status, Map<String, String> headers, Payload responseBody) {
    public static final Response NOT_FOUND = new Response(404, Collections.emptyMap(), null);
}