package org.esupportail.openconnect.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/*
    Clone/fork of InMemoryCookieStore :
    we just add persistence to file local storage whand add and remove cookie
    and we initialize the cookie store with the cookies from the file local storage
 */
public class FileLocalStorageCookieStore implements CookieStore {

    private final static Logger log = LoggerFactory.getLogger(FileLocalStorageCookieStore.class);

    FileLocalStorage fileLocalStorage = FileLocalStorage.getInstance();

    private List<HttpCookie> cookieJar = null;
    private Map<String, List<HttpCookie>> domainIndex = null;
    private Map<URI, List<HttpCookie>> uriIndex = null;
    private ReentrantLock lock = null;

    public FileLocalStorageCookieStore() {
        readFromFileLocalStorage();
    }

    public void add(URI uri, HttpCookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie is null");
        } else {
            this.lock.lock();

            try {
                this.cookieJar.remove(cookie);
                if (cookie.getMaxAge() != 0L) {
                    this.cookieJar.add(cookie);
                    if (cookie.getDomain() != null) {
                        this.addIndex(this.domainIndex, cookie.getDomain(), cookie);
                    }

                    if (uri != null) {
                        this.addIndex(this.uriIndex, this.getEffectiveURI(uri), cookie);
                    }
                }
            } finally {
                this.lock.unlock();
            }
            persistInFileLocalStorage();
        }
    }

    public List<HttpCookie> get(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri is null");
        } else {
            List<HttpCookie> cookies = new ArrayList();
            boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
            this.lock.lock();

            try {
                this.getInternal1(cookies, this.domainIndex, uri.getHost(), secureLink);
                this.getInternal2(cookies, this.uriIndex, this.getEffectiveURI(uri), secureLink);
            } finally {
                this.lock.unlock();
            }

            return cookies;
        }
    }

    public List<HttpCookie> getCookies() {
        this.lock.lock();

        List rt;
        try {
            Iterator<HttpCookie> it = this.cookieJar.iterator();

            while(it.hasNext()) {
                if (((HttpCookie)it.next()).hasExpired()) {
                    it.remove();
                }
            }
        } finally {
            rt = Collections.unmodifiableList(this.cookieJar);
            this.lock.unlock();
        }

        return rt;
    }

    public List<URI> getURIs() {
        List<URI> uris = new ArrayList();
        this.lock.lock();

        try {
            Iterator<URI> it = this.uriIndex.keySet().iterator();

            while(it.hasNext()) {
                URI uri = (URI)it.next();
                List<HttpCookie> cookies = (List)this.uriIndex.get(uri);
                if (cookies == null || cookies.size() == 0) {
                    it.remove();
                }
            }
        } finally {
            uris.addAll(this.uriIndex.keySet());
            this.lock.unlock();
        }

        return uris;
    }

    public boolean remove(URI uri, HttpCookie ck) {
        if (ck == null) {
            throw new NullPointerException("cookie is null");
        } else {
            boolean modified = false;
            this.lock.lock();

            try {
                modified = this.cookieJar.remove(ck);
            } finally {
                this.lock.unlock();
            }
            persistInFileLocalStorage();
            return modified;
        }
    }

    public boolean removeAll() {
        this.lock.lock();

        boolean var1;
        try {
            if (!this.cookieJar.isEmpty()) {
                this.cookieJar.clear();
                this.domainIndex.clear();
                this.uriIndex.clear();
                return true;
            }

            var1 = false;
        } finally {
            this.lock.unlock();
        }
        persistInFileLocalStorage();
        return var1;
    }

    private boolean netscapeDomainMatches(String domain, String host) {
        if (domain != null && host != null) {
            boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
            int embeddedDotInDomain = domain.indexOf(46);
            if (embeddedDotInDomain == 0) {
                embeddedDotInDomain = domain.indexOf(46, 1);
            }

            if (isLocalDomain || embeddedDotInDomain != -1 && embeddedDotInDomain != domain.length() - 1) {
                int firstDotInHost = host.indexOf(46);
                if (firstDotInHost == -1 && isLocalDomain) {
                    return true;
                } else {
                    int domainLength = domain.length();
                    int lengthDiff = host.length() - domainLength;
                    if (lengthDiff == 0) {
                        return host.equalsIgnoreCase(domain);
                    } else if (lengthDiff > 0) {
                        host.substring(0, lengthDiff);
                        String D = host.substring(lengthDiff);
                        return D.equalsIgnoreCase(domain);
                    } else if (lengthDiff != -1) {
                        return false;
                    } else {
                        return domain.charAt(0) == '.' && host.equalsIgnoreCase(domain.substring(1));
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void getInternal1(List<HttpCookie> cookies, Map<String, List<HttpCookie>> cookieIndex, String host, boolean secureLink) {
        ArrayList<HttpCookie> toRemove = new ArrayList();

        for(Map.Entry<String, List<HttpCookie>> entry : cookieIndex.entrySet()) {
            String domain = (String)entry.getKey();
            List<HttpCookie> lst = (List)entry.getValue();

            for(HttpCookie c : lst) {
                if (c.getVersion() == 0 && this.netscapeDomainMatches(domain, host) || c.getVersion() == 1 && HttpCookie.domainMatches(domain, host)) {
                    if (this.cookieJar.indexOf(c) != -1) {
                        if (!c.hasExpired()) {
                            if ((secureLink || !c.getSecure()) && !cookies.contains(c)) {
                                cookies.add(c);
                            }
                        } else {
                            toRemove.add(c);
                        }
                    } else {
                        toRemove.add(c);
                    }
                }
            }

            for(HttpCookie c : toRemove) {
                lst.remove(c);
                this.cookieJar.remove(c);
            }

            toRemove.clear();
        }

    }

    private <T> void getInternal2(List<HttpCookie> cookies, Map<T, List<HttpCookie>> cookieIndex, Comparable<T> comparator, boolean secureLink) {
        for(T index : cookieIndex.keySet()) {
            if (comparator.compareTo(index) == 0) {
                List<HttpCookie> indexedCookies = (List)cookieIndex.get(index);
                if (indexedCookies != null) {
                    Iterator<HttpCookie> it = indexedCookies.iterator();

                    while(it.hasNext()) {
                        HttpCookie ck = (HttpCookie)it.next();
                        if (this.cookieJar.indexOf(ck) != -1) {
                            if (!ck.hasExpired()) {
                                if ((secureLink || !ck.getSecure()) && !cookies.contains(ck)) {
                                    cookies.add(ck);
                                }
                            } else {
                                it.remove();
                                this.cookieJar.remove(ck);
                            }
                        } else {
                            it.remove();
                        }
                    }
                }
            }
        }

    }

    private <T> void addIndex(Map<T, List<HttpCookie>> indexStore, T index, HttpCookie cookie) {
        if (index != null) {
            List<HttpCookie> cookies = (List)indexStore.get(index);
            if (cookies != null) {
                cookies.remove(cookie);
                cookies.add(cookie);
            } else {
                cookies = new ArrayList();
                cookies.add(cookie);
                indexStore.put(index, cookies);
            }
        }

    }

    private URI getEffectiveURI(URI uri) {
        URI effectiveURI = null;

        try {
            effectiveURI = new URI("http", uri.getHost(), (String)null, (String)null, (String)null);
        } catch (URISyntaxException var4) {
            effectiveURI = uri;
        }

        return effectiveURI;
    }


    public static Map<String, String> serializeCookie(HttpCookie cookie) {
        Map<String, String> data = new HashMap<>();
        data.put("name", cookie.getName());
        data.put("value", cookie.getValue());
        data.put("domain", cookie.getDomain());
        data.put("path", cookie.getPath());
        data.put("maxAge", String.valueOf(cookie.getMaxAge()));
        data.put("secure", String.valueOf(cookie.getSecure()));
        data.put("httpOnly", String.valueOf(cookie.isHttpOnly()));
        return data;
    }

    public static HttpCookie deserializeCookie(Map<String, String> data) {
        HttpCookie cookie = new HttpCookie(data.get("name"), data.get("value"));
        cookie.setDomain(data.get("domain"));
        cookie.setPath(data.get("path"));
        cookie.setMaxAge(Long.parseLong(data.get("maxAge")));
        cookie.setSecure(Boolean.parseBoolean(data.get("secure")));
        cookie.setHttpOnly(Boolean.parseBoolean(data.get("httpOnly")));
        return cookie;
    }


    void persistInFileLocalStorage() {
        /*
        fileLocalStorage.setItem("cookieJar", cookieJar);
        fileLocalStorage.setItem("domainIndex", domainIndex);
        fileLocalStorage.setItem("uriIndex", uriIndex);
        fileLocalStorage.setItem("lock", lock);
         */
        List<Map<String, String>> cookieJarSerialized = new ArrayList<>();
        for (HttpCookie cookie : cookieJar) {
            cookieJarSerialized.add(serializeCookie(cookie));
        }
        fileLocalStorage.setItem("cookieJar", cookieJarSerialized);
        Map<String, List<Map<String, String>>> domainIndexSerialized = new HashMap<>();
        for (Map.Entry<String, List<HttpCookie>> entry : domainIndex.entrySet()) {
            String domain = entry.getKey();
            List<HttpCookie> cookies = entry.getValue();
            List<Map<String, String>> serializedCookies = new ArrayList<>();
            for (HttpCookie cookie : cookies) {
                serializedCookies.add(serializeCookie(cookie));
            }
            domainIndexSerialized.put(domain, serializedCookies);
        }
        fileLocalStorage.setItem("domainIndex", domainIndexSerialized);
        Map<URI, List<Map<String, String>>> uriIndexSerialized = new HashMap<>();
        for (Map.Entry<URI, List<HttpCookie>> entry : uriIndex.entrySet()) {
            URI uri = entry.getKey();
            List<HttpCookie> cookies = entry.getValue();
            List<Map<String, String>> serializedCookies = new ArrayList<>();
            for (HttpCookie cookie : cookies) {
                serializedCookies.add(serializeCookie(cookie));
            }
            uriIndexSerialized.put(uri, serializedCookies);
        }
        fileLocalStorage.setItem("uriIndex", uriIndexSerialized);
        fileLocalStorage.setItem("lock", lock);
    }

    public void readFromFileLocalStorage() {
        List<Map<String, String>> cookieJarSerialized = fileLocalStorage.getItem("cookieJar", List.class);
        if (cookieJarSerialized == null) {
            this.cookieJar = new ArrayList();
        } else {
            this.cookieJar = new ArrayList<>();
            for (Map<String, String> cookieData : cookieJarSerialized) {
                this.cookieJar.add(deserializeCookie(cookieData));
            }
        }
        Map<String, List<Map<String, String>>> domainIndexSerialized = fileLocalStorage.getItem("domainIndex", Map.class);
        if (domainIndexSerialized == null) {
            this.domainIndex = new HashMap();
        } else {
            this.domainIndex = new HashMap<>();
            for (Map.Entry<String, List<Map<String, String>>> entry : domainIndexSerialized.entrySet()) {
                String domain = entry.getKey();
                List<Map<String, String>> serializedCookies = entry.getValue();
                List<HttpCookie> cookies = new ArrayList<>();
                for (Map<String, String> cookieData : serializedCookies) {
                    cookies.add(deserializeCookie(cookieData));
                }
                this.domainIndex.put(domain, cookies);
            }
        }
        Map<URI, List<Map<String, String>>> uriIndexSerialized = fileLocalStorage.getItem("uriIndex", Map.class);
        if (uriIndexSerialized == null) {
            this.uriIndex = new HashMap();
        } else {
            this.uriIndex = new HashMap<>();
            for (Map.Entry<URI, List<Map<String, String>>> entry : uriIndexSerialized.entrySet()) {
                URI uri = entry.getKey();
                List<Map<String, String>> serializedCookies = entry.getValue();
                List<HttpCookie> cookies = new ArrayList<>();
                for (Map<String, String> cookieData : serializedCookies) {
                    cookies.add(deserializeCookie(cookieData));
                }
                this.uriIndex.put(uri, cookies);
            }
        }
        ReentrantLock lockSerialized = fileLocalStorage.getItem("lock", ReentrantLock.class);
        if (lockSerialized == null) {
            this.lock = new ReentrantLock();
        } else {
            this.lock = lockSerialized;
        }
    }

}
