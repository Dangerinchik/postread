package com.postread.data;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionKey implements Serializable {
    private Long targetId;
    private Long subscriberId;
}
