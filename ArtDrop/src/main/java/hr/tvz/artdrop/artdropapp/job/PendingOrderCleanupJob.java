package hr.tvz.artdrop.artdropapp.job;

import hr.tvz.artdrop.artdropapp.service.OrderService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@DisallowConcurrentExecution
public class PendingOrderCleanupJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(PendingOrderCleanupJob.class);

    @Autowired
    private OrderService orderService;

    @Value("${commerce.pending-order-grace-minutes}")
    private int graceMinutes;

    @Override
    public void execute(JobExecutionContext context) {
        try {
            int n = orderService.deleteAbandonedPendingOrders(graceMinutes);
            if (n > 0) {
                log.info("Deleted {} abandoned PENDING_PAYMENT order(s)", n);
            }
        } catch (Exception e) {
            log.error("PendingOrderCleanupJob failed", e);
        }
    }
}
