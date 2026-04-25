# Specification: Evidence-Driven Tender-to-Project Conversion

## 1. Overview
Upgrade the "Tender to Project" workflow from a data-copying tool to a robust, evidence-linked conversion engine. The core architectural shift involves introducing a Python Sidecar (MarkItDown) to handle structural document parsing, enabling the Java backend to perform high-accuracy extraction and rule-based business validation with full auditability.

## 2. Functional Requirements
- **Sidecar Structural Parsing (P0):**
    - Deploy an independent `document-converter-sidecar` service.
    - Convert various formats (PDF/Word/Excel) to Markdown with rich metadata (headings, tables, page offsets).
- **Evidence-Linked AI Extraction (P0):**
    - Implement structural chunking in Java based on Sidecar-provided headings.
    - Capture and store "Source Excerpts" for every extracted project field to enable user audit.
- **Pure-Java Business Guardrails (P0):**
    - Implement domain validation for Budget (sanity checks), Timeline (logic/holiday conflicts), and Qualifications (automated matching against internal pool).
- **Evidence Workbench (P1):**
    - Unified UI showing extracted data side-by-side with its Markdown source evidence.

## 3. Non-Functional Requirements
- **Resilience:** Fallback to legacy text extraction if Sidecar is unavailable.
- **Explainability:** Extraction confidence and source mapping for all AI-generated fields.
- **Performance:** Optimized chunking to prevent large-document timeouts.

## 4. Acceptance Criteria
- Successful integration of `document-converter-sidecar` as a `TenderDocumentTextExtractor` adapter.
- Verification that >80% of extracted fields link back to correct source excerpts in test documents.
- 100% of defined Java business rules (e.g., Budget/Timeline errors) correctly flagged.

## 5. Out of Scope
- Full-scale microservice platform management (Sidecar will be integrated into existing task pipelines).
- Custom OCR model training.
