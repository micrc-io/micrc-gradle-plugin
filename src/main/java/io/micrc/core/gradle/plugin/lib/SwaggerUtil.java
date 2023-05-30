package io.micrc.core.gradle.plugin.lib;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.IOException;

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

    /**
     * 转json时SecuritySchemeType的序列化方式
     */
    public static class SecuritySchemeTypeSerializer extends JsonSerializer<SecurityScheme.Type> {

        @Override
        public void serialize(SecurityScheme.Type type, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(type.toString());
        }
    }

    /**
     * 转json时SecuritySchemeIn的序列化方式
     */
    public static class SecuritySchemeInSerializer extends JsonSerializer<SecurityScheme.In> {
        @Override
        public void serialize(SecurityScheme.In in, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(in.toString());
        }
    }
}
