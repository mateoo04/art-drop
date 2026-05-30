package hr.tvz.artdrop.artdropapp.job;

import hr.tvz.artdrop.artdropapp.dto.FeaturedStatusSnapshot;
import hr.tvz.artdrop.artdropapp.service.FeaturedChallengeService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

@DisallowConcurrentExecution
public class FeaturedChallengeRotationJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(FeaturedChallengeRotationJob.class);

    @Autowired
    private FeaturedChallengeService service;

    @Override
    public void execute(JobExecutionContext context) {
        try {
            service.processScheduleIfReady();
            FeaturedStatusSnapshot s = service.getStatusForLogging();
            log.info("Featured: '{}' (ends in {}), next swap: {}",
                    s.currentTitle(), formatDuration(s.timeUntilEnd()), s.nextSwapDescription());
        } catch (Exception e) {
            log.error("FeaturedChallengeRotationJob failed", e);
        }
    }

    private static String formatDuration(Duration d) {
        if (d == null || d.isZero() || d.isNegative()) return "ended/none";
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}
