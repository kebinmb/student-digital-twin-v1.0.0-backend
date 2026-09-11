## 1. Core Structure Framework (The "C-T-E-C" Model)
- **Context & Persona**: Define the operational scope and technical baseline.
	- *Bad*: `Help me optimize this database query`
	- *Good*: `Act as a PostgreSQL DBA optimizing a multi-tenant SaaS query handling ~10M rows. Prioritize minimizing index scans and memory usage.`
- **Task**: State the primary objective using explicit imperative verbs. State what to do, not just what to avoid.
- **Examples (Few-Shot Prompting)**: Show 1-2 input/output pairs. LLMs mimic patterns far more reliably than they interpret abstract rules.
- **Constraint & Formant**: Specify the response schema (JSON, markdown table, raw code block without explanations, etc.) and explicit guardrails (e.g., "Do not use external libraries", "Limit to O(n) space complexity").

## 2. Standard vs Advanced Prompting Techniques

| Technique                       | How It Works                                                           | Best Used For                                                                      |
| ------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| **Zero-Shot**                   | Direct task instruction without examples.                              | Simple transformations, summarization, or standard boilerplat.                     |
| **Few-Shot**                    | Supplying 1-3 input/output pairs before the target input.              | Enforcing custom output schemas, strict tone matching, or edge-case handling.      |
| **Chain-of-Thought (CoT)**      | Asking the model to show intermediate reasoning ("Think step-by-step") | Mathematical proofs, complex logic trees, and debugging.                           |
| **Role & Persona Framing**      | Assigning an explicit professional identity or knowledge ceiling.      | Calibrating output depth (e.g., senior systems engineer vs introductory tutorial). |
| **Delimited Context Injection** | Wrapping user input, raw data, or docs in markdown fences (``` or ###) | Preventing prompt injection and clarifying what is data vs instruction             |
## 3. Practices Rules of Thumb
- **Isolate Data from Instructions**: Wrap dynamic payloads, schemas, or source documents in explicit tags (e.g., <context>...</context> or """..."""). This prevents the model from conflating instructional rules with raw data.
- **Specify "Negative Constraints" Positively**: Models process affirmative tokens better than negations. Instead of saying *Don't include introductory filler or unnecessary converational commentary*, instruct "*Output only the final SQL statement inside a single code block*".
- **Provide Output Templates**: If you need a specific structure, supply the literal skeleton:
	```markdown
	Return the response using this exact structure:
		-**Summary**:(1-2 sentences)
		-**Identified Bottleneck**: (Bulleted list)
		-**Refactored Code**:(Single snippet)
	```
- **Decouple Thinking from Output (Scratchpads)**: For difficult reasoning or analysis, instruct the model to perform analysis inside a designated block (*e.g., `<analysis>...</analysis>`*) before generating the final deliverable.