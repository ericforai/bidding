# Implementation Plan: High-Fidelity Tender-to-Project Conversion Engine

## Phase 1: Markdown Sidecar & Pipeline Integration
- [ ] Task: Develop minimal Python Sidecar using `MarkItDown` + FastAPI (returning rich structure JSON).
- [ ] Task: Integrate Sidecar as a new `TenderDocumentTextExtractor` adapter in the `tenderupload` module.
- [ ] Task: Write Tests (Verify PDF -> Sidecar -> Java rich metadata flow, with fallback to legacy extractor).
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Pipeline established' (Protocol in workflow.md)

## Phase 2: Structural Chunking & Evidence Anchoring
- [ ] Task: Implement Java "Structural Chunker" using Sidecar headings and character offsets.
- [ ] Task: Update AI Extraction Logic to save "Source Excerpts" and "Section Paths" for every field.
- [ ] Task: Write Tests (Evaluate extraction quality improvements via Markdown structure).
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Evidence capture' (Protocol in workflow.md)

## Phase 3: Pure-Java Business Guardrails
- [ ] Task: Implement domain-level validation rules (Budget Sanity, Timeline conflicts) in Java.
- [ ] Task: Implement Qualification Matcher (Cross-referencing extracted requirements vs internal pool).
- [ ] Task: Finalize "Evidence-Linked" Conversion View in the frontend.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Business Closure' (Protocol in workflow.md)
