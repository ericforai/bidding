package com.xiyu.bid.projectworkflow.parser;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ScoreDraftFileTypePolicy {

    public FileType detect(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".docx")) {
            return FileType.DOCX;
        }
        if (lower.endsWith(".doc")) {
            return FileType.DOC;
        }
        if (lower.endsWith(".pdf")) {
            throw new IllegalArgumentException("暂不支持 PDF 评分表解析，请优先上传 Word 版本评分标准");
        }
        throw new IllegalArgumentException("仅支持 .doc/.docx 评分文件");
    }
}
