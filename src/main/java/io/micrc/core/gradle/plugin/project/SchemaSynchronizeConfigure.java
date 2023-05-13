package io.micrc.core.gradle.plugin.project;

import com.google.common.base.CharMatcher;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;
import org.gradle.api.tasks.WorkResult;
import org.gradle.process.ExecResult;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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
//        return false;
    }

    private boolean mergeScheme(Project project, String repo, String contextName) {
        final List<String> gitFetch = List.of("git", "fetch");
        ExecResult fetchResult = project.exec(execSpec -> {
            execSpec.commandLine(gitFetch);
            execSpec.setStandardOutput(System.out);
            execSpec.setErrorOutput(System.err);
//            execSpec.setEnvironment();
        });
        if (fetchResult.getExitValue() != 0) {
            log.error("Could not fetch repo from remote. ");
            return false;
        }
        final List<String> gitRestore =
            List.of("git", "restore", "--source", repo + "/" + SCHEMA_BRANCH, "../schema/" + contextName);
        ExecResult restoreResult = project.exec(execSpec -> {
            execSpec.commandLine(gitRestore);
            execSpec.setStandardOutput(System.out);
            execSpec.setErrorOutput(System.err);
//            execSpec.setEnvironment();
        });
        if (restoreResult.getExitValue() != 0) {
            log.error("Could not restore files from: " + repo + "/" + SCHEMA_BRANCH);
            return false;
        }
        final List<String> gitTrack = List.of("git", "add", "../schema/" + contextName);
        ExecResult result = project.exec(execSpec -> {
            execSpec.commandLine(gitTrack);
            execSpec.setStandardOutput(System.out);
            execSpec.setErrorOutput(System.err);
//            execSpec.setEnvironment();
        });
        if (result.getExitValue() != 0) {
            log.error("Could not track files that restored from: " + repo + "/" + SCHEMA_BRANCH);
            return false;
        }
        return true;
    }

    private void readMeta() {
        Path metaFilePath = Paths.get(schemaLocation, CONTEXT_META_FILE);
        if (!Files.exists(metaFilePath)) {
            throw new IllegalStateException("could not obtain context info from intro.json. fix schema and retry to configure project.");
        }
        metaData.put("contextMeta", new JsonBuilder(new JsonSlurper().parse(metaFilePath.toFile())));
        configurable = true;
    }
}
