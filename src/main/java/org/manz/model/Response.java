package org.manz.model;

import org.manz.model.Payload;

import java.util.Map;

public record Response(int status, Map<String, String> headers, Payload responseBody) { }