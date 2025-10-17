package com.postread.data;

import com.postread.security.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscriptions_target", columnList = "target_id")
})
@AllArgsConstructor
@NoArgsConstructor
public class Subscription {
    @EmbeddedId
    private SubscriptionKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("targetId")
    @JoinColumn(name = "target_id")
    private User target;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subscriberId")
    @JoinColumn(name = "subscriber_id")
    private User subscriber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
