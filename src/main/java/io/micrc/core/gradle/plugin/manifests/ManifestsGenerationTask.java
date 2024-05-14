package io.micrc.core.gradle.plugin.manifests;

import groovy.util.Eval;
import io.micrc.core.gradle.plugin.lib.IntroJsonParser;
import io.micrc.core.gradle.plugin.lib.SystemEnv;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ManifestsGenerationTask {

    private static final String MICRC_MANIFESTS_BUILD_DIR = "micrc" + File.separator + "manifests";

    private static ManifestsGenerationTask instance;

    private ManifestsGenerationTask() {
    }

    public static ManifestsGenerationTask newInstance() {
        if (instance == null) {
            instance = new ManifestsGenerationTask();
        }
        return instance;
    }


    @SuppressWarnings("unchecked")
    public void generateManifests(Project project) {
        String activeProfile = SystemEnv.getActiveProfile(project);
        log.info("generate manifests");
        String buildDirPath = project.getBuildDir().getAbsolutePath() + File.separator + MICRC_MANIFESTS_BUILD_DIR;
        TemplateUtils.clearDir(Path.of(buildDirPath));
        Optional<Object> contextMeta = Optional.ofNullable(SchemaSynchronizeConfigure.metaData.get("contextMeta"));
        Optional<String> ownerDomain = Optional.ofNullable(
                (String) Eval.x(contextMeta.orElseThrow(), "x.content.ownerDomain")
        );
        String name = project.getName().replace("-service", "");
        Optional<String> namespace = Optional.ofNullable(
                (String) Eval.x(contextMeta.orElseThrow(), "x.content.namespace")
        );
        Optional<String> entry = Optional.ofNullable(
                (String) Eval.x(contextMeta.orElseThrow(), "x.content.gateway.entry")
        );
        Optional<String> fqdn = Optional.ofNullable(
                (String) Eval.x(contextMeta.orElseThrow(), "x.content.gateway.fqdn")
        );
        Map<String, String> ctx = new HashMap<>(Map.of(
                "ownerDomain", ownerDomain.orElseThrow(),
                "name", name,
                "version", project.getVersion().toString(),
                "desc", String.format("%s chart for kubernetes", name),
                "namespace", namespace.orElseThrow() + "-" + ownerDomain.orElseThrow(),
                "entry", entry.orElseThrow(),
                "fqdn", fqdn.orElseThrow()
        ));

        // 处理中间件snip模版
        Optional<Map<String, Object>> middlewares = Optional.ofNullable(
                (Map<String, Object>) Eval.x(contextMeta.orElseThrow(), "x.content.server.middlewares")
        );
        middlewares.orElseThrow().keySet().forEach(middleware -> {
            boolean enabled =
                ((Map<String, Object>) middlewares.orElseThrow().get(middleware)).get("enabled").equals("true");
            Optional<Map<String, Object>> profiles = Optional.ofNullable(
                (Map<String, Object>) ((Map<String, Object>) middlewares.orElseThrow().get(middleware)).get("profiles")
            );
            if (enabled) {
                ctx.put(
                        middleware + "_provider",
                        (String) ((Map<String, Object>) middlewares.orElseThrow().get(middleware)).get("provider")
                );
                ctx.put(
                        middleware + "_providers",
                        String.join(",", profiles.orElseThrow().keySet())
                );
            }
            middlewareBaseSnipsTemplateContext(ctx, buildDirPath, middleware, enabled);
            middlewareProfilesSnipsTemplateContext(ctx, profiles.orElseThrow(), buildDirPath, middleware, enabled, activeProfile);
        });

        // 移除意外产生的out-PrintWriter
        ctx.remove("out");
        // 生成service helm deployment
        generateHelm(ctx, buildDirPath, name);
        // 生成kustomize base/local/dev
        generateKustomize(ctx, buildDirPath, activeProfile);
    }

    private void generateHelm(Map<String, String> ctx, String buildDirPath, String contextName) {
        // helm Chart.yaml
        List<String> chartPaths =
                List.of("tmpl", "manifest", "k8s", "helm", "Chart.yaml");
        List<String> chartTargetPaths = List.of("k8s", "helm", contextName, "Chart.yaml");
        TemplateUtils.generate(ctx, buildDirPath, chartPaths, chartTargetPaths);
        // helm values.yaml
        List<String> valuesPaths =
                List.of("tmpl", "manifest", "k8s", "helm", "values.yaml");
        List<String> valuesTargetPaths = List.of("k8s", "helm", contextName, "values.yaml");
        TemplateUtils.generate(ctx, buildDirPath, valuesPaths, valuesTargetPaths);
        // helm .helmignore
        List<String> helmIgnorePaths =
                List.of("tmpl", "manifest", "k8s", "helm", ".helmignore");
        List<String> helmIgnoreTargetPaths = List.of("k8s", "helm", contextName, ".helmignore");
        TemplateUtils.generate(ctx, buildDirPath, helmIgnorePaths, helmIgnoreTargetPaths);
        // helm templates _helpers.tpl
        List<String> helpersPaths =
                List.of("tmpl", "manifest", "k8s", "helm", "templates", "_helpers.tpl");
        List<String> helpersTargetPaths = List.of("k8s", "helm", contextName, "templates", "_helpers.tpl");
        TemplateUtils.generate(ctx, buildDirPath, helpersPaths, helpersTargetPaths);
        // helm templates serviceaccount.yaml
        List<String> serviceaccountPaths =
                List.of("tmpl", "manifest", "k8s", "helm", "templates", "serviceaccount.yaml");
        List<String> serviceaccountTargetPaths =
                List.of("k8s", "helm", contextName, "templates", "serviceaccount.yaml");
        TemplateUtils.generate(ctx, buildDirPath, serviceaccountPaths, serviceaccountTargetPaths);
        // helm templates hpa.yaml
        List<String> hpaPaths =
                List.of("tmpl", "manifest", "k8s", "helm", "templates", "hpa.yaml");
        List<String> hpaTargetPaths = List.of("k8s", "helm", contextName, "templates", "hpa.yaml");
        TemplateUtils.generate(ctx, buildDirPath, hpaPaths, hpaTargetPaths);
        // helm templates service.yaml
        List<String> servicePaths =
                List.of("tmpl", "manifest", "k8s", "helm", "templates", "service.yaml");
        List<String> serviceTargetPaths = List.of("k8s", "helm", contextName, "templates", "service.yaml");
        TemplateUtils.generate(ctx, buildDirPath, servicePaths, serviceTargetPaths);
        // helm templates deployment.yaml
        List<String> deploymentPaths =
                List.of("tmpl", "manifest", "k8s", "helm", "templates", "deployment.yaml");
        List<String> deploymentTargetPaths = List.of("k8s", "helm", contextName, "templates", "deployment.yaml");
        TemplateUtils.generate(ctx, buildDirPath, deploymentPaths, deploymentTargetPaths);
    }

    private void generateKustomize(Map<String, String> ctx, String buildDirPath, String activeProfile) {
        // kustomize base kustomization.yaml and sealed-secret.yaml
        List<String> kustomizeBasePaths =
                List.of("tmpl", "manifest", "k8s", "kustomize", "base", "kustomization.yaml");
        List<String> kustomizeBaseTargetPaths = List.of("k8s", "kustomize", "base", "kustomization.yaml");
        TemplateUtils.generate(ctx, buildDirPath, kustomizeBasePaths, kustomizeBaseTargetPaths);
        // kustomize local kustomization.yaml
        List<String> kustomizeLocalPaths =
                List.of("tmpl", "manifest", "k8s", "kustomize", "local", "kustomization.yaml");
        List<String> kustomizeLocalTargetPaths = List.of("k8s", "kustomize", "local", "kustomization.yaml");
        TemplateUtils.generate(ctx, buildDirPath, kustomizeLocalPaths, kustomizeLocalTargetPaths);
        // kustomize dev kustomization.yaml
        List<String> kustomizeDevPaths =
                List.of("tmpl", "manifest", "k8s", "kustomize", "dev", "kustomization.yaml");
        List<String> kustomizeDevTargetPaths = List.of("k8s", "kustomize", "dev", "kustomization.yaml");
        TemplateUtils.generate(ctx, buildDirPath, kustomizeDevPaths, kustomizeDevTargetPaths);
        List<String> kustomizeDevSecretPaths =
                List.of("tmpl", "manifest", "k8s", "kustomize", "dev", "sealed-secret.yaml");
        List<String> kustomizeDevSecretTargetPaths = List.of("k8s", "kustomize", "dev", "sealed-secret.yaml");
        TemplateUtils.generate(ctx, buildDirPath, kustomizeDevSecretPaths, kustomizeDevSecretTargetPaths);
        List<String> kustomizeDevVirtualServicePaths =
                List.of("tmpl", "manifest", "k8s", "kustomize", "dev", "traffic-manager.yaml");
        List<String> kustomizeDevVirtualServiceTargetPaths = List.of("k8s", "kustomize", "dev", "traffic-manager.yaml");
        TemplateUtils.generate(
                ctx, buildDirPath, kustomizeDevVirtualServicePaths, kustomizeDevVirtualServiceTargetPaths
        );

        if (IntroJsonParser.isProdProfile(activeProfile)) {
            // kustomize prod kustomization.yaml
            List<String> kustomizeProdPaths =
                    List.of("tmpl", "manifest", "k8s", "kustomize", activeProfile, "kustomization.yaml");
            List<String> kustomizeProdTargetPaths = List.of("k8s", "kustomize", activeProfile, "kustomization.yaml");
            TemplateUtils.generate(ctx, buildDirPath, kustomizeProdPaths, kustomizeProdTargetPaths);
            List<String> kustomizeProdSecretPaths =
                    List.of("tmpl", "manifest", "k8s", "kustomize", activeProfile, "sealed-secret.yaml");
            List<String> kustomizeProdSecretTargetPaths = List.of("k8s", "kustomize", activeProfile, "sealed-secret.yaml");
            TemplateUtils.generate(ctx, buildDirPath, kustomizeProdSecretPaths, kustomizeProdSecretTargetPaths);
        }
    }


    // 渲染中间件profiles snip模版，并将渲染后的字符串加入上下文中，用于主文件渲染
    @SuppressWarnings("unchecked")
    private void middlewareProfilesSnipsTemplateContext(
            Map<String, String> ctx, Map<String, Object> providerProfiles,
            String buildDirPath, String middleware, boolean enabled, String activeProfile) {
        Map<String, String> profilesData = new HashMap<>();
        HashSet<String> profileSet = new HashSet<>();
        providerProfiles.keySet().forEach(provider -> {
            Map<String, Object> profiles = (Map<String, Object>) providerProfiles.get(provider);

            profiles.keySet().forEach(profileKey -> {
                String activeProfileKey = IntroJsonParser.getProfileIgnoreAnyProfile(profileKey,activeProfile);
                if (null == activeProfileKey) {
                    return;
                }

                String profilePropertiesKey = middleware + "_properties_" + activeProfileKey;
                profilesData.putIfAbsent(profilePropertiesKey, "");
                Map<String, Object> profile = (Map<String, Object>) profiles.get(profileKey);
                if (!profile.keySet().isEmpty()) {
                    Map<String,String> instance = (Map<String,String>)profile.get("instance");
                    Optional.of(instance).orElseThrow().keySet().forEach(prop -> {
                        String profileData = profilesData.get(profilePropertiesKey);
                        profileData += "\n    ";
                        profileData += provider + "_" + middleware + "_" + prop + ": " + instance.get(prop);
                        profilesData.put(profilePropertiesKey, profileData);
                    });
                    // gen template broker topics
                    Map<String,Object> resources = (Map<String,Object>)profile.get("resources");
                    if (resources != null) {
                        List<String> topics = (List<String>)resources.get("topics");
                        if (topics != null) {
                            String profileBrokerKey = middleware + "_template_" + activeProfileKey;
                            String brokerProperties = profilesData.get(profileBrokerKey);
                            if (null == brokerProperties) {
                                brokerProperties="";
                            }
                            Function<String,String> topicSuffix = topic -> IntroJsonParser.topicNameSuffixProfile(topic,activeProfileKey);
                            String newTopics = topics.stream().map(topicSuffix).collect(Collectors.joining(","));
                            if (!newTopics.isEmpty()) {
                                brokerProperties+= "\n      "+provider + "_broker_topics: "+ newTopics;
                            }
                            profilesData.put(profileBrokerKey, brokerProperties);
                        }
                    }

                    if (!instance.keySet().isEmpty()) {
                        profileSet.add(activeProfileKey);
                    }
                }

            });
        });
        ctx.putAll(profilesData);
        profileSet.forEach(profileKey ->
            ctx.put(
                middleware + "_secret_sealed_" + profileKey,
                enabled ? TemplateUtils.generate(
                    ctx,
                    buildDirPath,
                    List.of("tmpl", "manifest", "k8s", "kustomize", profileKey, "snips", middleware,
                            "secret-sealed.tmpl"),
                    List.of()
                ) : ""
            )
        );
    }

    // 渲染中间件base snip模版，并将渲染后的字符串加入上下文中，用于主文件渲染
    private void middlewareBaseSnipsTemplateContext(
            Map<String, String> ctx, String buildDirPath, String middleware, boolean enabled) {
        ctx.put(
                middleware + "_init",
                enabled ? TemplateUtils.generate(
                        ctx,
                        buildDirPath,
                        List.of("tmpl", "manifest", "k8s", "helm", "templates", "snips", middleware,
                                "init-container.tmpl"),
                        List.of()
                ) : ""
        );
        ctx.put(
                middleware + "_secret_values",
                enabled ? TemplateUtils.generate(
                        ctx,
                        buildDirPath,
                        List.of("tmpl", "manifest", "k8s", "helm", "templates", "snips", middleware,
                                "secret-values.tmpl"),
                        List.of()
                ) : ""
        );
        ctx.put(
                middleware + "_secret_volumeMount",
                enabled ? TemplateUtils.generate(
                        ctx,
                        buildDirPath,
                        List.of("tmpl", "manifest", "k8s", "helm", "templates", "snips", middleware,
                                "secret-volume-mount.tmpl"),
                        List.of()
                ) : ""
        );
        ctx.put(
                middleware + "_secret_volume",
                enabled ? TemplateUtils.generate(
                        ctx,
                        buildDirPath,
                        List.of("tmpl", "manifest", "k8s", "helm", "templates", "snips", middleware,
                                "secret-volume.tmpl"),
                        List.of()
                ) : ""
        );
        ctx.put(
                middleware + "_secret_inlineValues",
                enabled ? TemplateUtils.generate(
                        ctx,
                        buildDirPath,
                        List.of("tmpl", "manifest", "k8s", "kustomize", "base", "snips", middleware,
                                "secret-inline-values.tmpl"),
                        List.of()
                ) : ""
        );
    }
}
