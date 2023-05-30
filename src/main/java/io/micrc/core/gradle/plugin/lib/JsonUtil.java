package io.micrc.core.gradle.plugin.lib;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class JsonUtil {
    private JsonUtil() {}

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ObjectMapper OBJECT_NULL_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL);
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT); // 美化输出

        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        OBJECT_NULL_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		/*
		// 序列化和反序列化的时候针对浮点类型使用BigDecimal转换，避免精度损失和科学计数法
		OBJECT_MAPPER.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		SimpleModule simpleModule = new SimpleModule();
		simpleModule.addSerializer(Double.class, BigDecimalForDoubleSerializer.SINGLETON);
		OBJECT_MAPPER.registerModule(simpleModule);
		OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL);
		 */

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SecurityScheme.Type.class, new SwaggerUtil.SecuritySchemeTypeSerializer());
        simpleModule.addSerializer(SecurityScheme.In.class, new SwaggerUtil.SecuritySchemeInSerializer());
        OBJECT_MAPPER.registerModule(simpleModule);
        OBJECT_NULL_MAPPER.registerModule(simpleModule);
    }

    public static String writeValueAsString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String writeValueAsStringRetainNull(Object object) {
        try {
            return OBJECT_NULL_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public static <T> T write2Object(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static <T> T writeValueAsObject(String json, Class<T> targetClass) {
        try {
            return OBJECT_MAPPER.readValue(json, targetClass);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static <T> T writeValueAsObjectRetainNull(String json, Class<T> targetClass) {
        try {
            return OBJECT_NULL_MAPPER.readValue(json, targetClass);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> writeValueAsList(String json, Class<T> targetClass) {
        try {
            CollectionType listType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, targetClass);
            return OBJECT_MAPPER.readValue(json, listType);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static <T> T writeObjectAsObject(Object object, Class<T> targetClass) {
        return writeValueAsObject(writeValueAsString(object), targetClass);
    }

    public static <T> List<T> writeObjectAsList(Object object, Class<T> targetClass) {
        return writeValueAsList(writeValueAsString(object), targetClass);
    }

    public static JsonNode readTree(Object obj) {
        try {
            if (obj instanceof String) {
                return OBJECT_NULL_MAPPER.readTree((String) obj);
            }
            return OBJECT_NULL_MAPPER.readTree(JsonUtil.writeValueAsString(obj));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Object readPath(String json, String path) {
        try {
            JsonNode jsonNode = readTree(json).at(path);
            if (jsonNode instanceof ObjectNode) {
                return writeValueAsObject(jsonNode.toString(), Object.class);
            }
            if (jsonNode instanceof ArrayNode) {
                return writeValueAsObject(jsonNode.toString(), List.class);
            }
            if (jsonNode instanceof TextNode) {
                return jsonNode.textValue();
            }
            if (jsonNode instanceof BinaryNode) {
                return jsonNode.binaryValue();
            }
            if (jsonNode instanceof ShortNode) {
                return jsonNode.shortValue();
            }
            if (jsonNode instanceof IntNode) {
                return jsonNode.intValue();
            }
            if (jsonNode instanceof LongNode) {
                return jsonNode.longValue();
            }
            if (jsonNode instanceof BigIntegerNode) {
                return jsonNode.bigIntegerValue();
            }
            if (jsonNode instanceof DecimalNode) {
                return jsonNode.decimalValue();
            }
            if (jsonNode instanceof FloatNode) {
                return jsonNode.floatValue();
            }
            if (jsonNode instanceof DoubleNode) {
                return jsonNode.doubleValue();
            }
            if (jsonNode instanceof BooleanNode) {
                return jsonNode.booleanValue();
            }
            // 路径不存在，返回null
            if (jsonNode instanceof MissingNode || jsonNode instanceof NullNode) {
                return null;
            }
            throw new UnsupportedOperationException(jsonNode.getClass().getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String patch(String original, String path, String value) {
        String patchCommand = "[{ \"op\": \"replace\", \"path\": \"{{path}}\", \"value\": {{value}} }]";

        try {
            String pathReplaced = patchCommand.replace("{{path}}", path);
            String valueReplaced = pathReplaced.replace("{{value}}", value);
            JsonPatch patch = JsonPatch.fromJson(JsonUtil.readTree(valueReplaced));
            return JsonUtil.writeValueAsStringRetainNull(patch.apply(JsonUtil.readTree(original)));
        } catch (IOException | JsonPatchException e) {
            throw new RuntimeException("patch fail... please check object...");
        }
    }

    public static String add(String original, String path, String value) {
        String patchCommand = "[{ \"op\": \"add\", \"path\": \"{{path}}\", \"value\": {{value}} }]";
        try {
            String pathReplaced = patchCommand.replace("{{path}}", path);
            String valueReplaced = pathReplaced.replace("{{value}}", value);
            JsonPatch patch = JsonPatch.fromJson(JsonUtil.readTree(valueReplaced));
            return JsonUtil.writeValueAsStringRetainNull(patch.apply(JsonUtil.readTree(original)));
        } catch (IOException | JsonPatchException e) {
            throw new RuntimeException("patch fail... please check object...");
        }
    }

    public static String supplementNotExistsNode(String json, String targetPath) {
        String[] split = targetPath.split("/");
        String p = "";
        for (int i = 1; i < split.length; i++) {
            p = p + "/" + split[i];
            Object o = readPath(json, p);
            if (null == o) {
                json = add(json, p, "{}");
            }
        }
        return json;
    }
}