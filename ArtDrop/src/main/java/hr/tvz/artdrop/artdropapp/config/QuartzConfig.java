package hr.tvz.artdrop.artdropapp.config;

import hr.tvz.artdrop.artdropapp.job.FeaturedChallengeRotationJob;
import hr.tvz.artdrop.artdropapp.job.ReservationExpiryJob;
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
    public JobDetail reservationExpiryJobDetail() {
        return JobBuilder.newJob(ReservationExpiryJob.class)
                .withIdentity("reservationexpiryjob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reservationExpiryTrigger(
            JobDetail reservationExpiryJobDetail,
            @Value("${commerce.reservation-cleanup-cron:0 */1 * * * ?}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(reservationExpiryJobDetail)
                .withIdentity("reservationexpirytrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            AutowiringSpringBeanJobFactory jobFactory,
            JobDetail featuredRotationJobDetail,
            Trigger featuredRotationTrigger,
            JobDetail reservationExpiryJobDetail,
            Trigger reservationExpiryTrigger) {
        SchedulerFactoryBean s = new SchedulerFactoryBean();
        s.setJobFactory(jobFactory);
        s.setJobDetails(featuredRotationJobDetail, reservationExpiryJobDetail);
        s.setTriggers(featuredRotationTrigger, reservationExpiryTrigger);
        return s;
    }
}
