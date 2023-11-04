package org.manz;

import java.util.Map;
import java.util.Optional;

record Request(String method, String path, Map<String, String> headers, Optional<Payload> requestBody) { }