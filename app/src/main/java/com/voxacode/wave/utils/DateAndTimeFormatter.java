package com.voxacode.wave.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateAndTimeFormatter {
    
    private static final DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("MMMM, d, h:mm a");
    private static final ZoneId zoneId = ZoneId.systemDefault();
    
    public static String format( long timeInMillis ) {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli( timeInMillis ) , zoneId
        ).format( defaultFormatter );
    }
}
