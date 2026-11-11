# AI and research connections

The standalone Android phone uses a direct native ChatGPT connection for approved writing assistance. It does not need access to the user's PC. Offline editing, storage, descriptive DNA and document tools remain available without a connection. No on-phone language model is bundled.

| AI route | Data approved for sending | Behavior and boundary |
| --- | --- | --- |
| Direct ChatGPT Helper/Complete/regenerate | Exact full current draft record, requirements/rubric, Writer DNA guidance and included evidence excerpts | Fresh preview and explicit approval before every actual request, including each bounded completion round. Payload is frozen; changes to the draft, requirements, profile or selected model invalidate consent |
| Separate ChatGPT research | Approved short query and chosen source excerpts | Does not automatically include the full paper. Web search and returned citations depend on the account/model policy |
| Advanced Ollama or compatible server | Full writing request to the explicitly configured reachable server | Optional for other environments and developer QA. The remote production phone cannot use the user's firewalled PC endpoint |

The ChatGPT route uses the documented [open-source account integration](https://developers.openai.com/siwc/token-sharing-open-source). OAuth credentials remain in Android's encrypted native vault. The native writing adapter verifies the selected slug against the current account catalog, converts the reviewed system instructions and user record to a Responses request, sets `store:false` and `stream:true`, and accepts only a terminal completed response. It supplies no writing tools or unsupported sampling/output-token parameters. ChatGPT writing does not use a PC transport or an API-key fallback.

Direct ChatGPT writing preserves the full reviewed request up to 200,000 total message characters and 1 MiB serialized payload. Advanced server context settings apply only to server backends; OpenAI enforces the selected account model's actual context. The app sends no unsupported output-token override. Continuation steps and round counts remain bounded in the app. An oversized request is rejected without truncation, and changed content requires approval again.

Account usage errors stop generation; there is no automatic retry through another account, model or paid provider. An intentional later request requires approval again. Advanced local adapters retain their configured context guard because the complete request must fit that server's configured window.

Real browser sign-in, identity verification, saved encrypted session and the account-specific model catalog succeeded on the Samsung. Actual plan requests returned `subscription_sharing_usage_limit_exceeded`. Successful plan inference and web-search tool availability remain unverified. That error does not establish a reset time or the user's entire plan balance; current observed status is in [VERIFICATION.md](VERIFICATION.md).

Public scholarly APIs are separate optional research sources:

| Provider | Access | Evidence available | Verification |
| --- | --- | --- | --- |
| [Crossref REST](https://www.crossref.org/documentation/retrieve-metadata/rest-api/) | No account/key for public metadata | DOI records; abstracts when deposited | Refetch DOI and compare identity |
| [PubMed E-utilities](https://www.ncbi.nlm.nih.gov/books/NBK25499/) | No key for this low-volume client; requests paced below three/second | PMID metadata and accessible abstracts | Refetch PMID through EFetch |
| [Wikipedia Action API](https://www.mediawiki.org/wiki/API:Main_page) | Public read API | Encyclopedia introductions for background | Refetch article page ID |
| [ChatGPT plan research](https://developers.openai.com/siwc/token-sharing-open-source) | Connected eligible account, available usage and explicit research consent | Brief and web citations if account/model policy permits | Identity/catalog verified on the Samsung; inference currently quota-blocked; links and claims still require checking |

Queries' retrieved records are saved with the project. Ranking uses query-term overlap and DOI deduplication, not a guarantee of scholarly quality. Errors are shown alongside successful results. Retractions, methodology and conflicting evidence need review; language comparisons are tentative, not systematic-review conclusions.

Investigated future integrations: [OpenAlex](https://developers.openalex.org/) and [Semantic Scholar](https://www.semanticscholar.org/product/api). Their endpoint-specific account/key/quotas need checking before implementation; neither is integrated in 0.1.0. Government and university sources can appear through scholarly metadata and optional web search. The app does not scrape restricted sites, bypass paywalls or claim full-text access.

See the [NCBI disclaimer and copyright](https://www.ncbi.nlm.nih.gov/About/disclaimer.html), also linked in Research. Abstract retrieval is not permission to redistribute a publication. No credential is required for the three implemented public APIs.

Local-server model quality observations and optional upgrade candidates are documented in [MODELS.md](MODELS.md). A model's memory or benchmark claims do not establish its accuracy for a particular academic argument. Unknown citation tokens and unsupported links are surfaced or removed; source identities, quotations, personal examples and reasoning still need review.
