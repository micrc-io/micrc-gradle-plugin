package io.micrc.core.gradle.plugin.schemas;

import lombok.Getter;
import lombok.extern.java.Log;
import org.gradle.api.Project;
import org.gradle.process.ExecResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Log
@Getter
public class SchemaSynchronizeConfigure {
    private static SchemaSynchronizeConfigure INSTANCE;

    private final String schemaBranch = "schema";
    private final String schemaDir = "schema";
    private final String domainSchemaDir = "domain";
    private final String schemaPathToBuild = "micrc-schema";
    private boolean configurable = false;
    private String schemaPath;

    private String schemaLocation;

    private SchemaSynchronizeConfigure() {
    }

    public static SchemaSynchronizeConfigure instance() {
        if (INSTANCE == null) {
            INSTANCE = new SchemaSynchronizeConfigure();
        }
        return INSTANCE;
    }

    public void configure(Project project) {
        schemaPath = project.getBuildDir().getAbsolutePath() + File.separator + schemaPathToBuild;
        File schemaPackage = new File(schemaPath);
        if (schemaPackage.exists()) {
            schemaPackage.delete();
        }
        if (!schemaPackage.exists()) {
            schemaPackage.mkdirs();
        }
        schemaLocation = schemaPath + File.separator + schemaDir;
        String url = obtainRepoUrl(project);
        if (url == null) {
            return;
        }
        boolean cloned = cloneRepo(project, url);
        if (!cloned) {
            return;
        }
        boolean checkouted = checkoutSchema(project);
        if (!checkouted) {
            return;
        }
        configurable = true;
    }

    private String obtainRepoUrl(Project project) {
        final List<String> gitUrl = List.of("git", "remote", "-v");
        final OutputStream out = new ByteArrayOutputStream();
        project.exec(execSpec -> {
            execSpec.commandLine(gitUrl);
            execSpec.setStandardOutput(out);
        });
        Optional<String> optional = Arrays.stream(out.toString().split("\n")).filter(it -> it.contains("fetch")).findFirst();
        if (optional.isEmpty()) {
            log.warning("unable to obtain repo url.");
            return null;
        }
        String url = optional.get().split("\\s+")[1];
        if (url == null) {
            log.warning("unable to obtain repo url.");
            return null;
        }
        return url;
    }

    private boolean cloneRepo(Project project, String url) {
        try {
            Files.walkFileTree(Paths.get(schemaPath), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return false;
        }
        final List<String> gitClone = List.of("git", "clone",
                "--depth", "1",
                "--filter=blob:none",
                "--sparse",
                "-b", schemaBranch,
                url,
                schemaPath);
        ExecResult cloneResult = project.exec(execSpec -> {
            execSpec.commandLine(gitClone);
        });
        if (cloneResult.getExitValue() != 0) {
            log.warning("unable to clone repo.");
            return false;
        }
        return true;
    }
    private boolean checkoutSchema(Project project) {
        final List<String> gitCheckout = List.of("git", "sparse-checkout", "set", schemaDir + File.separator + project.getName(), schemaDir + File.separator + domainSchemaDir);
        ExecResult checkoutResult = project.exec(execSpec -> {
            execSpec.setWorkingDir(schemaPath);
            execSpec.commandLine(gitCheckout);
        });
        if (checkoutResult.getExitValue() != 0) {
            log.warning("unable to checkout domain schema.");
            return false;
        }
        return true;
    }
}
