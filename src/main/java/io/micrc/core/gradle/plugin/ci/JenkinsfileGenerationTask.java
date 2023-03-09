package io.micrc.core.gradle.plugin.ci;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrc.core.gradle.plugin.MicrcCompilationExtension;
import io.micrc.core.gradle.plugin.TemplateUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JenkinsfileGenerationTask extends DefaultTask {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DOMAIN_NAME_POINTER = "/domainName";

    private static final String DOMAIN_REGISTRY_POINTER = "/registry";

    private static final String REGISTRY_CREDENTIAL = "/registryCredential";

    private static final String GITOPS_REPOSITORY = "/gitopsRepository";

    private static final String GIT_CREDENTIAL = "/gitCredential";

    private static final String PROXY = "/proxy";

    @Input
    private String schemaPath;

    @TaskAction
    public void manifestsGeneration() {
        Project project = this.getProject();
        System.out.println("execute JenkinsfileGenerationTask");
        Map<String, String> properties = new HashMap<>();
        // system env placeholder
        properties.put("GIT_CREDENTIAL", "${env.git_credential}");
        properties.put("GIT_USERNAME", "${GIT_USERNAME}");
        properties.put("GIT_PASSWORD", "${GIT_PASSWORD}");
        properties.put("DOCKER_REGISTRY", "${env.docker_registry}");
        properties.put("REGISTRY_CREDENTIAL", "${env.registry_credential}");
        properties.put("REGISTRY_USERNAME", "${REGISTRY_USERNAME}");
        properties.put("REGISTRY_PASSWORD", "${REGISTRY_PASSWORD}");
        properties.put("GITOPS_REPO", "${env.gitops_repository}");
        properties.put("SERVICE_TAG", "${params.tag}");
        properties.put("COMMIT_NUMBER", "${params.commit_number}");
        properties.put("DOMAIN_REPO", "${params.domain_repository}");

        Path schemaFile = Paths.get(schemaPath, MicrcCompilationExtension.DOMAIN_DIR_NAME + File.separator + MicrcCompilationExtension.DOMAIN_INFO_NAME);
        String domainInfo = null;
        if (schemaFile.toFile().exists()) {
            try {
                domainInfo = Files.readString(schemaFile);
            } catch (IOException e) {
                // leave it out
            }
        }
        if (domainInfo == null || domainInfo.isBlank()) {
            throw new IllegalStateException("could not obtain domain info from domain-info.json. fix domain schema and retry configure project.");
        }
        String domainName = null;
        String registry = null;
        String registryCredential = null;
        String gitopsRepository = null;
        String gitCredential = null;
        JsonNode proxy = null;
        String httpsProxy = null;
        String noProxy = null;
        try {
            domainName = MAPPER.readTree(domainInfo).at(DOMAIN_NAME_POINTER).asText();
            registry = MAPPER.readTree(domainInfo).at(DOMAIN_REGISTRY_POINTER).asText();
            registryCredential = MAPPER.readTree(domainInfo).at(REGISTRY_CREDENTIAL).asText();
            gitopsRepository = MAPPER.readTree(domainInfo).at(GITOPS_REPOSITORY).asText();
            gitCredential = MAPPER.readTree(domainInfo).at(GIT_CREDENTIAL).asText();
            proxy = MAPPER.readTree(domainInfo).at(PROXY);
            httpsProxy = proxy.at("/httpsProxy").asText();
            noProxy = proxy.at("/noProxy").asText();
        } catch (IOException e) {
            throw new RuntimeException("could not resolve domain info, please check domain-info.json");
        }

        // schema params
        properties.put("domainName", domainName);
        properties.put("projectName", project.getName());
        properties.put("git_credential", gitCredential);
        properties.put("docker_registry", registry);
        properties.put("registry_credential", registryCredential);
        properties.put("gitops_repository", gitopsRepository);
        // proxy
        properties.put("https_proxy", httpsProxy);
        properties.put("no_proxy", noProxy);
        List<String> jenkinsfilePaths = List.of("ci", "jenkinsfile");
        List<String> jenkinsfileTargetPaths = List.of("jenkinsfile");
        TemplateUtils.generate(properties, project.getProjectDir().getAbsolutePath(), jenkinsfilePaths, jenkinsfileTargetPaths);
    }

    @Inject
    public JenkinsfileGenerationTask(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public JenkinsfileGenerationTask() {
    }

    public String getSchemaPath() {
        return schemaPath;
    }
}