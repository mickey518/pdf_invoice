package lab.dragon.invoice.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * @author mickey.wang
 */
public class DateUtil {

    private static ThreadLocal<Map<String, DateFormat>> threadLocal = new ThreadLocal<>();
    public static final String FILE_NAME_FORMAT_STRING = "yyyy/MMdd-HHmmss";

    /**
     * @param pattern
     * @return date format
     */
    public static DateFormat getDateFormat(String pattern) {
        Map<String, DateFormat> map = threadLocal.get();
        DateFormat format = null;
        if (null == map) {
            map = CollectionUtil.newHashMap();
            format = new SimpleDateFormat(pattern);
            map.put(pattern, format);
            threadLocal.set(map);
        } else {
            format = map.computeIfAbsent(pattern, k -> new SimpleDateFormat(k));
        }
        return format;
    }
}
