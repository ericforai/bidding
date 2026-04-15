package com.xiyu.bid.controller;

import com.xiyu.bid.service.CebCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * 手动触发爬虫接口，供管理员测试或手动补登
 */
@RestController
@RequestMapping("/api/admin/crawler")
@RequiredArgsConstructor
public class CebCrawlerController {

    private final CebCrawlerService cebCrawlerService;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerCrawler(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int rows) {
            
        int savedCount = cebCrawlerService.crawlAndSaveTenders(keyword, page, rows);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Crawler executed successfully",
                "savedCount", savedCount,
                "keyword", keyword,
                "page", page,
                "rows", rows
        ));
    }
}
