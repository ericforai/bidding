# Implementation Plan: Architecture & Robustness Hardening

## Phase 1: Baseline & Triage
- [ ] Task: Generate a targeted Checkstyle report for VisibilityModifier, Imports, and DesignForExtension.
- [ ] Task: Validate current build and test stability (1,188+ tests).
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Baseline & Triage' (Protocol in workflow.md)

## Phase 2: Automated Import & Style Cleanup
- [ ] Task: Write Tests (Verify no regressions in basic module loading).
- [ ] Task: Implement: Use IDE/Script tools to resolve all `UnusedImports` and `AvoidStarImport` violations.
- [ ] Task: Verify 0 import violations via Checkstyle.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Automated Import & Style Cleanup' (Protocol in workflow.md)

## Phase 3: Encapsulation Hardening (P0)
- [ ] Task: Write Tests (Ensure high-coverage for entities and DTOs being modified).
- [ ] Task: Implement: Convert package-private or public fields to `private` in Entities and DTOs (VisibilityModifier).
- [ ] Task: Implement: Refactor affected call sites or ensure Lombok coverage for the new private fields.
- [ ] Task: Verify 0 VisibilityModifier violations via Checkstyle.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Encapsulation Hardening' (Protocol in workflow.md)

## Phase 4: Extension Design Reduction (P2)
- [ ] Task: Write Tests (Verify architecture tests for final classes).
- [ ] Task: Implement: Apply `final` keyword to Service and Component classes that are not intended for extension.
- [ ] Task: Run full `mvn test` and `mvn checkstyle:checkstyle` to confirm improvements.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Extension Design Reduction' (Protocol in workflow.md)