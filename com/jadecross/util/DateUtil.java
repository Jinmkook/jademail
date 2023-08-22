package com.jadecross.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
    public static String getToday(String mydate) {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        return (new SimpleDateFormat(mydate)).format(date);
    }

    public static String getFromYesterday(String mydate) {
        SimpleDateFormat df = new SimpleDateFormat(mydate);
        Calendar c = Calendar.getInstance();
        if (c.get(7) == 2) {
            c.add(5, -3);
        } else {
            c.add(5, -1);
        }
        String addeddate = df.format(c.getTime());
        return addeddate;
    }

    public static String getFromDate(String mydate) {
        SimpleDateFormat df = new SimpleDateFormat(mydate);
        Calendar c = Calendar.getInstance();
        c.add(5, -30);
        String addeddate = df.format(c.getTime());
        return addeddate;
    }
}
