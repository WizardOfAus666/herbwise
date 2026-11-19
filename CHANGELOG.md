# Changelog

## [1.0.1] — API Traffic, Caching & Rate-Limit Repair

### Fixed
- **Eliminated `/timeseries` polling loop**: Fixed a bug where `onGameTick` -> `PlayerSkillService.refreshFromClient()` notified UI listeners unconditionally every 0.6s, causing `HerbWiseMarketPanel.refresh()` to trigger repeated `/timeseries` HTTP requests even when the Market tab was not active.
- **Fixed `/timeseries` cache bypass**: `WikiPriceService.fetchTimeseries()` now checks `PriceCacheService.isTrendCacheFresh()` (`300s` TTL) before making any network request.
- **Eliminated 100% 404 fallback request doubling**: Switched primary API base URL directly to the canonical OSRS Wiki Prices API v1 endpoint (`https://prices.runescape.wiki/api/v1/osrs`), removing the failing `/api/v2/osrs` pre-request that previously doubled HTTP traffic.
- **Fixed popout window listener leak**: Closing the standalone popout window now unregisters its UI listener from `HerbloreDataService`.

### Changed
- **Lazy on-demand `/5m`, `/1h`, and `/mapping` retrieval**: Ordinary background refreshes now fetch only `/latest` (1 request per refresh cycle instead of 4–8). `/5m` (`300s` TTL) and `/1h` (`3600s` TTL) are only fetched when `5-Minute Average` or `1-Hour Average` mode is active.
- **Increased default refresh interval**: Default background price refresh interval increased from `60s` to `120s`.
- **Event-driven skill updates**: `PlayerSkillService` now only notifies listeners when Herblore level, boosted level, XP, or login state actually changes, and `HerbWisePlugin.onStatChanged` filters specifically for `Skill.HERBLORE`.
- **Configuration change isolation**: Changing filter, budget, or refresh interval settings no longer triggers immediate HTTP requests.

### Added
- **Shared in-flight request deduplication**: Concurrent requests for the same endpoint share a single in-flight HTTP call.
- **HTTP 429 `Retry-After` handling, exponential backoff with jitter, and circuit breaker**: Respects `Retry-After` headers, applies jittered exponential backoff on `5xx`/network errors, and trips a `120s` circuit breaker after 3 consecutive failures.
- **Persistent disk + session price cache (`PriceCacheService`)**: Persists live prices across plugin restarts with stale-while-revalidate preservation on error.
- **Internal API Diagnostics & Compact Status UI**: Displays `CONNECTED`, `CACHED DATA`, `UPDATING`, `RATE LIMITED`, and `OFFLINE` states along with cache age, cached item count, request counters by endpoint, cache hits/misses, deduplicated requests, and HTTP 429 counts.
- **15 Mandatory Automated API Traffic Tests**: Added mocked HTTP interceptor test suite in `WikiPriceServiceTest` verifying deduplication, caching, rate limits, circuit breaker, tab switching, and offline fallback.

---

## [1.0.0] — Initial Release
- Complete verified OSRS Herblore recipe database (340+ recipes across 33+ potion families and 1–4 dose variants).
- Profit/loss and 2% Grand Exchange tax calculation engine with 5M cap and <50 GP exemption.
- 6-tab RuneLite sidebar UI (Dashboard, Potion Database, Budget Calculator, XP Planner, Market Analysis, Settings) and Recipe Detail view.
