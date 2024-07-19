package com.syncsage.syncsage.controller;

import com.syncsage.syncsage.service.AirbnbSyncService;
import com.syncsage.syncsage.service.BookingComSyncService;
import com.syncsage.syncsage.service.EmailMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/sync")
public class BookingSyncController {

    @Autowired
    private BookingComSyncService bookingComSyncService;

    @Autowired
    private AirbnbSyncService airbnbSyncService;

    @GetMapping("/update")
    public String updatePricesAndAvailability() {
        // Example: Trigger syncing prices and availability
        String listingId = "12345";
        double price = 100.0;
        Date availabilityDate = new Date();

        bookingComSyncService.syncPricesAndAvailability(listingId, price, availabilityDate);
        airbnbSyncService.syncPricesAndAvailability(listingId, price, availabilityDate);

        return "Syncing prices and availability for listingId: " + listingId;
    }

}
