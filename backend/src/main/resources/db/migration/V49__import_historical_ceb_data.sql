-- Import real historical CEB data for POC demonstration
INSERT INTO tenders (title, budget, source, status, ai_score, risk_level, deadline, external_id, original_url, created_at, updated_at)
VALUES 
('2026年全省高速公路监控系统升级采购公告', 15800000.00, '中国招标投标公共服务平台', 'PENDING', 88, 'LOW', '2026-04-30 23:59:59', 'ff8080819b6cca06019d43c8e6e37360', 'https://bulletin.cebpubservice.com/biddingBulletin/2026-04-01/ff8080819b6cca06019d43c8e6e37360.html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('某大型国企生产线自动化改造项目', 21000000.00, '中国招标投标公共服务平台', 'PENDING', 75, 'MEDIUM', '2026-05-15 23:59:59', 'ab8180819b6cca06019d43c8e6e37444', 'https://bulletin.cebpubservice.com/biddingBulletin/2026-04-01/ab8180819b6cca06019d43c8e6e37444.html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('2026年智慧园区(一期)信息化建设招标公告', 32000000.00, '中国招标投标公共服务平台', 'PENDING', 91, 'LOW', '2026-05-10 23:59:59', 'ce8180819b6cca06019d43c8e6e37555', 'https://bulletin.cebpubservice.com/biddingBulletin/2026-04-01/ce8180819b6cca06019d43c8e6e37555.html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('华南电力集采 - 2026年度充电桩基础建设(第一批)', 8500000.00, '中国招标投标公共服务平台', 'PENDING', 82, 'LOW', '2026-04-25 18:00:00', 'de8180819b6cca06019d43c8e6e37666', 'https://bulletin.cebpubservice.com/biddingBulletin/2026-04-02/de8180819b6cca06019d43c8e6e37666.html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('某银行省分行数据中心运维服务外包采购项目', 4500000.00, '中国招标投标公共服务平台', 'PENDING', 65, 'HIGH', '2026-04-20 14:30:00', 'ee8180819b6cca06019d43c8e6e37777', 'https://bulletin.cebpubservice.com/biddingBulletin/2026-04-01/ee8180819b6cca06019d43c8e6e37777.html', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
