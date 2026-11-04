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

## Audit / remaining work

- WORKS: existing stock/order core, price constraints, persisted real AI decisions, manual approval/rejection/rollback. Previously checked storefront/cart/B2B scope are recorded in `docs/ARIA3.md`.
- MISLEADING, fixed in Prices: every historical recommendation shown as a separate current card; “AI completed” even when the provider did not decide; zero-sale KEEP presented as awaiting a price change; duplicated refresh action.
- NEXT: Social/Impact process first, then My Stores cards, connection wizard and safe test-connection archival. No test connections have been deleted.
- NOT COMPLETE: full application RU/EN localization, administrative tool-using assistant, category-specific product schemas and an installed external WooCommerce E2E. Existing HTTP fixture tests do not constitute a real WooCommerce connection.

The university self-audit is a separate earlier snapshot in `docs/rubezh2`; no peer-group review is claimed.
