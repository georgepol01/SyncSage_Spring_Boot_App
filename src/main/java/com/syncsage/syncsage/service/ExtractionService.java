package com.syncsage.syncsage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExtractionService {

    @Value("${airbnb.listing.names}")
    private String listingNames;
    private static final Logger logger = LoggerFactory.getLogger(ExtractionService.class);
    private static final String LISTING_NAME_PATTERN = "(Traditional Village House( \\d*)?)";
    private static final Pattern CHECKIN_PATTERN = Pattern.compile("Check-in\\s+(\\w+),\\s+(\\w+)\\s+(\\d+)\\s+(\\d+:\\d+\\s+\\w+)");
    private static final Pattern CHECKOUT_PATTERN = Pattern.compile("Checkout\\s+(\\w+),\\s+(\\w+)\\s+(\\d+)\\s+(\\d+:\\d+\\s+\\w+)");

    public String extractListingName(String emailContent) {
        // Define the pattern to match the listing names
        Pattern regex = Pattern.compile(LISTING_NAME_PATTERN);
        Matcher matcher = regex.matcher(emailContent);

        // Extract and validate the listing name in one step
        while (matcher.find()) {
            String extractedName = matcher.group(1).trim();
            // Check if the extracted name is in the list of accepted names
            if (listingNames.contains(extractedName)) {
                return extractedName;
            }
        }
        // Return null or an appropriate value if no match is found
        return null;
    }

    public String[] extractBookingDates(String emailContent) {

        try {
            // Define patterns to capture the full date and time
            Matcher checkInMatcher = CHECKIN_PATTERN.matcher(emailContent);
            Matcher checkOutMatcher = CHECKOUT_PATTERN.matcher(emailContent);

            if (checkInMatcher.find() && checkOutMatcher.find()) {
                String checkInDay = checkInMatcher.group(1);
                String checkInMonth = checkInMatcher.group(2);
                String checkInDate = checkInMatcher.group(3);
                String checkInTime = checkInMatcher.group(4);

                String checkOutDay = checkOutMatcher.group(1);
                String checkOutMonth = checkOutMatcher.group(2);
                String checkOutDate = checkOutMatcher.group(3);
                String checkOutTime = checkOutMatcher.group(4);

                // Assume current year
                int currentYear = LocalDate.now().getYear();

                // Construct date strings with the year
                String checkInDateStr = String.format("%s, %s %s %d %s", checkInDay, checkInMonth, checkInDate, currentYear, checkInTime);
                String checkOutDateStr = String.format("%s, %s %s %d %s", checkOutDay, checkOutMonth, checkOutDate, currentYear, checkOutTime);

                // Define input and output date formats
                SimpleDateFormat inputFormat = new SimpleDateFormat("E, MMM d yyyy h:mm a", Locale.ENGLISH);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");

                // Parse and format the dates
                Date checkInDateParsed = inputFormat.parse(checkInDateStr);
                Date checkOutDateParsed = inputFormat.parse(checkOutDateStr);

                String checkInFormatted = outputFormat.format(checkInDateParsed);
                String checkOutFormatted = outputFormat.format(checkOutDateParsed);

                return new String[]{checkInFormatted, checkOutFormatted};
            }
        } catch (DateTimeParseException | ParseException e) {
            logger.error("Date parsing error: {}", e.getMessage());
        }
        return null;
    }

}
