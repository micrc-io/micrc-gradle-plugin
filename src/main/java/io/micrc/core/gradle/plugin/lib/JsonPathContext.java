package io.micrc.core.gradle.plugin.lib;

import groovy.util.Eval;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import org.apache.groovy.json.internal.LazyMap;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonPathContext {
    private final Object object;
    private static final String REGEX = "\\{([a-zA-Z]+)\\}";
    public JsonPathContext(Object object) {
        this.object = object;
    }
    public static String path(String path, String... args) {
        String newPath = path;
        for (String arg : args) {
            Pattern pattern = Pattern.compile(REGEX);
            Matcher matcher = pattern.matcher(newPath);
            newPath = matcher.replaceFirst(arg);
        }
        return newPath;
    }

    public Object get(String path) {
      return   Eval.x(object, "x.content."+ path);
    }
    public Object get(String path, String... args) {
      String newPath = path(path, args);
      return get(newPath);
    }

    public LazyMap getMap(String path) {
        Object object = get(path);
        return JsonUtil.writeObjectAsObject(object, LazyMap.class);
    }

    public LazyMap getMap(String path, String... args) {
        Object object = get(path, args);
        return JsonUtil.writeObjectAsObject(object, LazyMap.class);
    }

    public List<?> getList(String path) {
        Object object = get(path);
        return JsonUtil.writeObjectAsList(object, List.class);
    }

    public List<?> getList(String path, String... args) {
        Object object = get(path, args);
        return JsonUtil.writeObjectAsList(object, List.class);
    }
}
