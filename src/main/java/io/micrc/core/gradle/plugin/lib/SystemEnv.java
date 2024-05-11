package io.micrc.core.gradle.plugin.lib;

import org.gradle.api.Project;

public class SystemEnv {
    public static final String DEFAULT_PROFILE= "default";

    public static String getActiveProfile(Project project) {
        if (project.hasProperty("active_profile")) {
            return (String) project.property("active_profile");
        }
        return DEFAULT_PROFILE;
    }
}
