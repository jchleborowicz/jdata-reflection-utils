package pl.jdata.utils.reflection;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtils {

    private DateUtils() {
    }

    private static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static String dateToIsoDateString(Date date) {
        final SimpleDateFormat format = new SimpleDateFormat(ISO_DATE_TIME_FORMAT);
        return format.format(date);
    }

}
