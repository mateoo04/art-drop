package hr.tvz.artdrop.artdropapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stripe_webhook_event")
public class StripeWebhookEvent {
    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
