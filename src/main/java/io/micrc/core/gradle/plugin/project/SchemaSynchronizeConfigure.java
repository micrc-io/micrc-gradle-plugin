package io.micrc.core.gradle.plugin.project;

import com.google.common.base.CharMatcher;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import groovy.util.Eval;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;
import org.gradle.api.tasks.WorkResult;
import org.gradle.process.ExecResult;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Getter
public class SchemaSynchronizeConfigure {
    public static final Map<String, Object> metaData = new HashMap<>();

    private static SchemaSynchronizeConfigure instance;

    private static final String MICRC_BUILD_DIR = "micrc";
    private static final List<String> STABLE_BRANCHES =
            List.of("main", "master", "dev", "develop", "main", "hotfix", "schema");
    private static final String SCHEMA_DIR = "schema";
    private static final String SCHEMA_BRANCH = "schema";

    private static final String CONTEXT_META_FILE = "intro.json";

    private static final String BACKEND_META_FILE = "backendMetadata.json";
    private boolean configurable = false;
    private String micrcBuildPath;
    private String schemaLocation;

    private SchemaSynchronizeConfigure() {
    }

    public static SchemaSynchronizeConfigure newInstance() {
        if (instance == null) {
            instance = new SchemaSynchronizeConfigure();
        }
        return instance;
    }

    public void configure(Project project) {
        micrcBuildPath = project.getBuildDir().getAbsolutePath() + File.separator + MICRC_BUILD_DIR;
        schemaLocation = micrcBuildPath + File.separator + SCHEMA_DIR;
        File micrcBuildDir = new File(micrcBuildPath);
        if (!micrcBuildDir.exists()) {
            micrcBuildDir.mkdirs();
        }
        File schemaPackage = new File(schemaLocation);
        if (!schemaPackage.exists()) {
            schemaPackage.mkdirs();
        }

        String repo = obtainRepoName(project);
        String branch = obtainRepoBranch(project);
        if (repo == null || branch == null) {
            return;
        }
        boolean result = obtainSchema(project, branch, repo);
        if (!result) {
            return;
        }
        readMeta();
        readBackendMetadata();
    }

    private String obtainRepoName(Project project) {
        final List<String> gitRepo = List.of("git", "remote", "show");
        final OutputStream out = new ByteArrayOutputStream();
        ExecResult result = project.exec(execSpec -> {
            execSpec.commandLine(gitRepo);
            execSpec.setStandardOutput(out);
        });
        String repo = CharMatcher.breakingWhitespace().removeFrom(out.toString());
        if (result.getExitValue() != 0 || "".equals(repo)) {
            return null;
        }
        return repo;
    }

    private String obtainRepoBranch(Project project) {
        final List<String> gitBranch = List.of("git", "branch", "--show-current");
        final OutputStream out = new ByteArrayOutputStream();
        ExecResult result = project.exec(execSpec -> {
            execSpec.commandLine(gitBranch);
            execSpec.setStandardOutput(out);
        });
        String branch = CharMatcher.breakingWhitespace().removeFrom(out.toString());
        if (result.getExitValue() != 0) {
            return null;
        }
        return branch;
    }

    private boolean obtainSchema(Project project, String branch, String repo) {
        boolean merged = true;
        String contextName = project.getName().replace("-service", "");
        if (!STABLE_BRANCHES.contains(branch) && !"".equals(branch)) {
            merged = mergeScheme(project, repo, contextName);
        }
        if (merged) {
            return copySchema(project, contextName);
        }
        return false;
    }

    private boolean copySchema(final Project project, String contextName) {
        TemplateUtils.clearDir(Paths.get(schemaLocation));
        final String schemaSourcePath =
                project.getProjectDir().getParent() + File.separator + SCHEMA_DIR + File.separator + contextName;
        WorkResult result = project.copy(copySpec -> copySpec.from(schemaSourcePath).into(schemaLocation));
        return result.getDidWork();
    }

