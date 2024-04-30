package io.micrc.core.gradle.plugin.manifests;

import groovy.util.Eval;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;
import org.apache.commons.io.FilenameUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class DeploymentConfigure {
    private static DeploymentConfigure instance;

    private final boolean configurable;

    private DeploymentConfigure(boolean configurable) {
        this.configurable = configurable;
    }

    public static DeploymentConfigure newInstance(boolean configurable) {
        if (instance == null) {
            instance = new DeploymentConfigure(configurable);
        }
        return instance;
    }

    public void configure(Project project) {
        log.info("configure micrc project for skaffold.xml and jenkinsfile. ");
        Optional<Object> contextMeta = Optional.ofNullable(SchemaSynchronizeConfigure.metaData.get("contextMeta"));
        configureSkaffold(project, contextMeta.orElseThrow());
        configureJenkins(project, contextMeta.orElseThrow());
    }

    private void configureSkaffold(Project project, Object contextMeta) {
        Optional<String> contextName =
            Optional.ofNullable(configurable
                ? (String) Eval.x(contextMeta, "x.content.contextName")
                : project.getName().replace("-service", ""));

        // 配置skaffold.xml文件，写入project根目录
        String name = contextName.orElseThrow();
        Map<String, String> ctx = new HashMap<>(Map.of(
            "serviceName", name + "-service",
            "logicName", name + "-logic",
            "contextName", name
        ));
        TemplateUtils.generate(
            ctx, project.getProjectDir().getAbsolutePath(),
            List.of("tmpl", "manifest", "skaffold.yaml"),
            List.of("skaffold.yaml")
        );
    }

    private void configureJenkins(Project project, Object contextMeta) {
        configureJenkins("integration", project, contextMeta);
        configureJenkins("production", project, contextMeta);
    }

    private void configureJenkins(String buildEnv, Project project, Object contextMeta) {
        Optional<String> contextName =
            Optional.ofNullable(configurable
                ? (String) Eval.x(contextMeta, "x.content.contextName")
                : project.getName().replace("-service", ""));
        Optional<String> domainName =
            Optional.ofNullable(configurable
                ? (String) Eval.x(contextMeta, "x.content.ownerDomain")
                : FilenameUtils.getBaseName(project.getProjectDir().getParent()));
        Optional<String> proxyServerUrl = Optional.ofNullable(configurable
            ? (String) Eval.x(contextMeta, "x.content.global." + buildEnv + ".proxyServerUrl")
            : null);
        Optional<String> registry = Optional.ofNullable(configurable
            ? (String) Eval.x(contextMeta, "x.content.global." + buildEnv + ".registry")
            : null);
        Optional<String> gitopsRepo = Optional.ofNullable(configurable
            ? (String) Eval.x(contextMeta, "x.content.global." + buildEnv + ".gitopsRepo")
            : null);
        String name = contextName.orElseThrow();
        Map<String, String> ctx = new HashMap<>(Map.of(
            "domainName", domainName.orElseThrow(),
            "contextName", name,
            "serviceName", name + "-service",
            "logicName", name + "-logic",
            "registry", registry.orElseThrow(
                    () -> new IllegalStateException(
                        "registry not found. " +
                        "It must be in intro.json on meta of context."
                    )
                ),
            "gitopsRepo", gitopsRepo.orElseThrow(
                    () -> new IllegalStateException(
                        "gitopsRepo not found. " +
                        "It must be in intro.json on meta of context."
                    )
                ),
            "proxyServerUrl", proxyServerUrl.orElseThrow(
                    () -> new IllegalStateException(
                        "proxyServerUrl not found. " +
                        "It must be in intro.json on meta of context."
                    )
                ),
            "version", project.getVersion().toString()
        ));
        proxyServerUrl.ifPresent(url -> {
            String[] split = url.split(":");
            if (split.length == 3) {
                ctx.put("proxyServer", split[split.length - 2].replaceAll("/", ""));
                ctx.put("proxyPort", split[split.length - 1]);
            } else {
                ctx.put("proxyServer", "");
                ctx.put("proxyPort", "");
            }
        });
        Optional.ofNullable(configurable
                ? (String) Eval.x(contextMeta, "x.content.global." + buildEnv + ".proxyRepoUrl")
                : null).ifPresent(repoUrl -> {
            String[] split = repoUrl.split("/");
            ctx.put("noProxyRepo", split[2]);
        });
        TemplateUtils.generate(
            ctx, project.getProjectDir().getAbsolutePath(),
            List.of("tmpl", "ci", buildEnv + ".jenkinsfile"),
            List.of(buildEnv + ".jenkinsfile")
        );
    }
}
