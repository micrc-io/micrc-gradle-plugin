package io.micrc.core.gradle.plugin.lib;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;

public final class SwaggerUtil {
    private SwaggerUtil() {}

    private static final OpenAPIParser PARSER = new OpenAPIParser();

    private static final ParseOptions OPTIONS = new ParseOptions();

    static {
        OPTIONS.setResolveFully(true); // 替换$ref
    }

    public static OpenAPI readOpenApi(String apiContent) {
        return PARSER.readContents(apiContent, null, OPTIONS).getOpenAPI();
    }
}
