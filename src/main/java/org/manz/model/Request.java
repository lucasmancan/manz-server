package org.manz.model;

import org.manz.model.Payload;

import java.util.Map;
import java.util.Optional;

public record Request(String method, String path, Map<String, String> headers, Optional<Payload> requestBody) { }