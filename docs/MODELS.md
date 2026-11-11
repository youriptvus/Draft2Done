# Model choices and verification

Reviewed 2026-11-10 (America/New_York). No model was downloaded or changed during this research.

For the actual remote phone, use the direct ChatGPT account route when eligible usage is available. Choose only a slug returned for that account. Local model names in this document are optional server choices, not ChatGPT model identifiers. The Samsung successfully authenticated and fetched its catalog. After historical usage-limit failures, actual Helper, continuation, regeneration and query-only research now pass; this establishes integration behavior, not a comparative model-quality benchmark.

Recommendations should preserve an explicit user selection and distinguish the provider's capability claims from this application's tests. A catalog recommendation cannot guarantee factual correctness, source support or Writer DNA fidelity.

## Primary ChatGPT choices

Guidance dated 2026-11-10, aligned with `web/model-policy.js` and official OpenAI documentation. Among the recognized choices, Astra is the recommendation for demanding research; Sol balances capability and responsiveness, Terra suits natural writing from supplied material, and Luna prioritizes efficiency. These are starting points for review, not this app's inference benchmarks. [Model selection](https://developers.openai.com/api/docs/guides/model-selection), [GPT-6 family guidance](https://developers.openai.com/api/docs/guides/latest-model)

| Observed model slug | Recognized role | Practical choice |
| --- | --- | --- |
| `gpt-6-astra` | Deeper research | Strongest recognized option for demanding analysis; expect greater latency and usage |
| `gpt-6.1-sol` | Balanced research | Complex work with a balance of capability and responsiveness |
| `gpt-6.1-terra` | Natural writing | Clear, concise writing based on the material supplied |
| `gpt-6-luna` | Efficient research | Focused tasks and efficient repeated use; review complex evidence closely |

The actual signed-in Samsung catalog returned, in its observed order: `gpt-6.1-sol`, `gpt-6-astra`, `gpt-6.1-terra`, `gpt-6-sol`, `gpt-6-luna`, `gpt-5.6-sol`, `gpt-5.6-terra`, `gpt-5.6-luna`. Model loading, selecting the recommendation and completed writing/query-only research were verified. This list records availability for that account at that time, not availability for every user or a promise of unused quota. Current web-tool verification is recorded separately in VERIFICATION.md.

The app preserves the returned catalog and explicit user choice. Its recommendation selects only an eligible returned slug; older additional choices remain available for manual selection. Refresh the catalog after changing accounts. A catalog entry does not establish tool availability or waive a usage limit. [Plan-backed model catalog and terminal inference contract](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference)

## Existing server models actually evaluated

`llama3:8b-instruct-q4_K_M` was the stronger practical choice among the two already-installed models tested for writing. The final guarded run produced five reviewable responses for five fictional samples, with the full drafts retained and no unknown citation tokens in that run. Cold review still found unconfirmed personal details, an invented study premise, reasoning overstatement and approximate voice matching. `test-results/style-live-final.json` contains actual output and qualitative notes.

The initial `qwen2.5-coder:3b` run yielded two reviewable outputs and three output-limit/invalid-JSON refusals. It also repeated draft material and invented unsupported details. The comparison changed model and output budget, so it does not isolate their effects. These observations support a general-purpose writing model over that small coding model for the tested server; they do not establish a universal ranking.

These server results do not create a connection through the production phone's firewall. The local backend remains advanced functionality for a reachable server.

## Optional server candidates, all untested in this app

The target server has an RTX 5050 with about 8 GB VRAM and limited system RAM. Published artifact sizes below describe storage/download size, not total running memory. Rankings here are practical candidates for a future controlled comparison, not measured upgrades.

