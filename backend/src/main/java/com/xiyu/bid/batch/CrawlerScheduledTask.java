package com.xiyu.bid.batch;

import com.xiyu.bid.service.CebCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 爬虫定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlerScheduledTask {

    private final CebCrawlerService cebCrawlerService;

    /**
     * 定时抓取中国招标投标公共服务平台数据
     * 每天凌晨2点、8点、14点、20点执行
     */
    @Scheduled(cron = "0 0 2,8,14,20 * * ?")
    public void scheduleCebCrawl() {
        log.info("Starting scheduled CEB data crawl...");
        try {
            // 抓取第一页，每页150条 (由于是增量，我们假设两次抓取间隔内新增约这个数，更严谨的应该要循环分页直到遇到重复的为止)
            int savedCount = cebCrawlerService.crawlAndSaveTenders("", 1, 150);
            log.info("Scheduled CEB data crawl finished. Saved {} records.", savedCount);
        } catch (RuntimeException e) {
            log.error("Error during scheduled CEB data crawl", e);
        }
    }
}
