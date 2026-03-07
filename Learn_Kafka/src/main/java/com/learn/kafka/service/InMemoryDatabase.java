package com.learn.kafka.service;

import com.learn.kafka.model.Subscription;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory database using HashMap to store validated subscriptions
 */
@Service
public class InMemoryDatabase {
    
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    
    public void save(Subscription subscription) {
        subscriptions.put(subscription.getId(), subscription);
    }
    
    public Subscription findById(String id) {
        return subscriptions.get(id);
    }
    
    public Map<String, Subscription> getAll() {
        return new ConcurrentHashMap<>(subscriptions);
    }
    
    public void clear() {
        subscriptions.clear();
    }
    
    public int size() {
        return subscriptions.size();
    }
}
