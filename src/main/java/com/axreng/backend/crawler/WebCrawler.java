package com.axreng.backend.crawler;

import com.axreng.backend.Main;
import com.axreng.backend.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler implements Runnable {
    private final String id;
    private final String keyword;
    private final ConcurrentLinkedQueue<String> urls = new ConcurrentLinkedQueue<>();
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final String baseUrl = System.getenv("BASE_URL");
    private final AtomicBoolean isActive = new AtomicBoolean(true);
    private final Pattern linkPattern = Pattern.compile("href=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE);
    private final Pattern tagPattern = Pattern.compile("<[^>]+>"); // remove tags HTML
    private Logger LOG = LoggerFactory.getLogger(WebCrawler.class);

    public WebCrawler(String id, String keyword) {
        this.id = id;
        this.keyword = keyword.toLowerCase();
    }

    @Override
    public void run() {
        if (baseUrl == null) {
            LOG.error("Error: BASE_URL not defined");
            isActive.set(false);
            return;
        }
        
        try {
            crawl(baseUrl);
        } finally {
            isActive.set(false);
        }
    }
    
    private void crawl(String url) {
        if (!isActive.get() || !visitedUrls.add(url)) { // visitedUrls.add return false if URL was already visited
            return;
        }

        try {
            String html = fetchHtml(url);

            // Remove tags HTML and convert to lowercase
            String textContent = tagPattern.matcher(html).replaceAll(" ").toLowerCase();
            textContent = decodeHtmlEntities(textContent); // Decoding HTML entities

            // search keyword anywhere
            Pattern keywordPattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher keywordMatcher = keywordPattern.matcher(textContent);

            if (keywordMatcher.find()) {
                urls.add(url);
            }

            // extract and give following links
            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String nextUrl = matcher.group(1);
                if (!nextUrl.startsWith("http")) {
                    nextUrl = new URL(new URL(baseUrl), nextUrl).toString(); // Resolve relative URLs correctly
                }

                if (nextUrl.startsWith(baseUrl)) {
                    crawl(nextUrl);
                }
            }

        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    private String fetchHtml(String urlStr) throws Exception {
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return content.toString();
    }

    // Decoding of basic HTML
    private String decodeHtmlEntities(String text) {
        return text.replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&apos;", "'");
    }

    public SearchResult getResult() {
        return new SearchResult(id, isActive.get() ? "active" : "done", urls);
    }
}
