package com.learn.kafka.service;

import com.learn.kafka.model.Subscription;
import org.springframework.stereotype.Service;

/**
 * Service to validate subscription records
 */
@Service
public class SubscriptionValidator {
    
    public boolean validate(Subscription subscription) {
        if (subscription == null) {
            return false;
        }
        
        // Validate required fields
        if (subscription.getId() == null || subscription.getId().trim().isEmpty()) {
            return false;
        }
        
        if (subscription.getUserId() == null || subscription.getUserId().trim().isEmpty()) {
            return false;
        }
        
        if (subscription.getPlanType() == null || subscription.getPlanType().trim().isEmpty()) {
            return false;
        }
        
        // Validate amount is positive
        if (subscription.getAmount() == null || subscription.getAmount() <= 0) {
            return false;
        }
        
        // Validate active status is not null
        if (subscription.getActive() == null) {
            return false;
        }
        
        return true;
    }
}
