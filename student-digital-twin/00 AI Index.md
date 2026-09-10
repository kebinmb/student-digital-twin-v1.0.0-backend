# AGY System Instruction & Engineering Directives

## 1. Execution Protocol (Pre-flight Gate)
Before writing or modifying any code, verify whether the task touches Backend, Frontend, or Security. Apply the matching rules below:

---

## 2. Backend Architecture Directives
* **Framework & Runtime**: Spring Boot 4.1.0 with Java 21 LTS.
* **Modern Java Idioms**: Leverage Java 21 features (Virtual Threads / Project Loom, Record classes, Pattern Matching, Sealed classes, Sequenced Collections).
* **Architecture**: Clean layered architecture (Controller -> Service -> Repository/Store). Use modern Spring `@HttpExchange` or `RestClient` over legacy templates.
* **Security Reference**:
  - Always consult: `/03 Resources/00 Login and Authentication Springboot 4.1.0` (mapped from `/02 Resources`)
  - Historic context/implementation: `/04 Archives/01 Login and Authentication Backend and Frontend Implementation` (mapped from `/03 Archives`)
  - Active Specifications & Roadmaps: `/01 Projects/`
  - System Topology & QA Standards: `/02 Areas/`

---

## 3. Frontend Architecture Directives
* **Framework**: Angular 19+ (Standalone Components, Signals, `computed()`, `inject()`, Control Flow `@if` / `@for`, `takeUntilDestroyed`).
* **UI Library**: PrimeNG 19+ with Design Tokens and Pass-Through attributes (`pt`).
* **Design & Styling Authority**:
  - Strictly adhere to global styles in `src/styles.css` and token configuration in `src/theme/custom-theme.ts`.
  - **No rogue ad-hoc CSS**: Check existing utility classes before adding new selectors.
  - **Color Palette & Brand Identity**:
    - Primary Forest Green: `#116834` (hover: `#0c5027`, light tint: `#f0fdf4`, border: `#bbf7d0`)
    - Warning/Gold Accent: `#d97706` (tint: `#fffbeb`, border: `#fde68a`)
    - Neutral Slate Borders: `#cbd5e1`, `#e2e8f0`
    - Text Neutral: `#0f172a` (navy headings), `#475569` (body slate), `#64748b` (muted subtext)
  - **Encapsulation & Layout**:
    - Prefer flex utility layout pairings (`flex-col`, `flex-row`, `flex-between`, `flex-center`) with standard gaps (`gap-xs` to `gap-2xl`).
    - Eliminate conflicting inline classes (e.g., avoid combining `flex-row` and `flex-col` on the same tag).
    - Limit `::ng-deep` exclusively to un-stylable PrimeNG overlays; prefer PrimeNG `styleClass` or design tokens.

---

## 4. Quality Checklist Prior to Returning Code
1. [ ] Did you check `custom-theme.ts` for established color variables and border radiuses?
2. [ ] Are all newly added HTML classes defined in the component's CSS or the global stylesheet?
3. [ ] Are signal-based reactivity principles respected (no manual `cdr.detectChanges()` where `computed` or signals apply)?
4. [ ] Did security changes cross-reference the 03/04 authentication guides?