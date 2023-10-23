package io.micrc.core.gradle.plugin.project;

import groovy.util.Eval;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.testing.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class ProjectConfigure {
    private static ProjectConfigure instance;

    private final boolean configurable;

    private ProjectConfigure(boolean configurable) {
        this.configurable = configurable;
    }

    public static ProjectConfigure newInstance(boolean configurable) {
        if (instance == null) {
            instance = new ProjectConfigure(configurable);
        }
        return instance;
    }

    public void configure(Project project) {
        log.info("configure micrc project. ");
        Optional<String> buildType = Optional.ofNullable((String) project.getProperties().get("BUILD_ENV"));
        Optional<Object> contextMeta = Optional.ofNullable(SchemaSynchronizeConfigure.metaData.get("contextMeta"));
        configureIdentity(project, contextMeta.orElseThrow(), buildType.orElse("integration"));
        configurePlugin(project);
        configureDependencies(project, contextMeta.orElseThrow(), buildType.orElse("integration"));
        configurePropertiesFile(project, contextMeta.orElseThrow());
        configureJunitTest(project);
    }

    private void configureIdentity(
            Project project, Object contextMeta, String buildType) {
        log.info("configure micrc project info for service. ");
        Optional<String> contextName =
            Optional.ofNullable(configurable
                ? (String) Eval.x(contextMeta, "x.content.contextName")
                : project.getName().replace("-service", ""));
        Optional<String> group = Optional.ofNullable(configurable
            ? (String) Eval.x(contextMeta, "x.content.server.basePackages")
            : null);
        Optional<String> version = Optional.ofNullable(configurable
            ? (String) Eval.x(contextMeta, "x.content.server.version")
            : null);
        Optional<String> proxyRepoUrl =
            Optional.ofNullable(configurable
                ? buildType.equals("integration")
                    ? (String) Eval.x(contextMeta, "x.content.global.integration.proxyRepoUrl")
                    : (String) Eval.x(contextMeta, "x.content.global.production.proxyRepoUrl")
                : null);
        project.setGroup(group.orElseThrow(
            () -> new IllegalStateException(
                "basePackages not found. " +
                "It must be in intro.json on meta of context."
            )
        ));
        project.setVersion(version.orElseThrow(
            () -> new IllegalStateException(
                "version not found. " +
                "It must be in intro.json on meta of context."
            )
        ));

        log.info("configure micrc project info for logic. ");
        // 处理logic，生成pom.xml并放入logic工程
        String logicName = contextName.orElseThrow() + "-logic";
        Map<String, String> ctx = new HashMap<>(Map.of(
            "group", group.orElseThrow(),
            "version", version.orElseThrow(),
            "logicName", logicName,
            "proxyRepoUrl", proxyRepoUrl.orElseThrow(
                () -> new IllegalStateException(
                    "proxyRepoUrl not found. " +
                    "It must be in intro.json on meta of context."
                )
            )
        ));
        TemplateUtils.generate(
            ctx,
            project.getProjectDir().getParent(),
            List.of("tmpl", "project", "pom.xml"),
            List.of(logicName, "pom.xml")
        );
    }

    private void configurePlugin(Project project) {
        log.info("configure gradle plugins: java, spring-boot, spring-dependency-management, jib. ");
        PluginContainer plugins = project.getPlugins();
        plugins.apply("java");
        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        javaPluginExtension.setSourceCompatibility(JavaVersion.VERSION_11);
        javaPluginExtension.setTargetCompatibility(JavaVersion.VERSION_11);
        plugins.apply("org.springframework.boot");
        plugins.apply("io.spring.dependency-management");
        plugins.apply("com.google.cloud.tools.jib");
    }

    private void configureDependencies(
            Project project, Object contextMeta, String buildType) {
        log.info("configure maven repositories: mavenLocal, proxyRepoUrl, mavenCentral. ");
        Optional<String> proxyRepoUrl =
            Optional.ofNullable(configurable
                ? buildType.equals("integration")
                    ? (String) Eval.x(contextMeta, "x.content.global.integration.proxyRepoUrl")
                    : (String) Eval.x(contextMeta, "x.content.global.production.proxyRepoUrl")
                : null);
        RepositoryHandler repositories = project.getRepositories();
        repositories.mavenLocal();
        repositories.maven(
            repository -> repository.setUrl(
                proxyRepoUrl.orElseThrow(
                    () -> new IllegalStateException(
                        "proxyRepoUrl not found. " +
                        "It must be in intro.json on meta of context."
                    )
                )
            )
        );
        repositories.mavenCentral();
        project.afterEvaluate(proj -> {
            log.info("configure dependencies: "
                + "spring-boot-starter, jakarta.persistence-api, "
                + "lombok, spring-boot-starter-test, micrc-core. ");
            DependencyHandler dependencies = proj.getDependencies();
            dependencies.add("implementation", "org.springframework.boot:spring-boot-starter");
            // runtime core
            dependencies.add("implementation", "io.micrc.core:micrc-core:v0.0.20");
            dependencies.add("implementation", "io.micrc.core:micrc-annotations:v0.0.5");
            // persistence annotations
            dependencies.add("implementation", "jakarta.persistence:jakarta.persistence-api:2.2.3");
            // jackson annotation
            dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-annotations:2.13.5");
            // shiro
            dependencies.add("implementation", "org.apache.shiro:shiro-core:1.7.1");
            // spring data jpa
            dependencies.add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa");
            // kafka
            dependencies.add("implementation", "org.springframework.kafka:spring-kafka");
            // hibernate-types
            dependencies.add("implementation", "com.vladmihalcea:hibernate-types-55:2.18.0");
            // lombok
            dependencies.add("compileOnly", "org.projectlombok:lombok");
            dependencies.add("annotationProcessor", "org.projectlombok:lombok");
            // springboot test
            dependencies.add("testImplementation", "org.springframework.boot:spring-boot-starter-test");
        });
    }

    @SuppressWarnings("all")
    private void configurePropertiesFile(Project project, Object contextMeta) {
        log.info("generate properties file: micrc.properties. ");
        Optional<String> ownerDomain = Optional.ofNullable(configurable
            ? (String) Eval.x(contextMeta, "x.content.ownerDomain")
            : null);
        Optional<String> namespace = Optional.ofNullable(configurable
                ? (String) Eval.x(contextMeta, "x.content.namespace")
                : null);
        Optional<String> contextName = Optional.ofNullable(configurable
                ? (String) Eval.x(contextMeta, "x.content.contextName")
                : null);
        Optional<String> publicUri = Optional.ofNullable(configurable
            ? (String) Eval.x(contextMeta, "x.content.server.api.publicUri")
            : null);
        Task processResources = project.getTasks().findByName("processResources");
        if (processResources != null) {
            processResources.doLast(task -> {
                try {
                    Path propsFilePath = Path.of(
                        project.getBuildDir().getAbsolutePath(),
                        "resources",
                        "main",
                        "micrc.properties"
                    );
                    Files.deleteIfExists(propsFilePath);
                    Files.write(
                        propsFilePath,
                        List.of(
                            "spring.application.name=" + project.getProperties().get("name"),
                            "application.version=" + project.getProperties().get("version"),
                            "micrc.domain=" + ownerDomain.orElseThrow(
                                () -> new IllegalStateException(
                                    "ownerDomain not found. " +
                                    "It must be in intro.json on meta of context."
                                )
                            ),
                            "micrc.api.public.uris=" + publicUri.orElse(""),
                            "micrc.x-host=" + namespace.orElseThrow() + "." + ownerDomain.orElseThrow() + "." + contextName.orElseThrow()
                        ),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void configureJunitTest(Project project) {
        log.info("configure junit test. ");
        Task test = project.getTasks().findByName("test");
        if (test != null) {
            test.doFirst(task -> ((Test) task).useJUnitPlatform());
        }
    }
}
