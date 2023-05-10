package io.micrc.core.gradle.plugin;

import lombok.Data;

import java.io.File;

@Data
public class MicrcCompilationConstants {

    public static final String RESOURCE_DIR_PATH = "src" + File.separator + "main" + File.separator + "resources";

    /**
     * MICRC 组织名称
     */
    public static final String MICRC_GROUP_NAME = "micrc";

    /**
     * schema分支名称
     */
    public static final String SCHEMA_BRANCH = "schema";

    /**
     * schema目录名称
     */
    public static final String SCHEMA_DIR = "schema";

    /**
     * schema目录在Build下的文件夹名称
     */
    public static final String BUILD_SCHEMA_PATH = MICRC_GROUP_NAME + "-" + SCHEMA_DIR;

    /**
     * schema下domain包名称
     */
    public static final String DOMAIN_DIR_NAME = "domain";

    /**
     * schema下domain描述文件名称
     */
    public static final String DOMAIN_INFO_NAME = "info.json";

    /**
     * 上下文下-上下文入口文件名称
     */
    public static final String SERVICE_INTRO_NAME = "intro.json";

    /**
     * 上下文下-数据库模式文件夹名称
     */
    public static final String DBS_DIR_NAME = "dbs";

    /**
     * 上下文下-逻辑文件夹名称
     */
    public static final String LOGICS_DIR_NAME = "logics";

    /**
     * 上下文下-映射文件夹名称
     */
    public static final String MAPPINGS_DIR_NAME = "mappings";

    /**
     * 上下文下-模型文件夹名称
     */
    public static final String MODELS_DIR_NAME = "models";

    /**
     * 上下文下-协议文件夹名称
     */
    public static final String PROTOCOLS_DIR_NAME = "protocols";

    /**
     * 上下文下-用例文件夹名称
     */
    public static final String USERCASES_DIR_NAME = "usercases";

    /**
     * 上下文下-用例文件内文件夹起始标识
     */
    public static final String USERCASE_DIR_START_NAME = "UC-";

    /**
     * 用例下入口文件名称
     */
    public static final String USERCASE_INTRO_NAME = "meta.json";
}
