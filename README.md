# HerbWise — OSRS Herblore Profit & Training Assistant

**HerbWise** is a complete Old School RuneScape (OSRS) RuneLite plugin designed to help players make data-driven Herblore training and money-making decisions using real-time Grand Exchange prices and verified OSRS Wiki recipe data.

---

## Key Features

### 1. Real-Time OSRS Wiki Prices & Resilient Caching
- Integrates with the official **OSRS Wiki Real-Time Prices API** (`/latest`, `/mapping`, `/5m`, `/1h`, `/timeseries`), using `https://prices.runescape.wiki/api/v2/osrs` with automatic endpoint resolution to `https://prices.runescape.wiki/api/v1/osrs`.
- Sends a descriptive `User-Agent` header (`HerbWise-RuneLite-Plugin/1.0.0`) on every request.
- Uses bulk endpoints rather than per-item polling, with configurable background refresh intervals, manual refresh button, last-updated timestamps, connection status indicator (`LIVE API`, `CACHED DATA`, `REFRESHING`, `API OFFLINE`), exponential retry backoff, and local disk/snapshot price fallback (`~/.runelite/herbwise/price-cache.json`).
- All network calls execute on a dedicated daemon worker thread (`HerbWise-PriceUpdater`) so the RuneLite client thread never freezes.

### 2. Complete Verified Potion Database (115+ Recipes Across 33+ Families)
- Supports all standard and advanced OSRS Herblore potion families:
  - **Standard Potions**: Attack, Antipoison, Strength, Serum 207, Guthix rest, Compost, Restore, Energy, Defence, Agility, Combat, Prayer, Super attack, Superantipoison, Fishing, Super energy, Hunter, Goading, Super strength, Prayer regeneration,Super restore, Sanfew serum, Super defence, Ranging, Magic, Stamina, Zamorak brew, Saradomin brew, Surge.
  - **Weapon Poisons & Antivenoms**: Weapon poison, Weapon poison(+), Weapon poison(++), Antidote+, Antidote++, Anti-venom, Anti-venom+, Extended anti-venom+.
  - **Antifires**: Antifire potion, Extended antifire, Super antifire, Extended super antifire.
  - **High-Tier & Boss Potions**: Bastion potion, Battlemage potion, Ancient brew, Menaphite remedy, Super combat potion, Forgotten brew.
  - **Divine Potions (Prifddinas Crystal Dust)**: Divine super attack, Divine super strength, Divine super defence, Divine ranging, Divine magic, Divine bastion, Divine battlemage, Divine super combat.
  - **Varlamore & Special Potions**: Moonlight potion.
  - **Unfinished Potions**: Guam through Torstol and Huasca unfinished potions (explicitly categorised under `RecipeCategory.UNFINISHED_POTION` with `0 XP` and excluded from completed potion training recommendations).
- Treats **4-dose, 3-dose, 2-dose, and 1-dose** variants as distinct tradable `ItemID`s with separate market prices.
- Includes verified alternative preparation paths (`From Clean Herb + Vial of Water` vs. `From Unfinished Potion`).

### 3. Dedicated Profit, GE Tax & XP Efficiency Engine
- **4 Price Calculation Modes**:
  1. `Instant Buy/Sell (Conservative)` — Buys ingredients at instant-buy (`high`) and sells potions at instant-sell (`low`).
  2. `5-Minute Average` — Uses 5-minute volume-weighted averages (`avgHighPrice5m` / `avgLowPrice5m`).
  3. `1-Hour Average` — Uses 1-hour volume-weighted averages (`avgHighPrice1h` / `avgLowPrice1h`).
  4. `Patient GE Offers` — Buys ingredients via patient bids (`low`) and sells potions via patient asks (`high`).
- **Accurate OSRS Grand Exchange Tax (`GeTaxCalculator`)**:
  - 2% tax rounded down per individual item sold (`floor(salePrice * 0.02)`).
  - Items sold for under **50 GP** are tax-exempt (`0 GP` tax).
  - Capped at **5,000,000 GP** tax per item (at `>= 250,000,000 GP`).
  - Respects explicit tax-exempt items (`GeTaxRules`) and **never charges tax on ingredient purchases**.
