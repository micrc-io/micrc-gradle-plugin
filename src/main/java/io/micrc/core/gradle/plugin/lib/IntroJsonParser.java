package io.micrc.core.gradle.plugin.lib;

import org.gradle.api.Project;

import java.util.List;

public class IntroJsonParser {
    // prod env
    private static final List<String> prod = List.of("alpha","beta","ga");
    // no kustomize template in resources
    private static final List<String> noTemplate = List.of(SystemEnv.DEFAULT_PROFILE,"ver","acc");
    // test env
    private static boolean isTestProfile(String profile) {
        return List.of("local", "default").contains(profile);
    }
    // integration env
    private static boolean isIntegrationProfile(String profile) {
        return List.of("dev").contains(profile);
    }
    // production env
    public static boolean isProdProfile(String profile) {
        return prod.contains(profile);
    }

    // get intro.json profile
    public static String getIntroJsonProfile(Project project) {
        String envProfile = SystemEnv.getActiveProfile(project);
        if(isProdProfile(envProfile)) {
            // intro.json only profile is prod for alpha,beta,ga
            return "prod";
        }
        //TODO csyang keep default profile run ok
        if (noTemplate.contains(envProfile)) {
            return "dev";
        }
        return envProfile;
    }

    // topic name suffix
    public static String topicNameSuffixProfile(String topicName,String profile) {
        String envCamelcase = profile.substring(0, 1).toUpperCase() + profile.substring(1);
        // prod ga env need topic suffix  and no profile template
        if("ga".equals(profile) || noTemplate.contains(profile)) {
            return topicName;
        }
        return topicName+envCamelcase;
    }

    // get profile to  gen snips/*/tmpl
    public static String getProfileBetweenIntroJsonAndEnvProfile(String introJsonProfile,String envProfile) {
        if ("prod".equals(introJsonProfile)) {
            if (prod.contains(envProfile)) {
                return envProfile;
            }else {
                // is prod and not match alpha,beta,ga; so get null
                return null;
            }
        }
        return introJsonProfile;
    }

    // ignore default profile ,reason tmpl has no default
    public static String getProfileIgnoreAnyProfile(String introJsonProfile,String envProfile) {
        if (noTemplate.contains(introJsonProfile)) {
            return null;
        }
        return getProfileBetweenIntroJsonAndEnvProfile(introJsonProfile,envProfile);
    }
}
