package org.manz;

import java.util.Map;

record Response(int status, Map<String, String> headers, Payload responseBody) { }