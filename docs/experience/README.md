# ARIA: product experience checkpoint · 5 November 2026

Working branch: `codex/production-foundation`. Production, main and the production database are unchanged.

## 1. Prices — completed

Open local preview: **http://127.0.0.1:8084/control.html#smart**.
Owner credentials: ignored `data/preview/access.txt`. Start/restart instructions remain in `docs/ARIA3.md`.

- The primary screen is a compact table, with one action per product. At 390/768 px it becomes readable product rows; both themes were checked.
- Six searchable filters count the **latest decision for each active product**, across all pages. These are current-workspace counts, not lifetime AI activity. Historical calculations, scenarios, sales, lots, explanations, confirmation, rejection and rollback remain in details.
- Current name and price come from the product record. Historical snapshots are preserved. A changed price/stock or elapsed review time requires a new calculation.
- Opening this section only reads data. “Проверить цены” explicitly invokes the existing engine/authorized autopilot; the existing background manager is preserved.
- KEEP with no confirmed sales explains the lack of evidence. Java calculations are labelled separately from actual AI participation. A skipped request does not become a fake successful AI run or provider failure.

Verification:

- 11 new Java tests passed against a separate PostgreSQL database, including Flyway V1–V28 and Hibernate validation. The previous 190-test baseline remains documented separately; it has not been relabelled as a new full run.
- `scripts/pricing-workspace-e2e.cjs` passed: read-only entry, six width/theme views, search, KEEP, actual confirmation → public price, price history/rollback, current names/prices, stale recalculation, rejection, explicit fault-injection/retry and unchanged owner permissions. Test lots are written off and products archived through the normal audit-preserving services.
- A real request through the owner UI returned `source=AI`, model `dots-studio/dots-3-note-preview:free`. Its validated KEEP decision was subsequently verified in both Pricing and AI Control Center without sending another request. No consents were enabled, quota reset or price applied by this live check. Evidence: `evidence/pricing-live-proof.json`.
- Desktop primary content decreased from approximately 12,129 to 2,500 characters and 78 to 25 enabled buttons (includes filters, search and navigation). Details remain available.

## 2. Social / ARIA Impact — completed

Open `/control.html#socialPlans` for the owner, `/social.html` for organizations and the public.

- The owner first sees proposals, current state and a single next action. Organization settings, all six process steps, source calculation and history are in details. Expired proposals lead to cancellation, not a misleading approval action.
- Drafts do not hold stock. Approval requires a verified eligible organization and explicit goods/storage/transport checks. Responsible staff and time precede handover; the final form separately confirms goods, signed document and actual receipt. Existing Java validity, organization and stock checks remain in force.
- Public Social Shelf uses keyed RU/EN copy for the whole flow: verification, reservations, documents, loading, empty states, errors and retry. Original merchant names/reasons are not translated. This is **not a claim of completed application-wide localization**; administrative modules still need it.
- “Сейчас нет доступных предложений” explains how offers appear. All recorded demo activity is explicitly educational; no real charitable handover is claimed.
- Navigation away from a pricing decision now clears its focus parameter, so returning to Prices opens the current workspace.

`scripts/social-experience-e2e.cjs` passed against real local APIs: ORG verification in the UI, owner approval, acceptance conditions, risk → draft → reserve → logistics → document → actual educational handover (12→9) → CSV report; public reservation ignores a duplicate click and does not prematurely consume stock. Explicit 503/retry, RU/EN and 390/768/1440 in both themes were checked. Test stock was closed and the test organization's acceptance disabled; history was retained. No external AI was called by this test.

## Audit / remaining work

- WORKS: existing stock/order core, price constraints, persisted real AI decisions, manual approval/rejection/rollback. Previously checked storefront/cart/B2B scope are recorded in `docs/ARIA3.md`.
- MISLEADING, fixed in Prices: every historical recommendation shown as a separate current card; “AI completed” even when the provider did not decide; zero-sale KEEP presented as awaiting a price change; duplicated refresh action.
- NEXT: My Stores cards, connection wizard and safe test-connection archival. No test connections have been deleted.
- NOT COMPLETE: full application RU/EN localization, administrative tool-using assistant, category-specific product schemas and an installed external WooCommerce E2E. Existing HTTP fixture tests do not constitute a real WooCommerce connection.

The university self-audit is a separate earlier snapshot in `docs/rubezh2`; no peer-group review is claimed.
