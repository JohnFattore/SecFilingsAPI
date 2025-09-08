package com.example.sec_api.util;

import java.time.LocalDate;
import java.util.*;

public class QuarterUtils {
        public static List<LocalDate> getLast3Quarters(LocalDate period) {
        List<LocalDate> last3Quarters = new ArrayList<>();
        int year = period.getYear();
        int month = period.getMonthValue();
        if (month == 3) {
            last3Quarters.add(LocalDate.of(year-1, 12, 31));
            last3Quarters.add(LocalDate.of(year-1, 9, 30));
            last3Quarters.add(LocalDate.of(year-1, 6, 30));
        }
        else if (month == 6) {
            last3Quarters.add(LocalDate.of(year, 3, 31));
            last3Quarters.add(LocalDate.of(year-1, 12, 31));
            last3Quarters.add(LocalDate.of(year-1, 9, 30));
        }
        else if (month == 9) {
            last3Quarters.add(LocalDate.of(year, 6, 30));
            last3Quarters.add(LocalDate.of(year, 3, 31));
            last3Quarters.add(LocalDate.of(year-1, 12, 31));
        }
        else if (month == 12) {
            last3Quarters.add(LocalDate.of(year, 9, 30));
            last3Quarters.add(LocalDate.of(year, 6, 30));
            last3Quarters.add(LocalDate.of(year, 3, 31));
        }
        else {
            throw new RuntimeException("quarter not found");
        }
        return last3Quarters;
    }

    public static LocalDate parseQuarter(String quarterStr) {
        String yearPart = quarterStr.substring(2, 6);
        int year = Integer.parseInt(yearPart);
        int q = Integer.parseInt(quarterStr.substring(7));

        int month;
        int day;

        switch (q) {
            case 1: month = 3; day = 31; break;
            case 2: month = 6; day = 30; break;
            case 3: month = 9; day = 30; break;
            case 4: month = 12; day = 31; break;
            default: throw new IllegalArgumentException("Invalid quarter: " + q);
        }

        return LocalDate.of(year, month, day);
    }

    public static LocalDate nextQuarter(LocalDate current) {
        int month = current.getMonthValue();
        if (month == 3) {
            return LocalDate.of(current.getYear(), 6, 30);
        }
        else if (month == 6) {
            return LocalDate.of(current.getYear(), 9, 30);
        }
        else if (month == 9) {
            return LocalDate.of(current.getYear(), 12, 31);
        }
        else if (month == 12) {
            return LocalDate.of(current.getYear() + 1, 3, 31);
        }
        else {
            throw new RuntimeException("quarter not found");
        }
    }
}
