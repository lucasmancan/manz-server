package org.manz.model;

import java.util.List;

public record Route(HttpMethod method, String path, List<String> queryParametersNames, List<String> pathParametersNames){}
