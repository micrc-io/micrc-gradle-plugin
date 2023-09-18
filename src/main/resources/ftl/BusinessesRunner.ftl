package ${basePackage}.infrastructure.runner.${aggregationPackage};

import io.micrc.core.annotations.integration.runner.RunnerAdapter;
import io.micrc.core.annotations.integration.runner.RunnerExecution;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

<#if runnerOrder?? && runnerOrder != ''>@Order(${runnerOrder})
</#if>@RunnerAdapter(
    serviceName = "${logic}Service",
    executePath = "${dataPath}"
)
public interface ${logic}Runner extends ApplicationRunner {

    @Component("${logic}Runner")
    public static class ${logic}RunnerImpl implements ${logic}Runner {

        @Override
        @Transactional
        @RunnerExecution
        public void run(ApplicationArguments args) throws Exception {

        }
    }
}
