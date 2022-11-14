package io.micrc.core.gradle.plugin.manifests;

import lombok.extern.java.Log;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

@Log
public class SkaffoldConfigure {
    private static SkaffoldConfigure INSTANCE;

    private SkaffoldConfigure() {}

    public static SkaffoldConfigure newInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SkaffoldConfigure();
        }
        return INSTANCE;
    }

    public void configure(Project project) {
        log.info("configure micrc project deployment manifests. ");
        // TODO 读取schema中上下文信息，domain-info.json。获取子域名称(镜像repo使用：[registry]/[domain]/[context]:[tag])
        // 配置skaffold.xml文件，写入project根目录
    }
    
    private String appVersionToChartVersion(String appVersion) {
        String[] versions = appVersion.split("\\.");
        return String.format("%d.%d.%d", Integer.valueOf(versions[0]), Integer.valueOf(versions[1] + versions[2]), 0);
    }
}
