package se.romram.cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.helpers.HTTPDate;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Simple cookie object created to reduce external dependencies.
 * Author: mikael.larsson@romram.se
 * Date: 2015-01-04
 * Time: 13:44
 */
public class RelaxCookie {

    private Logger log = LoggerFactory.getLogger(RelaxCookie.class);
    private static final int FIRST = 0;
    private static final String EXPIRES = "Expires";
    private static final String DOMAIN = "Domain";
    private static final String PATH = "Path";
    private static final String HTTP_ONLY = "HttpOnly";
    private static final String SECURE = "Secure";
    private static final String EXTERNAL_FORMAT = "%s=%s";

    private String name;

    private String value;

    private String comment;

    private String domain;

    private Date expiryDate;

    private String path;

    private boolean isSecure;

    private boolean isHttp;

    private boolean hasPathAttribute = false;

    private boolean hasDomainAttribute = false;

    private int version = 0;


    public RelaxCookie() {
        this(null, "noname", null, null, null, false, false);
    }

    public RelaxCookie(String domain, String name, String value) {
        this(domain, name, value, null, null, false, false);
    }

    public RelaxCookie(String domain, String name, String value,
                       String path, Date expires, boolean http, boolean secure) {

        log.trace("enter Cookie(String, String, String, String, Date, boolean)");
        if (name == null) {
            throw new IllegalArgumentException("Cookie name may not be null");
        }
        if (name.trim().equals("")) {
            throw new IllegalArgumentException("Cookie name may not be blank");
        }
        this.setName(name);
        this.setValue(value);
        this.setPath(path);
        this.setDomain(domain);
        this.setExpiryDate(expires);
        this.setHttp(http);
        this.setSecure(secure);
    }

    public RelaxCookie(String domain, String name, String value, String path,
                       int maxAge, boolean http, boolean secure) {

        this(domain, name, value, path, null, http, secure);
        if (maxAge < -1) {
            throw new IllegalArgumentException("Invalid max age:  " + Integer.toString(maxAge));
        }
        if (maxAge >= 0) {
            setExpiryDate(new Date(System.currentTimeMillis() + maxAge * 1000L));
        }
    }

    public RelaxCookie(String cookieString) {
        this();
        String[] cookieValues = cookieString.split(";");
        for (int i=0; i<cookieValues.length; i++) {
            String nameValuePair = cookieValues[i];
            String[] nameValue = nameValuePair.split("=");
            String name = nameValue[0].trim();
            String value = nameValue.length==2 ? nameValue[1].trim() : null;
            if (i == FIRST) {
                setName(name);
                setValue(value);
            } else {
                if (DOMAIN.equalsIgnoreCase(name)) {
                    setDomain(value);
                    continue;
                }
                if (PATH.equalsIgnoreCase(name)) {
                    setPath(value);
                    continue;
                }
                if (SECURE.equalsIgnoreCase(name)) {
                    setSecure(true);
                    continue;
                }
                if (HTTP_ONLY.equalsIgnoreCase(name)) {
                    setHttp(true);
                    continue;
                }
                if (EXPIRES.equalsIgnoreCase(name)) {
                    try {
                        setExpiryDate(new HTTPDate().parseDate(value));
                    } catch (ParseException e) {
                        log.error("Cookie expiration date ({}) is not parsable.", value);
                    }
                    continue;
                }
            }
        }
    }


    public String getKey() {
        return name + path + domain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isPersistent() {
        return (null != expiryDate);
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        if (domain != null) {
            int ndx = domain.indexOf(":");
            if (ndx != -1) {
                domain = domain.substring(0, ndx);
            }
            this.domain = domain.toLowerCase();
        }
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean getHttp() {
        return isHttp;
    }

    public void setHttp(boolean isHttp) {
        this.isHttp = isHttp;
    }

    public boolean getSecure() {
        return isSecure;
    }

    public void setSecure(boolean secure) {
        isSecure = secure;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isExpired() {
        return (expiryDate != null
                && expiryDate.getTime() <= System.currentTimeMillis());
    }

    public boolean isExpired(Date now) {
        return (expiryDate != null
                && expiryDate.getTime() <= now.getTime());
    }

    public boolean isValidForUrl(URL url) {
        String domain = url.getHost();
        String path = url.getPath();
        if (isExpired() || !matchDomain(domain) || !matchPath(path)) {
            return false;
        }
        return true;
    }

    private boolean matchPath(String path) {
        if (getPath() == null) return true;
        return path.startsWith(getPath());
    }

    private boolean matchDomain(String domain) {
        if (getDomain() == null) return true;
        return domain.endsWith(getDomain());
    }

    public boolean isPathAttributeSpecified() {
        return hasPathAttribute;
    }

    public void setPathAttributeSpecified(boolean value) {
        hasPathAttribute = value;
    }

    public boolean isDomainAttributeSpecified() {
        return hasDomainAttribute;
    }

    public void setDomainAttributeSpecified(boolean value) {
        hasDomainAttribute = value;
    }

    public int hashCode() {
        int hash = 3;
        hash = 7 * hash + this.getName().hashCode();
        hash = 7 * hash + this.domain.hashCode();
        hash = 7 * hash + this.path.hashCode();
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof RelaxCookie) {
            RelaxCookie that = (RelaxCookie) obj;
            return (this.name.equals(that.name)
                    && this.domain.equals(that.domain)
                    && this.path.equals(that.path));
        } else {
            return false;
        }
    }

    public String toExternalForm() {
        return String.format(EXTERNAL_FORMAT, name, value);
    }

    public int compare(Object o1, Object o2) {
        log.trace("enter Cookie.compare(Object, Object)");

        if (!(o1 instanceof RelaxCookie)) {
            throw new ClassCastException(o1.getClass().getName());
        }
        if (!(o2 instanceof RelaxCookie)) {
            throw new ClassCastException(o2.getClass().getName());
        }
        RelaxCookie c1 = (RelaxCookie) o1;
        RelaxCookie c2 = (RelaxCookie) o2;
        if (c1.getPath() == null && c2.getPath() == null) {
            return 0;
        } else if (c1.getPath() == null) {
            if (c2.getPath().equals("/")) {
                return 0;
            } else {
                return -1;
            }
        } else if (c2.getPath() == null) {
            if (c1.getPath().equals("/")) {
                return 0;
            } else {
                return 1;
            }
        } else {
            return c1.getPath().compareTo(c2.getPath());
        }
    }

    public String toString() {
        return toExternalForm();
    }


}
