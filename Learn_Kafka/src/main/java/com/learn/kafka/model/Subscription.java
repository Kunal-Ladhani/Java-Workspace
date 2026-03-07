package com.learn.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    private String id;
    private String userId;
    private String planType;
    private Double amount;
    private Boolean active;
}
