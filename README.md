# HerbWise — OSRS Herblore Profit & Training Assistant

**HerbWise** helps Old School RuneScape players plan Herblore training and find profitable potion-making methods using real-time Grand Exchange prices from the OSRS Wiki and verified Herblore recipe data.

---

## Features

### 1. Dashboard & Smart Training Recommendations
- **Automatic Herblore Level Sync**: Detects your current Herblore level and XP when logged in, showing XP remaining to your next level, unlocked recipes, and your next potion unlock.
- **Quick Summary Cards**: Highlights the current **Best Profit Per Potion**, **Cheapest XP**, **Best XP Per GP**, and **Total Available Recipes**.
- **Smart Recommendations**: Switch between 6 training goals to find the best potion to make right now:
  - **Best Profit** — Highest net profit per potion after GE tax.
  - **Cheapest XP** — Lowest GP loss (or highest profit) per XP gained.
  - **Best XP Rate** — Fastest estimated Herblore XP per hour.
  - **Best Balanced** — Best overall balance of XP speed, GP/XP cost, and trade volume.
  - **Best For My Budget** — Best training method within your configured GP budget.
  - **Best For My Level** — Best potion currently unlocked at your Herblore level (with optional boost support).
- **Sortable & Filterable Potion Table**: Compare ingredient cost, sale price, GE tax, net profit/loss, GP/XP, and profit margin across all Herblore recipes with color-coded profitability indicators.

### 2. Complete Herblore Recipe Database & Inspector
- Covers standard potions, super potions, combat/ranging/magic/bastion/battlemage potions, brews, restores, antifires, antivenoms, divine potions, staminas, prayer regeneration, goading, surge, and unfinished potions across 1- to 4-dose variants.
- Click any potion row to open the **Recipe Breakdown**, showing:
  - Individual ingredient prices and quantities.
  - Cost comparison between making from a **clean herb + vial of water** vs. buying an **unfinished potion**.
  - Profit/loss and XP breakdown for batches of **1**, **100**, **1,000**, and **10,000** potions.
  - Direct link to the item on the OSRS Wiki.

### 3. Grand Exchange Tax & Price Modes
- Automatically accounts for the **2% Grand Exchange tax** on finished potion sales (including the <50 GP exemption and 5M GP tax cap), while never charging tax on ingredient purchases.
- Choose between 4 price calculation modes to match how you trade on the GE:
  - **Instant Buy / Sell (Conservative)** — Buys ingredients at instant-buy price and sells potions at instant-sell price.
  - **5-Minute Average** — Uses recent 5-minute average GE trade prices.
  - **1-Hour Average** — Uses 1-hour average GE trade prices.
  - **Patient GE Offers** — Buys ingredients via slow buy offers and sells potions via slow sell offers.

### 4. Budget Calculator
- Enter any GP budget (or choose quick presets like `100K`, `500K`, `1M`, `5M`, `10M`, `25M`) to calculate:
  - Maximum potions you can afford to make.
  - Complete ingredient shopping list and total cost.
  - Expected sale value, GE tax, net profit/loss, and remaining GP.
  - Total Herblore XP gained and number of bank inventory loads required.

### 5. Target XP & Level Planner
- Plan your training to a target Herblore level (e.g., Level 70, 80, 90, 99) or a custom XP goal:
  - Calculates exact number of potions and ingredients needed.
  - Estimates total GP required, net profit/loss after selling, inventory loads, and crafting time.

### 6. Market Trends & Liquidity Check
- Inspect 1-hour trade volume, bid-ask spread, price movement, and historical price charts for any potion or ingredient.
- Optional warnings for thinly traded or highly volatile items so you avoid getting stuck with illiquid potions.

---

## How to Use
1. Enable **HerbWise** in RuneLite and click the **HerbWise potion icon** in the client sidebar.
2. Log in to automatically sync your Herblore level (or use the offline level selector in the Dashboard header).
3. Use the **Refresh Prices** button at the top-right anytime to fetch the latest OSRS Wiki Grand Exchange prices, or adjust your preferred pricing mode and Herblore boost (+1 to +5) in the **Settings** tab.
