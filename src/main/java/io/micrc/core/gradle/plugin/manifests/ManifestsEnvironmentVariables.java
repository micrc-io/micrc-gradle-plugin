package io.micrc.core.gradle.plugin.manifests;

import java.util.HashMap;
import java.util.Map;

public class ManifestsEnvironmentVariables {

    public static final Map<String, String> envs = new HashMap<>();

    static {
        // system env placeholder
        envs.put("IMAGE", "$IMAGE");
    }

}