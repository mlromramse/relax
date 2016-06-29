package se.romram.cookie;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by micke on 2015-01-04.
 */
public class RelaxCookieManager {
    private static String SET_COOKIE = "Set-Cookie";
    private Map<String, RelaxCookie> relaxCookieMap;

    public Map<String, RelaxCookie> getCookieMap() {
        if (relaxCookieMap == null) {
            relaxCookieMap = new ConcurrentHashMap<>();
        }
        return relaxCookieMap;
    }


    public void updateCookiesFromHeaderFields(Map<String, List<String>> responseHeaderFields) {
        for (String key : responseHeaderFields.keySet()) {
            if (SET_COOKIE.equalsIgnoreCase(key)) {
                for (String cookieString : responseHeaderFields.get(key)) {
                    RelaxCookie cookie = new RelaxCookie(cookieString);
                    getCookieMap().put(cookie.getKey(), cookie);
                }
            }
        }
    }

    public StringBuffer getCookieRequestHeaderBuffer(URL url) {
        StringBuffer cookieBuffer = new StringBuffer();
        for (RelaxCookie cookie : getCookieMap().values()) {
            if (cookie.isValidForUrl(url)) {
                if (cookieBuffer.length() != 0) {
                    cookieBuffer.append(";");
                }
                cookieBuffer.append(cookie.toExternalForm());
            }
        }
        return cookieBuffer;
    }
}
