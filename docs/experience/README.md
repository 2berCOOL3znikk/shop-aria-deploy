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
- NOT COMPLETE: full application RU/EN localization, administrative tool-using assistant, category-specific product schemas, self-service company provisioning and an installed external WooCommerce E2E with external-catalogue AI/writeback. Existing HTTP fixture tests do not constitute a real WooCommerce connection.

The university self-audit is a separate earlier snapshot in `docs/rubezh2`; no peer-group review is claimed.

## 3. My Stores — completed owner workflow

Open **http://127.0.0.1:8084/control.html#stores**.

- One Open action per card; catalogue, orders, history, connection repair, access and notifications are inside the store. The duplicate heading is gone. Current / Test / Archive views and search separate the owner's working list from training connections.
- The six-step wizard persists each completed step and resumes from the saved connection state. Access errors stop the process visibly. External-catalogue AI remains explicitly unavailable at step 6; it is not a fictional connected AI feature.
- V29 adds explicit `test_connection`, `archived` and `archived_at` only. Existing connections default to their original classification. Older three-field Create requests still work (the new flag is nullable/optional).
- Archive cancels queued/claimed work, revokes webhook admission and sets a backward-compatible disconnected state. Encrypted read access, catalogue/order snapshots and history remain. Restore requires another access check and enables neither writes nor webhooks.
- Permanent removal requires an explicitly marked archived empty WooCommerce test connection, confirmation, current version and no products/orders/sync history/tasks/receipts/access. Dependencies are rechecked under the store lock. The deletion audit remains.
- Only nine strictly recognized historical loopback QA connections were renamed/marked/archived by an opt-in script; none were deleted. A separate newly created empty test connection was removed in the browser to verify the guarded flow. Other test fixtures remain archived with their history.
- `stores-e2e.cjs` retains the previous real demo sync, catalogue, order failure, signed local webhook/deduplication/secret-clearing and role checks, adapted to the new UI; it adds actual archive/restore and guarded removal. `stores-readonly-e2e.cjs` verifies stable 390/768/1440 light/dark views, wizard resume to the real AI center and explicit 503/retry without connection writes.

## Final verification

- **208 Java tests passed**, zero failures/errors/skips, against a new local PostgreSQL database with Flyway V1–V29 and Hibernate validate. `scripts/test-postgres.ps1` always creates a fresh database: HTTP integration fixtures persist their requests, so reusing an old test database is invalid. Windows PowerShell warnings on native stderr are logged; Maven's exit code decides the result.
- V28→V29 was also tested on the earlier isolated database. Two separate app/database pairs with restricted logins, identical owner email/signing secret and different company bindings passed isolation, including foreign-token store reads/removal checks/archive denial.
- All **23 existing sections** remain reachable at desktop/mobile widths. The storefront's previously saved real product/order/stock persisted through the new application restart.
- A second actual free OpenRouter request was made through the UI on active educational SKU **ARIA-DEMO-016**, decision **257**: validated **KEEP**, model `dots-studio/dots-3-note-preview:free`. It remains visible in the main Prices list and Control Center. Verification reused that persisted result; no further model call or consent/quota reset was needed. Price/stock and owner permissions stayed unchanged. Request/verification evidence is separate.
- `scripts/verify-preview-restore.ps1` restored a restricted local dump into a fresh database in one transaction. V29, product/order/store/AI counts, demo stock and inactive archived queues matched. This verifies the local preview, not a production backup.
- Public staging was not provisioned. No main/production/paid-resource changes were made. Secrets and backups remain outside Git.

Owner instructions with screenshots: **`OWNER_GUIDE_RU.md`**. Publication steps requiring later explicit authorization: **`PRODUCTION_PLAN_RU.md`**. Next concrete development step: **`NEXT_STEPS_RU.md`**.
