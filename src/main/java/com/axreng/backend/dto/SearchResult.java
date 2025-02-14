package com.axreng.backend.dto;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SearchResult {
    private final String id;
    private final String status;
    private final ConcurrentLinkedQueue<String> urls;

    public SearchResult(String id, String status, ConcurrentLinkedQueue<String> urls) {
        this.id = id;
        this.status = status;
        this.urls = urls;
    }
}