- **Comprehensive Metrics**:
  - Ingredient Cost, Gross Sale Value, GE Tax, Net Sale Value, Profit/Loss per 1 / 100 / 1,000 / 10,000 potions, Profit Margin %, XP per potion, Ingredient Cost per XP, Net Cost/Loss per XP, Profit per XP, Potions per hour, XP per hour, and Net Profit/Loss per hour.

### 4. Six Integrated Sidebar Tabs + Recipe Detail Inspector
1. **Dashboard (`HerbWiseDashboard`)**:
   - **Player Herblore Integration**: Automatically reads current Herblore level & XP from the active RuneLite client (`Client.getRealSkillLevel(Skill.HERBLORE)` / `Client.getSkillExperience(Skill.HERBLORE)`), showing XP remaining to next level, progress bar, unlocked recipe count, and levels until next unlock.
   - **Top 4 Summary Cards**: `Best Profit Per Potion`, `Cheapest XP`, `Best XP Per GP`, and `Total Available Recipes`.
   - **Smart Training Recommendation Engine**: Evaluates 6 distinct categories (`Best Profit`, `Cheapest XP`, `Best XP Rate`, `Best Balanced Option`, `Best For My Budget`, `Best For My Level`) with transparent `0–100` scoring and dynamic `"Why?"` bullet points.
   - **Main Recommendations Table**: 10 sortable columns (`Icon`, `Potion Name`, `Lvl`, `XP`, `Ingr. Cost`, `Sale Price`, `GE Tax`, `Profit/Loss`, `Cost/XP`, `Margin`), semantic color coding (Green = Profitable, Red = Loss, Yellow = Uncertain/Stale, Grey = Missing/Locked), filters, and instant search.
2. **Potion Database (`HerbWisePotionTable` + `HerbWiseRecipeDetailPanel`)**:
   - Full recipe browser including 1–4 dose variants and alternative recipes. Clicking any row opens the **Recipe Detail View** with per-ingredient unit/total costs, clean herb vs. unfinished potion comparison, batch profit table (1 / 100 / 1,000 / 10,000), hourly XP & profit rates, and an interactive Wiki link.
3. **Budget Calculator (`HerbWiseBudgetPanel`)**:
   - Enter any GP budget or click presets (`100K`, `500K`, `1M`, `5M`, `10M`, `25M`) to calculate maximum potions affordable, total ingredient cost, expected sale value, GE tax, net profit/loss, total XP gained, remaining GP, inventory loads (`14/inv`), and full ingredient shopping list.
   - Includes a **Target XP & Level Quantity Planner** (`"Want 10,000 XP"` or `"Reach Level 90"`).
4. **XP & Production Calculator (`HerbWiseXpCalculator`)**:
   - Uses the official OSRS experience table (`HerbloreXpTable`) with target level presets (`70, 75, 80, 85, 90, 95, 99`) or custom potion quantity to compute exact potions needed, ingredient shopping list, total investment, GE tax, total profit/loss, GP/XP, inventory loads, and crafting time.
5. **Market Analysis (`HerbWiseMarketPanel`)**:
   - Displays current buy/sell prices, 5m/1h averages, historical `/timeseries` price & volume chart, trend direction (`Rising / Becoming More Expensive`, `Falling / Becoming Cheaper`, `Stable`), price movement, 1-hour volume, bid-ask spread, volatility/thin-trade warnings, and price age.
6. **Settings (`HerbWiseSettingsPanel`)**:
   - Configures refresh interval, price calculation mode (with live explanation), filter rules, GE tax toggle, volatility/stale warnings, default budget, default target level, Herblore boost mode (`None`, `Botanical Pie +4`, `Greenman's Ale +1`, `Mature +2`, `Spicy Stew +5`), table display mode (`Detailed` / `Compact`), and notifications.

---

## Building, Testing & Running

### Run All Unit Tests & Package Plugin JAR
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 verify
```

### Launch Full RuneLite Client with HerbWise Loaded
```powershell
.\run-herbwise.bat
```

### Launch Standalone HerbWise Sidebar Preview
```powershell
.\run-preview.bat
```
