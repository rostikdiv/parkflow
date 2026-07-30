package com.parkflow.inventory.application;

import com.parkflow.shared.domain.events.ReservationChangedEvent;
import com.parkflow.shared.domain.events.SpotStatusEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListener {

    /**
     * Clear all entries in the availability cache when a spot changes status.
     * In a production environment, we would likely parse the keys and only evict
     * the specific lot, but for this milestone we evict all.
     */
    @EventListener
    @CacheEvict(value = "availability", allEntries = true)
    public void onSpotStatusEvent(SpotStatusEvent event) {
        // Cache evicted automatically by annotation
    }

    /**
     * Clear all entries in the availability cache when a reservation is created or cancelled.
     */
    @EventListener
    @CacheEvict(value = "availability", allEntries = true)
    public void onReservationChangedEvent(ReservationChangedEvent event) {
        // Cache evicted automatically by annotation
    }
}
