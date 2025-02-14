package com.axreng.backend;

import java.util.concurrent.*;

import com.axreng.backend.dto.SearchRequest;
import com.axreng.backend.dto.SearchResponse;
import com.axreng.backend.crawler.WebCrawler;
import com.google.gson.Gson;
import static spark.Spark.*;

public class Main {
    private static final Gson gson = new Gson();
    private static final ConcurrentHashMap<String, WebCrawler> searches = new ConcurrentHashMap<>();
    private static final String APPLICATION_JSON = "application/json";
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        port(4567);

        get("/crawl/:id", (req, res) -> {
            String id = req.params(":id");
            WebCrawler task = searches.get(id);
            if (task == null) {
                res.status(404);
                return "Search not found for ID: " + id;
            }

            res.type(APPLICATION_JSON);
            return gson.toJson(task.getResult());
        });

        post("/crawl", (req, res) -> {
            SearchRequest request = gson.fromJson(req.body(), SearchRequest.class);
            if (request.getKeyword() == null || request.getKeyword().length() < 4 || request.getKeyword().length() > 32) {
                res.status(400);
                return "Invalid keyword length";
            }

            String id = generateId();
            WebCrawler task = new WebCrawler(id, request.getKeyword());
            searches.put(id, task);
            executor.submit(task);

            res.type(APPLICATION_JSON);
            return gson.toJson(new SearchResponse(id));
        });
    }

    private static String generateId() {
        return java.util.UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "").substring(0, 8);
    }
}
