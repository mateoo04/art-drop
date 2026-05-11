package hr.tvz.artdrop.artdropapp.config;

import hr.tvz.artdrop.artdropapp.job.FeaturedChallengeRotationJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {

    @Bean
    public AutowiringSpringBeanJobFactory springBeanJobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory factory = new AutowiringSpringBeanJobFactory();
        factory.setApplicationContext(applicationContext);
        return factory;
    }

    @Bean
    public JobDetail featuredRotationJobDetail() {
        return JobBuilder.newJob(FeaturedChallengeRotationJob.class)
                .withIdentity("featuredchallengerotationjob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger featuredRotationTrigger(
            JobDetail featuredRotationJobDetail,
            @Value("${artdrop.featured-rotation-cron:0 * * * * ?}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(featuredRotationJobDetail)
                .withIdentity("featuredchallengerotationtrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            AutowiringSpringBeanJobFactory jobFactory,
            JobDetail featuredRotationJobDetail,
            Trigger featuredRotationTrigger) {
        SchedulerFactoryBean s = new SchedulerFactoryBean();
        s.setJobFactory(jobFactory);
        s.setJobDetails(featuredRotationJobDetail);
        s.setTriggers(featuredRotationTrigger);
        return s;
    }
}
