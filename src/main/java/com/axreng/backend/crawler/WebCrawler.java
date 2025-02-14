package com.axreng.backend.crawler;

import com.axreng.backend.dto.SearchResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler implements Runnable {
    private final String id;
    private final String keyword;
    private final ConcurrentLinkedQueue<String> urls = new ConcurrentLinkedQueue<>();
    private final HashSet<String> visitedUrls = new HashSet<>();
    private final String baseUrl = System.getenv("BASE_URL");
    private final AtomicBoolean isActive = new AtomicBoolean(true);
    private final Pattern linkPattern = Pattern.compile("href=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE);
    
    
    public WebCrawler(String id, String keyword) {
        this.id = id;
        this.keyword = keyword.toLowerCase();
    }

    @Override
    public void run() {
        if (baseUrl == null) {
            System.err.println("Error: BASE_URL not defined");
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
        if (!isActive.get() || visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);
        
        try {
            String html = fetchHtml(url);

            if (html.contains(keyword)) {
                urls.add(url);
            }

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String nextUrl = matcher.group(1);
                if (!nextUrl.startsWith("http")) {
                    nextUrl = baseUrl + (nextUrl.startsWith("/") ? nextUrl : "/" + nextUrl);
                }

                if (nextUrl.startsWith(baseUrl)) {
                    crawl(nextUrl);
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
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
                content.append(line.toLowerCase()).append("\n");
            }
        }

        return content.toString();
    }

    public SearchResult getResult() {
        return new SearchResult(id, isActive.get() ? "active" : "done", urls);
    }
}
