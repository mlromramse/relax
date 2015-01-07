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

    static public String formatDate(Date date) {
        if (date == null) return null;
        HTTP_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
        return HTTP_DATE_FORMAT.format(date);
    }

    static public String formatDate(long epoch) {
        return formatDate(new Date(epoch));
    }

    static public Date parseDate(String dateAsGMTString) throws ParseException {
        return HTTP_DATE_FORMAT.parse(dateAsGMTString);
    }

}