    private boolean mergeScheme(Project project, String repo, String contextName) {
        try {
            String caseName = findCaseNameByBranch();
            if (caseName == null) {
                log.error("Could not find case name from remote branch.");
                return false;
            }
            Stream<List<String>> stream = Stream.of(
                    List.of("git", "fetch"),
                    List.of("git", "restore", "--source", repo + "/" + SCHEMA_BRANCH, "../schema/" + contextName + "/cases/" + caseName));
            boolean fetchAndRestoreCaseResult = executeCommandAndGetResult(stream, project, repo, contextName, caseName);
            if (!fetchAndRestoreCaseResult) {
                return false;
            }
            String aggregationName = findAggregationNameInCaseIntro(project, contextName, caseName);
            if (aggregationName == null) {
                log.error("Could not find aggregation name from intro.");
                return false;
            }
            stream = Stream.of(
                    List.of("git", "restore", "--source", repo + "/" + SCHEMA_BRANCH, "../schema/" + contextName + "/aggregations/" + aggregationName),
                    List.of("git", "restore", "--source", repo + "/" + SCHEMA_BRANCH, "../schema/" + contextName + "/intro.json"),
                    List.of("git", "add", "../schema/" + contextName));
            return executeCommandAndGetResult(stream, project, repo, contextName, aggregationName);
        } catch (Exception e) {
            log.error("Merge schema exception: {}", e.getLocalizedMessage());
            return false;
        }
    }

    private String findAggregationNameInCaseIntro(Project project, String contextName, String caseName) {
        String caseDir = project.getProjectDir().getParent() + "/schema/" + contextName + "/cases/" + caseName;
        readCaseMeta(caseDir);
        return (String) Eval.x(metaData.get("caseMeta"), "x.content.aggregationName");
    }

    private static Boolean executeCommandAndGetResult(Stream<List<String>> stream, Project project, String repo, String contextName, String caseName) {
        return stream.map(command -> {
                    ExecResult execResult = project.exec(execSpec -> {
                        execSpec.commandLine(command);
                        execSpec.setStandardOutput(System.out);
                        execSpec.setErrorOutput(System.err);
                    });
                    if (execResult.getExitValue() != 0) {
                        log.error("Could not execute command: {}", String.join(" ", command));
                        return false;
                    }
                    return true;
            }).filter(result -> !result).findFirst().orElse(true);
    }

    private static String findCaseNameByBranch() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "branch", "-vv");
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String caseName = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("* ")) {
                String[] lineArray = line.split(" ");
                caseName = lineArray[3].split("\\[origin/")[1].split("]")[0];
                break;
            }
        }
        reader.close();
        return caseName;
    }

    private void readMeta() {
        Path metaFilePath = Paths.get(schemaLocation, CONTEXT_META_FILE);
        if (!Files.exists(metaFilePath)) {
            throw new IllegalStateException("could not obtain context info from intro.json. fix schema and retry to configure project.");
        }
        metaData.put("contextMeta", new JsonBuilder(new JsonSlurper().parse(metaFilePath.toFile())));
        configurable = true;
    }

    private void readCaseMeta(String caseSchemaPath) {
        Path metaFilePath = Paths.get(caseSchemaPath, CONTEXT_META_FILE);
        if (!Files.exists(metaFilePath)) {
            throw new IllegalStateException("could not obtain case info from intro.json. fix schema and retry to configure project.");
        }
        metaData.put("caseMeta", new JsonBuilder(new JsonSlurper().parse(metaFilePath.toFile())));
    }

    private void readBackendMetadata() {
        Path backendMetadataFilePath = Paths.get(schemaLocation, BACKEND_META_FILE);
        if (!Files.exists(backendMetadataFilePath)) {
            return;
        }
        metaData.put("backendMeta", TemplateUtils.readFile(backendMetadataFilePath));
    }
}
