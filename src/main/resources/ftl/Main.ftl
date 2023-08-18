package ${basePackage};

import io.micrc.core.EnableMicrcSupport;
import io.micrc.core.MicrcApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMicrcSupport
public class ${contextName}ServiceApplication {
    public static void main(String[] args) {
        MicrcApplication.run(${contextName}ServiceApplication.class, args);
    }
}
