package io.micrc.core.gradle.plugin.lib;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class FreemarkerUtil {
    private FreemarkerUtil() {}

    private static Configuration conf = new Configuration(Configuration.VERSION_2_3_31);

    private static Template temp;

    static {
        //            conf.setDirectoryForTemplateLoading(new File("src/main/resources/ftl/"));
        conf.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_31));
    }

    public static void generator(String ftlName, Map<String, Object> map, String fileName) {
        try {
//            temp = conf.getTemplate(ftlName + ".ftl");
            temp = loadTemplate("ftl/" + ftlName + ".ftl");
            File file = new File(fileName);
            if (!file.exists()) {
                File dir = new File(file.getParent());
                boolean mkdirs = dir.mkdirs();
                boolean newFile = file.createNewFile();
            }
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fw);
            temp.process(map, bw);
            bw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Template loadTemplate(String templatePath) {
        try {
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(templatePath);
            if (inputStream == null) {
                inputStream = TemplateUtils.class.getClassLoader().getResourceAsStream(templatePath);
                if (inputStream == null) {
                    throw new IllegalStateException("template file: " + templatePath + " is not exists. ");
                }
            }
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            return new Template(templatePath, reader, conf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