| Exact optional tag | Published artifact | Current compatibility evidence | Why evaluate it |
| --- | --- | --- | --- |
| [`qwen3.5:4b-q4_K_M`](https://ollama.com/library/qwen3.5:4b-q4_K_M) | 3.3 GB | Current [registry configuration](https://registry.ollama.ai/v2/library/qwen3.5/blobs/sha256:788a4d94bc3f431ac0cd067005be4b838a09330427962952420b05d10ecf84db) requires Ollama **0.30.0** | A newer general-purpose candidate with more memory headroom than its 9B relative. Thinking behavior, request-schema compatibility and actual writing quality need testing before recommendation as a default |
| [`qwen3:4b-instruct-2507-q4_K_M`](https://ollama.com/library/qwen3:4b-instruct-2507-q4_K_M) | 2.5 GB | Current [config](https://registry.ollama.ai/v2/library/qwen3/blobs/sha256:b72accf9724e93698c57cbd3b1af2d3341b3d05ec2089d86d273d97964853cd2) has no `requires` field; an exact current minimum was not established | Lower-memory instruction model to evaluate for straightforward writing. Qwen describes the 2507 Instruct variant as its updated non-thinking line; that can simplify bounded paragraph generation. [Qwen release description](https://github.com/QwenLM/Qwen3#qwen3-2507) |
| [`llama3.1:8b-instruct-q4_K_M`](https://ollama.com/library/llama3.1:8b-instruct-q4_K_M) | 4.9 GB | Current [config](https://registry.ollama.ai/v2/library/llama3.1/blobs/sha256:455f34728c9b5dd3376378bfb809ee166c145b0b4c1f1a6feca069055066ef9a) has no `requires` field; [Ollama 0.3.0 release](https://github.com/ollama/ollama/releases/tag/v0.3.0) explicitly supports this family, without proving an exact minimum | A familiar successor to the tested Llama 3 model. Its advertised longer context and general capabilities do not prove better voice preservation or safe maximum context on this GPU |
| [`gemma3:4b-it-q4_K_M`](https://ollama.com/library/gemma3:4b-it-q4_K_M) | 3.4 GB | Current [registry configuration](https://registry.ollama.ai/v2/library/gemma3/blobs/sha256:4bf7b0a45f1af5d9f6f8b847044d8379eef6b36f3ca8b4dcc63a8dfed7426e53) requires Ollama **0.30.0** | A different compact general-purpose family for a writing/summarization comparison. Its vision support is not used by the current writing route; faithful text style remains untested |

The practical next *evaluation* candidate is Qwen3.5 4B after a deliberate compatible server update; Qwen3 4B Instruct is a smaller alternative. The already-installed tested Llama remains preferable to claiming an untested model is best. Existing Ollama 0.19.4 cannot be assumed compatible with freshly republished tags requiring 0.30.0. Historical launch announcements or family README minimums can differ from the current exact tag's metadata; recheck the tag before any authorized installation.

The official Qwen3.5 family page's headline benchmark table describes a much larger model. Those results must not be assigned to the 4B model. Published capabilities are useful for selecting candidates, not evidence of this app's academic-writing accuracy. [Qwen3.5 family page](https://ollama.com/library/qwen3.5)

## Memory, context and newer-family limits

Start optional-server testing with one loaded model and a modest 4K context; consider 8K only after measuring memory use and confirming the entire approved request fits. Model weights, KV cache, working buffers, other GPU programs and system RAM all affect fit. The existing Llama tests used 8K, but this is not a guarantee for another architecture. Increasing context increases memory consumption; check actual placement with `ollama ps`. [Ollama context guidance](https://docs.ollama.com/context-length), [GPU/CPU placement](https://docs.ollama.com/faq#how-can-i-tell-if-my-model-was-loaded-onto-the-gpu)

[`qwen3.5:9b-q4_K_M`](https://ollama.com/library/qwen3.5:9b-q4_K_M) is 6.6 GB before runtime overhead, so it is a tight, unverified choice for 8 GB VRAM and limited system RAM. It is not the default recommendation. The newer Qwen3.8 27B artifact is 18 GB and Qwen3.6 variants start around 18 GB, making them impractical full-GPU candidates for this setup. [Qwen3.8](https://ollama.com/library/qwen3.8), [Qwen3.6](https://ollama.com/library/qwen3.6)

Gemma 4 E2B illustrates why parameter labels alone are insufficient: its current Ollama artifacts span 4.6–7.5 GB, while the GGUF registry config requires 0.30.9. It is another untested future option, not an automatic replacement. [Official tag](https://ollama.com/library/gemma4:e2b), [current config](https://registry.ollama.ai/v2/library/gemma4/blobs/sha256:3ff8a734c316b74964b7c7f7318e56ad85077da655a985aaa73f466c450d993a)

Thinking-capable models can consume additional time and output budget before useful prose. A future server upgrade must verify model capabilities and appropriate thinking controls against the actual server, then repeat full-draft, JSON, citation, missing-detail and five-style checks. No advertised 128K/256K context should be treated as fitting this machine. Draft2Done refuses oversized requests instead of silently truncating a paper.
