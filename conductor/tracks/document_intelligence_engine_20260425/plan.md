# Implementation Plan: Document Intelligence Engine (DocInsight)

## Phase 1: Backend Extraction & Modularization
- [ ] Task: Create new package `com.xiyu.bid.docinsight`.
- [ ] Task: Move `TenderDocumentStorage`, `StoredTenderDocument`, and related infrastructure (e.g., `LocalTenderDocumentStorage`) to the new module, renaming them generically (e.g., `DocumentStorage`, `StoredDocument`).
- [ ] Task: Move `MarkItDownSidecarTextExtractor` and related DTOs (`ExtractedTenderDocument`) to the new module, renaming them generically.
- [ ] Task: Move `StructuralDocumentChunker` and `TenderDocumentTextChunker` to the new module, generalizing their input/output if necessary.
- [ ] Task: Refactor `BidTenderDocumentImportAppService` and `OpenAiTenderDocumentAnalyzer` to consume the new `docinsight` module services instead of local implementations.

## Phase 2: Generic AI Extraction Pipeline
- [ ] Task: Define a generic `DocumentAnalysisInput` and `DocumentAnalysisResult` interface in `docinsight`.
- [ ] Task: Create a base `OpenAiDocumentAnalyzer` that accepts a customizable schema and prompt template.
- [ ] Task: Refactor the existing tender parsing logic (`OpenAiTenderDocumentAnalyzer`) to extend or utilize this new generic analyzer.
- [ ] Task: Ensure existing tests pass after refactoring.

## Phase 3: Frontend Componentization
- [ ] Task: Create `src/components/common/DocVerificationWorkbench.vue` based on `TenderConversionWorkbench.vue`.
- [ ] Task: Make the new workbench accept generic schema definitions for the left-side form (dynamic rendering based on provided data structure).
- [ ] Task: Replace the usage of `TenderConversionWorkbench.vue` in `ProjectCreate.vue` with the new generic `DocVerificationWorkbench.vue`.
- [ ] Task: Ensure the "Evidence Highlight" feature works flawlessly with the generic component.

## Phase 4: Integration & Verification
- [ ] Task: Create a generic API endpoint (e.g., `/api/docinsight/parse`) to test the standalone engine.
- [ ] Task: Verify the entire tender parsing flow (import -> parse -> verify -> draft) works as expected using the newly abstracted engine.
