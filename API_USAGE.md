# HerbWise — OSRS Wiki API Architecture, Traffic Audit & Compliance (`API_USAGE.md`)

## 1. Original Excessive-Traffic Problem & Root Causes Discovered

Following the initial release of **HerbWise**, the plugin was disabled on the RuneLite Plugin Hub due to `"excessive wiki traffic"`. A full code audit identified four compounding root causes:

1. **Unconditional `onGameTick` / UI Refresh -> `/timeseries` Request Loop (Primary Traffic Multiplier)**:
   - `HerbWisePlugin.onGameTick()` called `PlayerSkillService.refreshFromClient()` every game tick (`0.6s`).
   - `PlayerSkillService.refreshFromClient()` unconditionally invoked `notifyListeners(...)` even when the player's Herblore level and XP had not changed.
   - `HerbloreDataService` listened to `PlayerSkillService` and invoked `notifyUiListeners()` -> `HerbWisePanel.refreshAllTabs()` -> `HerbWiseMarketPanel.refresh()`.
   - `HerbWiseMarketPanel.refresh()` unconditionally called `updateSelectedItemAnalysis()` -> `dataService.fetchTimeseriesAsync(...)`, firing an un-cached HTTP GET `/timeseries?timestep=5m&id=...` request every `0.6s` (~100 requests/minute per active logged-in player) even when the Market tab was hidden.
   - Additionally, `WikiPriceService.fetchTimeseries()` only checked `PriceCacheService.getCachedTrendAnalysis()` inside the `catch (IOException)` block rather than before issuing the HTTP request.
2. **Non-Existent `/api/v2/osrs` Primary Base URL Causing 100% 404 Fallback Doubling**:
   - `WikiPriceService` attempted `https://prices.runescape.wiki/api/v2/osrs` first on every request and fell back to `https://prices.runescape.wiki/api/v1/osrs` on `IOException`. Because the OSRS Wiki Prices API serves `/api/v1/osrs` (and returns HTTP `404` on `/api/v2/osrs`), every single endpoint fetch generated **two** HTTP requests (one guaranteed `404` + one `/api/v1/osrs` call).
3. **Eager Multi-Endpoint Polling on Every 60-Second Refresh**:
   - Every 60-second background poll in `fetchAllHerblorePrices()` downloaded `/latest`, `/5m`, and `/1h` simultaneously (plus `/mapping` on every client restart), even though default recipe calculations (`INSTANT_CONSERVATIVE`) only use `/latest` prices and local item definitions (`ItemIdMappings`), and even though `/5m` and `/1h` only update on 5-minute and 1-hour intervals.
4. **Lack of In-Flight Deduplication, Manual Refresh Cooldown, `429 Retry-After` Handling, and Circuit Breaker**:
   - Concurrent callers could trigger parallel identical requests; clicking the UI Refresh button or changing settings could trigger immediate refreshes; and HTTP `429` / `5xx` errors lacked `Retry-After` enforcement and a circuit breaker.

---

## 2. Central API Architecture & Endpoints Used

All OSRS Wiki API requests pass exclusively through the singleton `WikiPriceService`, coordinated by `HerbloreDataService` and `PriceCacheService`. No UI component or calculator makes direct HTTP requests.

- **Base URL**: `https://prices.runescape.wiki/api/v1/osrs`
- **User-Agent**: `HerbWise-RuneLite-Plugin/1.0.0 (+https://github.com/WizardOfAus666/herbwise; OSRS Herblore Profit & Training Assistant)`

| Endpoint | Purpose | Fetch Policy | Cache TTL |
| :--- | :--- | :--- | :--- |
| `/latest` | Bulk instant-buy (`high`) & instant-sell (`low`) prices for all tradeable items | Periodic background refresh (default `120s`) when cache is expired | Configurable (`120s` default, min `30s`) |
| `/5m` | Bulk 5-minute rolling average prices & volumes | **Lazy / On-Demand only** when `5-Minute Average` mode is selected or Market averages are requested | `300s` (5 minutes) |
| `/1h` | Bulk 1-hour rolling average prices & volumes | **Lazy / On-Demand only** when `1-Hour Average` mode is selected or Market averages are requested | `3600s` (1 hour) |
| `/mapping` | Item metadata (names, buy limits, alch values) | **On-Demand only** (local `ItemIdMappings` covers all Herblore items by default) | `86400s` (24 hours) |
| `/timeseries` | Historical price points for a single item in the Market tab | **Explicit user interaction only** on the Market tab when cache entry is expired | `300s` (5 minutes) per `(itemId, timestep)` |

---

## 3. Intelligent Caching & Stale-While-Revalidate (`PriceCacheService`)

- **Zero Requests During Recipe Calculations**: All 340+ Herblore recipes share a precomputed set of unique item IDs (`WikiRecipeService.getRequiredUniqueItemIds()`) and read exclusively from the in-memory `PriceCacheService` / `activePrices` map (`0` HTTP requests during `recalculateAll()`).
- **Stale-While-Revalidate Preservation**: Incoming `/latest`, `/5m`, and `/1h` payloads are merged into existing cached `ItemPrice` records rather than wiping previous data. If a refresh fails or returns malformed JSON, all previously valid cached prices remain intact and usable offline.
- **Cross-Restart Persistence**: Cached prices and tier timestamps are persisted across plugin stop/start cycles in memory and saved to `.runelite/herbwise/price-cache.properties` on disk so restarting the plugin or client immediately loads warm cached prices without forcing redundant downloads.

---

## 4. Request Deduplication, Rate Limiting & Circuit Breaker

- **Shared In-Flight Request Deduplication**: `WikiPriceService.executeDeduplicatedEndpoint()` synchronizes concurrent callers per endpoint path using `EndpointFlightState`. If multiple threads or UI components request the same expired endpoint simultaneously, only **1** HTTP call is made and all waiting callers share its result.
- **HTTP 429 & `Retry-After` Compliance**: If the API returns `HTTP 429 Too Many Requests`, `WikiPriceService` parses the `Retry-After` header (integer seconds or RFC-1123 HTTP date, defaulting to `60s`), enters `RATE_LIMITED` state, blocks all outbound requests until the window expires, preserves cached prices, and schedules a single delayed retry via RuneLite's injected `ScheduledExecutorService`.
- **Exponential Backoff with Jitter & Circuit Breaker**: Consecutive `5xx` (`500`, `502`, `503`, `504`) or network failures apply exponential backoff with random jitter (`base * 2^(n-1) + jitter`). After 3 consecutive failures, the circuit breaker opens for a `120s` cooldown window, blocking request storms while continuing offline calculations.
- **Manual Refresh Cooldown**: The UI Refresh button routes through `HerbloreDataService.manualRefreshAsync()`, enforcing a minimum `15s` cooldown when the cache is already fresh.

---

## 5. Running Automated Tests

All 44 unit and integration tests (including the 15 mandatory API traffic, deduplication, caching, rate-limiting, and circuit-breaker tests in `WikiPriceServiceTest`) use mocked `OkHttpClient` interceptors and generate **zero** real network traffic:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 clean verify
```
