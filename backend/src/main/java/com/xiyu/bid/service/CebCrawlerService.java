package com.xiyu.bid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.dto.ceb.CebResponse;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 中国招标投标公共服务平台 (CEB) 数据抓取服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CebCrawlerService {

    private final TenderRepository tenderRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String CEB_API_URL = "http://www.cebpubservice.com/ctpsp_iiss/searchbusinesstypebeforedooraction/getStringMethod.do";
    private static final String CEB_DETAIL_URL_TEMPLATE = "https://bulletin.cebpubservice.com/bulletin/{date}/{id}.html";

    /**
     * 抓取最新标讯
     *
     * @param keyword 搜索关键字 (可为空)
     * @param pageNo  页码
     * @param rows    每页条数
     * @return 抓取并保存的条数
     */
    @Transactional
    public int crawlAndSaveTenders(String keyword, int pageNo, int rows) {
        log.info("Starting CEB crawl. Keyword: {}, Page: {}, Rows: {}", keyword, pageNo, rows);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Referer", "https://bulletin.cebpubservice.com/");
        headers.set("Origin", "https://bulletin.cebpubservice.com");
        headers.set("Accept", "application/json, text/javascript, */*; q=0.01");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("searchName", keyword != null ? keyword : "");
        body.add("searchArea", "");
        body.add("searchIndustry", "");
        body.add("businessType", "招标公告");
        body.add("pageNo", String.valueOf(pageNo));
        body.add("row", String.valueOf(rows));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(CEB_API_URL, request, String.class);
            log.info("CEB API response received. Status: {}", response.getStatusCode());
            log.debug("CEB Raw Response: {}", response.getBody());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Failed to fetch data from CEB. Status: {}", response.getStatusCode());
                return 0;
            }

            // Print the first 500 chars to logs for debugging
            String responseBody = response.getBody();
            if (responseBody.length() > 500) {
                log.info("Response Preview: {}...", responseBody.substring(0, 500));
            } else {
                log.info("Response Preview: {}", responseBody);
            }

            CebResponse cebResponse = objectMapper.readValue(responseBody, CebResponse.class);
            if (cebResponse == null || cebResponse.getObject() == null || cebResponse.getObject().getReturnlist() == null) {
                log.warn("CEB API returned null or empty result. Success flag: {}", cebResponse != null && cebResponse.isSuccess());
                return 0;
            }

            List<CebResponse.CebTenderItem> items = cebResponse.getObject().getReturnlist();
            int savedCount = 0;

            for (CebResponse.CebTenderItem item : items) {
                if (saveItemIfNotExists(item)) {
                    savedCount++;
                }
            }

            log.info("CEB crawl completed. Found: {}, Saved new: {}", items.size(), savedCount);
            return savedCount;

        } catch (IOException | RuntimeException e) {
            log.error("Exception occurred during CEB crawling:", e);
            return 0;
        }
    }

    /**
     * 将 CEB 项保存为 Tender，如果 externalId 已存在则跳过
     */
    private boolean saveItemIfNotExists(CebResponse.CebTenderItem item) {
        if (item.getBusinessId() == null || item.getBusinessObjectName() == null) {
            return false;
        }

        if (tenderRepository.findByExternalId(item.getBusinessId()).isPresent()) {
            // 已存在，跳过
            return false;
        }

        try {
            Tender tender = new Tender();
            tender.setExternalId(item.getBusinessId());
            tender.setTitle(item.getBusinessObjectName());
            tender.setSource("中国招标投标公共服务平台");
            
            // 构建原始链接 
            // CEB 的新版链接结构通常为: https://bulletin.cebpubservice.com/bulletin/2026-03-24/ff8080819b6cca06019d43c8e6e37360.html
            // 发布日期/截止日期 需要转换为 yyyy-MM-dd
            String dateStr = extractDateOnly(item.getBulletinEndTime());
            if (dateStr != null) {
                tender.setOriginalUrl(CEB_DETAIL_URL_TEMPLATE
                        .replace("{date}", dateStr)
                        .replace("{id}", item.getBusinessId()));
            }

            tender.setDeadline(parseDate(item.getBulletinEndTime()));
            tender.setStatus(Tender.Status.PENDING);
            tender.setAiScore(0);
            
            // 这里我们没法直接拿到预算，可以设置为空或者通过详情页爬取（TODO）
            tender.setBudget(BigDecimal.ZERO); 
            tender.setRiskLevel(Tender.RiskLevel.LOW); // Default to low

            tenderRepository.save(tender);
            return true;
        } catch (RuntimeException e) {
            log.warn("Failed to save CEB item: {}", item.getBusinessObjectName(), e);
            return false;
        }
    }

    private String extractDateOnly(String datetimeStr) {
        if (datetimeStr == null || datetimeStr.length() < 10) {
            return null;
        }
        return datetimeStr.substring(0, 10); // "yyyy-MM-dd"
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            // "yyyy-MM-dd" or "yyyy-MM-dd HH:mm:ss"
            if (dateStr.length() == 10) {
                return LocalDateTime.parse(dateStr + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse date: {}", dateStr);
            return null;
        }
    }
}
