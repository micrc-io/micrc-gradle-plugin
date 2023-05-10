package io.micrc.core.gradle.plugin.schemas;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Schemas {

    private static final Map<String, Object> serviceMeta = new HashMap<>();

    /**
     * 用例基础元数据信息
     */
    public static final Map<String, String> usercaseMetas = new HashMap<>();

    /**
     * 用例内文件(地址读入内存,具体解析放置在各个具体生成逻辑内)
     */
    public static final Map<String, File[]> usercaseFiles = new HashMap<>();

    /**
     * OPENAPI协议关系信息(说明某一个聚合下存在那些协议,以聚合名称为Key,如后期聚合存在ID,则替换为ID,以协议ID为值)
     */
    public static final Map<String, List<String>> protocolRelations = new HashMap<>();

    /**
     * 该上下文下所有的协议,以协议名为Key 协议内容为值
     */
    public static final Map<String, String> protocols = new HashMap<>();
}