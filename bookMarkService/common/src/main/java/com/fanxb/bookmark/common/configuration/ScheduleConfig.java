package com.fanxb.bookmark.common.configuration;

import com.fanxb.bookmark.common.factory.CustomThreadFactory;
import com.fanxb.bookmark.common.factory.ThreadPoolFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created with IntelliJ IDEA
 *
 * @author fanxb
 */
@Configuration
@Slf4j
public class ScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        ScheduledExecutorService service = new ScheduledThreadPoolExecutor(5, new CustomThreadFactory("schedule"));
        scheduledTaskRegistrar.setScheduler(service);
        log.info("自定义schedule线程池成功");
    }

}
