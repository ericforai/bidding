package com.xiyu.bid.biddraftagent.infrastructure.tenderdocument;

import com.xiyu.bid.biddraftagent.application.ExtractedTenderDocument;
import com.xiyu.bid.biddraftagent.application.TenderDocumentTextExtractor;
import com.xiyu.bid.projectworkflow.parser.FileType;
import com.xiyu.bid.projectworkflow.parser.WordTextExtractor;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * @deprecated 已由 {@link com.xiyu.bid.docinsight.infrastructure.extractor.MarkItDownSidecarExtractor}
 *             及其降级路径取代。新代码请使用 docinsight 模块的提取器。将在 next-release 移除。
 */
@Deprecated(since = "next-release", forRemoval = true)
@Component
@RequiredArgsConstructor
public class PoiPdfTenderDocumentTextExtractor implements TenderDocumentTextExtractor {

    private static final int MIN_TEXT_LENGTH = 80;

    private final WordTextExtractor wordTextExtractor;

    @Override
    public ExtractedTenderDocument extract(String fileName, String contentType, byte[] content) {
        String text = normalize(extractByType(fileName, content));
        if (text.length() < MIN_TEXT_LENGTH) {
            throw new IllegalArgumentException("未能从招标文件提取有效正文；扫描件 PDF 请先 OCR 或上传 Word/文本型 PDF");
        }
        return new ExtractedTenderDocument(fileName, contentType, text, text.length(), "poi-pdfbox-v1");
    }

    private String extractByType(String fileName, byte[] content) {
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            if (lowerName.endsWith(".docx")) {
                return wordTextExtractor.extract(inputStream, FileType.DOCX);
            }
            if (lowerName.endsWith(".doc")) {
                return wordTextExtractor.extract(inputStream, FileType.DOC);
            }
            if (lowerName.endsWith(".pdf")) {
                return extractPdfText(content);
            }
            throw new IllegalArgumentException("仅支持 .doc、.docx、文本型 .pdf 招标文件");
        } catch (IOException ex) {
            throw new IllegalStateException("读取招标文件正文失败", ex);
        }
    }

    private String extractPdfText(byte[] content) throws IOException {
        try (PDDocument document = PDDocument.load(content)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
