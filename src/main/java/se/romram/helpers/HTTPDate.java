package se.romram.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by micke on 2015-01-06.
 */
public class HTTPDate {
    private static SimpleDateFormat HTTP_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    static final String sync = "syncObject";
    static {
        HTTP_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String formatDate(Date date) {
        if (date == null) return null;
        synchronized (sync) {
            return HTTP_DATE_FORMAT.format(date);
        }
    }

    public static String formatDate(long epoch) {
        try {
            return formatDate(new Date(epoch));
        } catch (Exception e) {
            System.out.println("EPOCH: " + epoch);
            e.printStackTrace();
            return "";
        }
    }

    public static Date parseDate(String dateAsGMTString) throws ParseException {
        synchronized (sync) {
            return HTTP_DATE_FORMAT.parse(dateAsGMTString);
        }
    }

}
