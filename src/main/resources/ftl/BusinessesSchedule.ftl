package ${basePackage}.infrastructure.schedule.${aggregationPackage};

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import ${basePackage}.application.businesses.${aggregationPackage}.${logic}Service;
import ${basePackage}.domain.${aggregationPackage}.command.${logic}Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ${logic}Schedule {

    @Autowired
    private ${logic}Service service;

    @Async
    @Scheduled(cron = "${cron}")
    @SchedulerLock(name = "${logic}")
    public void execute() {
        service.execute(new ${logic}Command());
    }
}
