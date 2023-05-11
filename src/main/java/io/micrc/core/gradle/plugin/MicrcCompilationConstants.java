package io.micrc.core.gradle.plugin;

import lombok.Data;

import java.io.File;

@Data
public class MicrcCompilationConstants {

    public static final String PROTOCOL_REST = File.separator + "protocol" + File.separator + "rest";

    public static final String MODEL = File.separator + "model";

    public static final String SRC_MAIN_RESOURCES = File.separator + "src" + File.separator + "main" + File.separator + "resources";

    public static final String SRC_MAIN_RESOURCES_APIDOC = SRC_MAIN_RESOURCES + File.separator + "apidoc";

    public static final String SRC_MAIN_RESOURCES_AGGREGATIONS = SRC_MAIN_RESOURCES + File.separator + "aggregations";

    public static final String MICRC_SCHEMA_AGGREGATIONS = File.separator + "micrc" + File.separator + "schema" + File.separator + "aggregations";
}
