package hr.tvz.artdrop.artdropapp.job;

import hr.tvz.artdrop.artdropapp.service.ReservationService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@DisallowConcurrentExecution
public class ReservationExpiryJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    @Autowired
    private ReservationService reservationService;

    @Override
    public void execute(JobExecutionContext context) {
        try {
            int n = reservationService.releaseExpired();
            if (n > 0) {
                log.info("Released {} expired artwork reservation(s)", n);
            }
        } catch (Exception e) {
            log.error("ReservationExpiryJob failed", e);
        }
    }
}
