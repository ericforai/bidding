-- 强制约束：tender_id + reminder_type 联合唯一
-- 防止并发双击/重复订阅导致重复提醒设置记录
-- 同时优化查询：findByTenderIdAndReminderType 可命中覆盖索引

ALTER TABLE tender_reminder_settings
    ADD CONSTRAINT uk_tender_reminder_tender_type
    UNIQUE (tender_id, reminder_type);
