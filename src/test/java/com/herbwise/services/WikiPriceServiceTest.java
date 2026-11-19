package com.herbwise.services;

import com.google.gson.Gson;
import com.herbwise.HerbWiseConfig.HerbloreBoostMode;
import com.herbwise.HerbWiseConfig.TableDisplayMode;
import com.herbwise.HerbWisePanel;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.MarketTrendAnalysis;
import com.herbwise.models.MarketTrendAnalysis.TimeseriesPoint;
import com.herbwise.models.MarketTrendAnalysis.TrendDirection;
import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.PriceCalculationMode;
import com.herbwise.models.TrainingRecommendation;
import com.herbwise.models.TrainingRecommendation.Category;
import com.herbwise.services.HerbloreDataService.ConnectionState;
import com.herbwise.ui.HerbWiseTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import net.runelite.api.ItemID;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit and integration tests for {@link WikiPriceService}, {@link PriceCacheService},
 * {@link HerbloreDataService}, UI layout verification, Plugin Hub compliance, and all 15 mandatory
 * API traffic, caching, deduplication, rate-limiting, and circuit-breaker scenarios.
 */
public class WikiPriceServiceTest
{
	private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

	private static final String SAMPLE_LATEST_JSON = "{\"data\":{"
		+ "\"2434\":{\"high\":9320,\"highTime\":1760000100,\"low\":9240,\"lowTime\":1760000090},"
		+ "\"139\":{\"high\":7010,\"highTime\":1760000105,\"low\":6940,\"lowTime\":1760000095},"
		+ "\"99\":{\"high\":6510,\"highTime\":1760000110,\"low\":6440,\"lowTime\":1760000100},"
		+ "\"257\":{\"high\":6450,\"highTime\":1760000110,\"low\":6390,\"lowTime\":1760000100},"
		+ "\"231\":{\"high\":480,\"highTime\":1760000110,\"low\":450,\"lowTime\":1760000100},"
		+ "\"227\":{\"high\":5,\"highTime\":1760000110,\"low\":4,\"lowTime\":1760000100}"
		+ "}}";

	private static final String SAMPLE_5M_JSON = "{\"data\":{"
		+ "\"2434\":{\"avgHighPrice\":9310,\"highPriceVolume\":420,\"avgLowPrice\":9250,\"lowPriceVolume\":390},"
		+ "\"139\":{\"avgHighPrice\":7000,\"highPriceVolume\":310,\"avgLowPrice\":6950,\"lowPriceVolume\":280}"
		+ "}}";

	private static final String SAMPLE_1H_JSON = "{\"data\":{"
		+ "\"2434\":{\"avgHighPrice\":9295,\"highPriceVolume\":3100,\"avgLowPrice\":9235,\"lowPriceVolume\":2950},"
		+ "\"139\":{\"avgHighPrice\":6990,\"highPriceVolume\":2400,\"avgLowPrice\":6935,\"lowPriceVolume\":2150}"
		+ "}}";

	private static final String SAMPLE_MAPPING_JSON = "["
		+ "{\"id\":2434,\"name\":\"Prayer potion(4)\",\"limit\":2000},"
		+ "{\"id\":139,\"name\":\"Prayer potion(3)\",\"limit\":2000},"
		+ "{\"id\":99,\"name\":\"Ranarr potion (unf)\",\"limit\":10000}"
		+ "]";

	private static final String SAMPLE_TIMESERIES_JSON = "{\"data\":["
		+ "{\"timestamp\":1760000000,\"avgHighPrice\":9000,\"avgLowPrice\":8900,\"highPriceVolume\":200,\"lowPriceVolume\":180},"
		+ "{\"timestamp\":1760000300,\"avgHighPrice\":9150,\"avgLowPrice\":9050,\"highPriceVolume\":250,\"lowPriceVolume\":210},"
		+ "{\"timestamp\":1760000600,\"avgHighPrice\":9350,\"avgLowPrice\":9250,\"highPriceVolume\":300,\"lowPriceVolume\":280}"
		+ "]}";

	private AtomicLong simulatedClockMs;
	private AtomicInteger mockHttpCallCount;
	private PriceCacheService priceCacheService;
	private WikiPriceService wikiPriceService;
	private File tempCacheFile;

	@Before
	public void setUp() throws Exception
	{
		PriceCacheService.clearSessionPersistentCache();
		simulatedClockMs = new AtomicLong(1_760_000_200_000L);
		mockHttpCallCount = new AtomicInteger(0);
		tempCacheFile = File.createTempFile("herbwise-test-cache-", ".properties");
		tempCacheFile.deleteOnExit();

		priceCacheService = new PriceCacheService(tempCacheFile);
		OkHttpClient mockClient = buildMockClient((request, count) -> {
			String url = request.url().toString();
			if (url.contains("/latest"))
			{
				return buildJsonResponse(request, 200, SAMPLE_LATEST_JSON);
			}
			if (url.contains("/5m"))
			{
				return buildJsonResponse(request, 200, SAMPLE_5M_JSON);
			}
			if (url.contains("/1h"))
			{
				return buildJsonResponse(request, 200, SAMPLE_1H_JSON);
			}
			if (url.contains("/mapping"))
			{
				return buildJsonResponse(request, 200, SAMPLE_MAPPING_JSON);
			}
			if (url.contains("/timeseries"))
			{
				return buildJsonResponse(request, 200, SAMPLE_TIMESERIES_JSON);
			}
			return buildJsonResponse(request, 404, "{\"error\":\"not found\"}");
		});

		wikiPriceService = new WikiPriceService(mockClient, new Gson(), priceCacheService);
		wikiPriceService.setClockMillis(simulatedClockMs::get);
	}

	@After
	public void tearDown()
	{
		PriceCacheService.clearSessionPersistentCache();
		if (tempCacheFile != null && tempCacheFile.exists())
		{
			tempCacheFile.delete();
		}
	}

	private interface MockHandler
	{
		Response handle(Request request, int callNumber) throws IOException;
	}

	private OkHttpClient buildMockClient(MockHandler handler)
	{
		Interceptor interceptor = chain -> {
			int callNum = mockHttpCallCount.incrementAndGet();
			return handler.handle(chain.request(), callNum);
		};
		return new OkHttpClient.Builder()
			.addInterceptor(interceptor)
			.build();
	}

	private static Response buildJsonResponse(Request request, int code, String body)
	{
		return new Response.Builder()
			.request(request)
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message(code == 200 ? "OK" : "HTTP " + code)
			.body(ResponseBody.create(JSON_MEDIA, body))
			.build();
	}

	@Test
	public void testParseAndMergeLatestMapping5mAnd1hPayloads() throws Exception
	{
		Map<Integer, ItemPrice> mapping = wikiPriceService.parseMappingJson(SAMPLE_MAPPING_JSON);
		Assert.assertEquals(3, mapping.size());

		Map<Integer, ItemPrice> merged = wikiPriceService.parseLatestJson(SAMPLE_LATEST_JSON, 1760000120L);
		wikiPriceService.merge5mJson(merged, SAMPLE_5M_JSON, 1760000120L);
		wikiPriceService.merge1hJson(merged, SAMPLE_1H_JSON, 1760000120L);

		ItemPrice pray4 = merged.get(ItemID.PRAYER_POTION4);
		Assert.assertNotNull(pray4);
		Assert.assertEquals("Prayer potion(4)", pray4.getItemName());
		Assert.assertEquals(Long.valueOf(9_320L), pray4.getInstantBuyPrice());
		Assert.assertEquals(Long.valueOf(9_240L), pray4.getInstantSellPrice());
		Assert.assertEquals(Long.valueOf(9_310L), pray4.getAvgHighPrice5m());
		Assert.assertEquals(Long.valueOf(9_250L), pray4.getAvgLowPrice5m());
		Assert.assertEquals(810, pray4.getTotalVolume5m());
		Assert.assertEquals(Long.valueOf(9_295L), pray4.getAvgHighPrice1h());
		Assert.assertEquals(Long.valueOf(9_235L), pray4.getAvgLowPrice1h());
		Assert.assertEquals(6_050, pray4.getTotalVolume1h());
		Assert.assertEquals(2000, pray4.getBuyLimit());
		Assert.assertFalse(pray4.isFromCache());
	}

	@Test
	public void testParseTimeseriesPayloadAndTrendDetection() throws Exception
	{
		List<TimeseriesPoint> points = wikiPriceService.parseTimeseriesJson(SAMPLE_TIMESERIES_JSON);
		MarketTrendAnalysis rising = wikiPriceService.analyzeTimeseries(
			ItemID.PRAYER_POTION4, "Prayer potion(4)", "5m", points);

		Assert.assertEquals(3, rising.getPoints().size());
		Assert.assertEquals(TrendDirection.RISING, rising.getDirection());
		Assert.assertTrue(rising.getPriceChangeGp() > 0L);
		Assert.assertTrue(rising.getPriceChangePercent() > 2.0);
		Assert.assertEquals(1_420L, rising.getTotalVolumeInWindow());
	}

	/**
	 * TEST 1: 100 recipes request overlapping ingredient prices.
	 * Expected: One underlying request per unique item batch (/latest) within the cache period.
	 */
	@Test
	public void test1_HundredRecipesOverlappingIngredientsTriggerSingleUnderlyingRequest() throws Exception
	{
		WikiRecipeService recipeService = new WikiRecipeService();
		List<PotionRecipe> allRecipes = recipeService.getAllVerifiedRecipes();
		Assert.assertTrue("Must have at least 100 verified recipes", allRecipes.size() >= 100);

		List<PotionRecipe> hundredRecipes = allRecipes.subList(0, 100);
		for (PotionRecipe recipe : hundredRecipes)
		{
			wikiPriceService.getOrFetchItemPrice(recipe.getPotionItemId());
			for (PotionIngredient ing : recipe.getIngredients())
			{
				if (ing.isTradeable())
				{
					wikiPriceService.getOrFetchItemPrice(ing.getItemId());
				}
			}
		}

		Assert.assertEquals("100 recipes requesting overlapping ingredients must trigger exactly 1 bulk /latest request",
			1, mockHttpCallCount.get());
		Assert.assertEquals(1L, wikiPriceService.getDiagnostics().getTotalApiRequests());
		Assert.assertTrue("Subsequent lookups must be served from cache hits",
			wikiPriceService.getDiagnostics().getCacheHits() >= 99L);
	}

	/**
	 * TEST 2: Multiple UI components request the same price simultaneously.
	 * Expected: One shared request.
	 */
	@Test
	public void test2_MultipleUiComponentsSimultaneousRequestsShareSingleRequest() throws Exception
	{
		CountDownLatch serverEntered = new CountDownLatch(1);
		CountDownLatch releaseServer = new CountDownLatch(1);
		AtomicInteger actualNetworkCalls = new AtomicInteger(0);

		OkHttpClient slowMockClient = buildMockClient((request, count) -> {
			actualNetworkCalls.incrementAndGet();
			serverEntered.countDown();
			try
			{
				releaseServer.await(2, TimeUnit.SECONDS);
			}
			catch (InterruptedException ignored)
			{
			}
			return buildJsonResponse(request, 200, SAMPLE_LATEST_JSON);
		});

		WikiPriceService concurrentService = new WikiPriceService(slowMockClient, new Gson(), priceCacheService);
		concurrentService.setClockMillis(simulatedClockMs::get);

		int threadCount = 8;
		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startGate = new CountDownLatch(1);
		List<Future<ItemPrice>> futures = new ArrayList<>();

		for (int i = 0; i < threadCount; i++)
		{
			futures.add(pool.submit(() -> {
				startGate.await(2, TimeUnit.SECONDS);
				return concurrentService.getOrFetchItemPrice(ItemID.PRAYER_POTION4);
			}));
		}

		startGate.countDown();
		Assert.assertTrue(serverEntered.await(2, TimeUnit.SECONDS));
		releaseServer.countDown();

		for (Future<ItemPrice> f : futures)
		{
			ItemPrice p = f.get(2, TimeUnit.SECONDS);
			Assert.assertNotNull(p);
			Assert.assertEquals(Long.valueOf(9_320L), p.getInstantBuyPrice());
		}
		pool.shutdownNow();

		Assert.assertEquals("All 8 simultaneous UI requests must share 1 single network call",
			1, actualNetworkCalls.get());
		Assert.assertTrue("Deduplicated counter must record shared callers",
			concurrentService.getDiagnostics().getDeduplicatedRequests() >= 1L);
	}

	/**
	 * TEST 3: Plugin restarts.
	 * Expected: Previously cached prices load correctly without requiring a new HTTP request.
	 */
	@Test
	public void test3_PluginRestartLoadsPreviouslyCachedPrices() throws Exception
	{
		// Initial plugin session fetches live prices and persists them
		wikiPriceService.fetchAllHerblorePrices();
		Assert.assertEquals(1, mockHttpCallCount.get());

		// Simulate full plugin restart with a brand new PriceCacheService & HerbloreDataService reading from disk
		PriceCacheService.clearSessionPersistentCache();
		PriceCacheService restartedCache = new PriceCacheService(tempCacheFile);
		Assert.assertFalse("Restarted cache must load warm live prices rather than bundled fallback",
			restartedCache.isUsingFallbackSnapshot());

		ItemPrice restoredPrayer = restartedCache.getCachedPrice(ItemID.PRAYER_POTION4);
		Assert.assertNotNull("Prayer potion(4) must be restored from persistent cache on restart", restoredPrayer);
		Assert.assertEquals(Long.valueOf(9_320L), restoredPrayer.getInstantBuyPrice());
		Assert.assertTrue(restoredPrayer.isFromCache());

		WikiPriceService restartedWikiService = new WikiPriceService(
			buildMockClient((req, c) -> buildJsonResponse(req, 200, SAMPLE_LATEST_JSON)),
			new Gson(),
			restartedCache
		);
		restartedWikiService.setClockMillis(simulatedClockMs::get);

		HerbloreDataService restartedDataService = new HerbloreDataService(
			restartedWikiService, new WikiRecipeService(), restartedCache, new PlayerSkillService(),
			null, null, null, null, null, null, null, null, null
		);

		// Because the restored cache is still within its TTL window, refreshPricesNow() makes 0 new HTTP calls
		restartedDataService.refreshPricesNow();
		Assert.assertEquals("Warm cache after plugin restart must not trigger an immediate duplicate HTTP request",
			1, mockHttpCallCount.get());
	}

	/**
	 * TEST 4: HTTP 429 response.
	 * Expected: Retry-After is respected and cached data remains available.
	 */
	@Test
	public void test4_Http429RespectsRetryAfterAndPreservesCachedData() throws Exception
	{
		// Seed cache with valid live data first
		wikiPriceService.fetchAllHerblorePrices();
		Assert.assertEquals(1, mockHttpCallCount.get());

		// Advance clock past 60s TTL so next fetch hits the server, which returns HTTP 429 with Retry-After: 90
		simulatedClockMs.addAndGet(65_000L);
		AtomicInteger rateLimitServerCalls = new AtomicInteger(0);

		OkHttpClient client429 = buildMockClient((request, count) -> {
			int n = rateLimitServerCalls.incrementAndGet();
			if (n == 1)
			{
				return new Response.Builder()
					.request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(429)
					.message("Too Many Requests")
					.header("Retry-After", "90")
					.body(ResponseBody.create(JSON_MEDIA, "{\"error\":\"rate limit\"}"))
					.build();
			}
			return buildJsonResponse(request, 200, SAMPLE_LATEST_JSON);
		});

		WikiPriceService service429 = new WikiPriceService(client429, new Gson(), priceCacheService);
		service429.setClockMillis(simulatedClockMs::get);

		HerbloreDataService dataService = new HerbloreDataService(
			service429, new WikiRecipeService(), priceCacheService, new PlayerSkillService(),
			null, null, null, null, null, null, null, null, null
		);

		dataService.refreshPricesNow(true);
		Assert.assertEquals(1, rateLimitServerCalls.get());
		Assert.assertTrue(service429.isRateLimited());
		Assert.assertEquals(90L, service429.getRemainingCooldownSeconds());
		Assert.assertEquals(1L, service429.getDiagnostics().getHttp429Responses());
		Assert.assertEquals(ConnectionState.RATE_LIMITED, dataService.getConnectionState());

		// Verify previously cached live price (9,320 GP) is preserved and NOT wiped
		ItemPrice preserved = dataService.getItemPrice(ItemID.PRAYER_POTION4);
		Assert.assertNotNull(preserved);
		Assert.assertEquals(Long.valueOf(9_320L), preserved.getInstantBuyPrice());

		// Any attempt during the 90-second Retry-After window must be blocked locally without hitting the server
		simulatedClockMs.addAndGet(30_000L);
		dataService.refreshPricesNow(true);
		Assert.assertEquals("No HTTP request may be sent before Retry-After expires",
			1, rateLimitServerCalls.get());

		// Advance past 90-second Retry-After window -> request succeeds
		simulatedClockMs.addAndGet(65_000L);
		dataService.refreshPricesNow(true);
		Assert.assertEquals(2, rateLimitServerCalls.get());
		Assert.assertFalse(service429.isRateLimited());
		Assert.assertEquals(ConnectionState.CONNECTED, dataService.getConnectionState());
	}

	/**
	 * TEST 5: Wiki API unavailable.
	 * Expected: No crash and offline calculations still work.
	 */
	@Test
	public void test5_WikiApiUnavailableOfflineCalculationsStillWork()
	{
		OkHttpClient offlineClient = buildMockClient((request, count) -> {
			throw new IOException("Network unreachable");
		});
		PriceCacheService freshCache = new PriceCacheService(null);
		WikiPriceService offlineService = new WikiPriceService(offlineClient, new Gson(), freshCache);
		offlineService.setClockMillis(simulatedClockMs::get);

		HerbloreDataService dataService = new HerbloreDataService(
			offlineService, new WikiRecipeService(), freshCache, new PlayerSkillService(),
			null, null, null, null, null, null, null, null, null
		);

		dataService.refreshPricesNow(true);
		Assert.assertEquals(ConnectionState.OFFLINE, dataService.getConnectionState());
		Assert.assertFalse("Evaluated recipe results must never be empty when API is offline",
			dataService.getAllEvaluatedResults().isEmpty());
		Assert.assertEquals(6, dataService.getCurrentRecommendations().size());
	}

	/**
	 * TEST 6: User changes tabs repeatedly.
	 * Expected: No unnecessary API requests.
	 */
	@Test
	public void test6_RepeatedTabSwitchingGeneratesZeroApiRequests() throws Exception
	{
		wikiPriceService.fetchAllHerblorePrices();
		int callsAfterStartup = mockHttpCallCount.get();
		Assert.assertEquals(1, callsAfterStartup);

		HerbloreDataService dataService = new HerbloreDataService(
			wikiPriceService, new WikiRecipeService(), priceCacheService, new PlayerSkillService(),
			null, null, null, null, null, null, null, null, null
		);
		HerbWisePanel panel = new HerbWisePanel(dataService);

		String[] tabs = new String[]{
			HerbWisePanel.TAB_DASHBOARD,
			HerbWisePanel.TAB_DATABASE,
			HerbWisePanel.TAB_BUDGET,
			HerbWisePanel.TAB_XP_CALC,
			HerbWisePanel.TAB_MARKET,
			HerbWisePanel.TAB_SETTINGS
		};

		for (int round = 0; round < 5; round++)
		{
			for (String tab : tabs)
			{
				panel.switchTab(tab);
			}
			panel.refreshAllTabs();
		}

		Assert.assertEquals("Switching across all 6 tabs repeatedly must generate 0 API requests",
			callsAfterStartup, mockHttpCallCount.get());
	}

	/**
	 * TEST 7: User changes refresh configuration.
	 * Expected: Only the central scheduler controls refresh behaviour (no direct request burst).
	 */
	@Test
	public void test7_ChangingRefreshConfigurationDoesNotTriggerImmediateApiRequests() throws Exception
	{
		wikiPriceService.fetchAllHerblorePrices();
		int callsBeforeConfigChanges = mockHttpCallCount.get();

		HerbloreDataService dataService = new HerbloreDataService(
			wikiPriceService, new WikiRecipeService(), priceCacheService, new PlayerSkillService(),
			null, null, null, null, null, null, null, null, null
		);

		for (int interval : new int[]{60, 90, 120, 180, 300})
		{
			dataService.applyRuntimeSettings(
				interval,
				PriceCalculationMode.INSTANT_CONSERVATIVE,
				false, false, true, true, true,
				2_000_000L, 90, true,
				TableDisplayMode.DETAILED,
				HerbloreBoostMode.NONE,
				false
			);
		}

		Assert.assertEquals("Changing refresh configuration must not trigger immediate HTTP requests",
			callsBeforeConfigChanges, mockHttpCallCount.get());
		Assert.assertEquals(300, dataService.getRefreshIntervalSeconds());
	}

	/**
	 * TEST 8: All 340+ recipes calculated.
	 * Expected: No individual API requests generated by calculations.
	 */
	@Test
	public void test8_CalculatingAll340PlusRecipesGeneratesZeroApiRequests()
	{
		HerbloreDataService dataService = new HerbloreDataService(
			wikiPriceService, new WikiRecipeService(), priceCacheService, new PlayerSkillService(),
			null, null, null, null, null, null, null, null, null
		);

		int beforeCalcCalls = mockHttpCallCount.get();
		dataService.recalculateAll(simulatedClockMs.get() / 1000L);
		List<PotionProfitResult> results = dataService.getAllEvaluatedResults();

		Assert.assertTrue("Must calculate all 340+ verified recipes", results.size() >= 340);
		Assert.assertEquals("Recipe calculation engine must generate 0 HTTP calls",
			beforeCalcCalls, mockHttpCallCount.get());
	}

	/**
	 * TEST 9: Plugin shuts down.
	 * Expected: Background tasks and scheduled requests are stopped safely.
	 */
	@Test
	public void test9_PluginShutdownStopsScheduledTasksSafely()
	{
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		try
		{
			HerbloreDataService dataService = new HerbloreDataService(
				wikiPriceService, new WikiRecipeService(), priceCacheService, new PlayerSkillService(),
				null, null, null, null, null, null, null, null, null
			);
			dataService.setExecutorService(scheduler);
			dataService.startUp();

			Assert.assertTrue(dataService.isPluginRunning());
			Assert.assertTrue(dataService.hasActiveScheduledTasks());

			dataService.shutDown();
			Assert.assertFalse(dataService.isPluginRunning());
			Assert.assertFalse("All scheduled futures must be cancelled on shutDown()",
				dataService.hasActiveScheduledTasks());
		}
		finally
		{
			scheduler.shutdownNow();
		}
	}

	/**
	 * TEST 10: Multiple simultaneous requests for expired data.
	 * Expected: One underlying request shared by all callers.
	 */
	@Test
	public void test10_MultipleSimultaneousRequestsForExpiredDataShareSingleRequest() throws Exception
	{
		// Initial fetch
		wikiPriceService.fetchAllHerblorePrices();
		Assert.assertEquals(1, mockHttpCallCount.get());

		// Expire the cache
		simulatedClockMs.addAndGet(120_000L);

		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch proceed = new CountDownLatch(1);
		AtomicInteger expiredFetchCalls = new AtomicInteger(0);

		OkHttpClient gatedClient = buildMockClient((request, count) -> {
			expiredFetchCalls.incrementAndGet();
			entered.countDown();
			try
			{
				proceed.await(2, TimeUnit.SECONDS);
			}
			catch (InterruptedException ignored)
			{
			}
			return buildJsonResponse(request, 200, SAMPLE_LATEST_JSON);
		});

		WikiPriceService service = new WikiPriceService(gatedClient, new Gson(), priceCacheService);
		service.setClockMillis(simulatedClockMs::get);

		ExecutorService pool = Executors.newFixedThreadPool(6);
		CountDownLatch startGate = new CountDownLatch(1);
		List<Future<Map<Integer, ItemPrice>>> futures = new ArrayList<>();

		for (int i = 0; i < 6; i++)
		{
			Callable<Map<Integer, ItemPrice>> task = () -> {
				startGate.await(2, TimeUnit.SECONDS);
				return service.fetchAllHerblorePrices(PriceCalculationMode.INSTANT_CONSERVATIVE, false);
			};
			futures.add(pool.submit(task));
		}

		startGate.countDown();
		Assert.assertTrue(entered.await(2, TimeUnit.SECONDS));
		proceed.countDown();

		for (Future<Map<Integer, ItemPrice>> f : futures)
		{
			Map<Integer, ItemPrice> res = f.get(2, TimeUnit.SECONDS);
			Assert.assertFalse(res.isEmpty());
		}
		pool.shutdownNow();

		Assert.assertEquals("6 concurrent callers on expired cache must share 1 single HTTP request",
			1, expiredFetchCalls.get());
	}

	/**
	 * TEST 11: Repeated failures.
	 * Expected: Circuit breaker prevents request storms.
	 */
	@Test
	public void test11_RepeatedFailuresTripCircuitBreakerAndPreventRequestStorms()
	{
		AtomicInteger serverHits = new AtomicInteger(0);
		OkHttpClient failingClient = buildMockClient((request, count) -> {
			serverHits.incrementAndGet();
			return buildJsonResponse(request, 503, "{\"error\":\"service unavailable\"}");
		});

		WikiPriceService cbService = new WikiPriceService(failingClient, new Gson(), priceCacheService);
		cbService.setClockMillis(simulatedClockMs::get);
		cbService.setCircuitBreakerConfig(3, 120_000L, 1_000L, 60_000L);

		// Trigger 3 consecutive failures (advancing simulated clock past individual backoff windows)
		for (int i = 0; i < 3; i++)
		{
			try
			{
				cbService.fetchAllHerblorePrices(PriceCalculationMode.INSTANT_CONSERVATIVE, true);
				Assert.fail("Expected IOException on HTTP 503");
			}
			catch (IOException expected)
			{
			}
			if (i < 2)
			{
				simulatedClockMs.addAndGet(10_000L);
			}
		}

		Assert.assertEquals(3, serverHits.get());
		Assert.assertTrue("Circuit breaker must open after 3 consecutive failures",
			cbService.isCircuitBreakerOpen());

		// Attempt 20 more requests while circuit breaker is open -> 0 additional server hits!
		for (int i = 0; i < 20; i++)
		{
			try
			{
				cbService.fetchAllHerblorePrices(PriceCalculationMode.INSTANT_CONSERVATIVE, true);
			}
			catch (IOException expected)
			{
			}
		}
		Assert.assertEquals("Circuit breaker must block all 20 requests during cooldown storm",
			3, serverHits.get());
	}

	/**
	 * TEST 12: Malformed or incomplete API response.
	 * Expected: Previous valid cached information is preserved.
	 */
	@Test
	public void test12_MalformedApiResponsePreservesPreviousValidCache() throws Exception
	{
		wikiPriceService.fetchAllHerblorePrices();
		ItemPrice validBefore = priceCacheService.getCachedPrice(ItemID.PRAYER_POTION4);
		Assert.assertNotNull(validBefore);
		Assert.assertEquals(Long.valueOf(9_320L), validBefore.getInstantBuyPrice());

		// Advance past TTL and return malformed JSON
		simulatedClockMs.addAndGet(120_000L);
		OkHttpClient malformedClient = buildMockClient((req, count) ->
			buildJsonResponse(req, 200, "{malformed_json_payload"));

		WikiPriceService brokenService = new WikiPriceService(malformedClient, new Gson(), priceCacheService);
		brokenService.setClockMillis(simulatedClockMs::get);

		HerbloreDataService dataService = new HerbloreDataService(
			brokenService, new WikiRecipeService(), priceCacheService, new PlayerSkillService(),
			null, null, null, null, null, null, null, null, null
		);

		dataService.refreshPricesNow(true);

		ItemPrice preservedAfter = dataService.getItemPrice(ItemID.PRAYER_POTION4);
		Assert.assertNotNull("Valid cached item must not be lost after malformed JSON response", preservedAfter);
		Assert.assertEquals(Long.valueOf(9_320L), preservedAfter.getInstantBuyPrice());
		Assert.assertEquals(ConnectionState.CACHED_DATA, dataService.getConnectionState());
	}

	/**
	 * TEST 13: Mapping cache.
	 * Expected: Mapping data is not downloaded repeatedly.
	 */
	@Test
	public void test13_MappingDataIsCachedAndNotDownloadedRepeatedly() throws Exception
	{
		wikiPriceService.fetchMappingIfNeeded();
		wikiPriceService.fetchMappingIfNeeded();
		wikiPriceService.fetchMappingIfNeeded();

		Assert.assertEquals("Mapping endpoint must be downloaded only once within its 24h TTL",
			1L, wikiPriceService.getEndpointRequestCount("/mapping"));
	}

	/**
	 * TEST 14: Historical price requests (/5m, /1h, /timeseries).
	 * Expected: Only requested when a feature needs historical data.
	 */
	@Test
	public void test14_HistoricalPriceEndpointsOnlyRequestedWhenFeatureNeedsThem() throws Exception
	{
		// Ordinary default refresh (INSTANT_CONSERVATIVE)
		wikiPriceService.fetchAllHerblorePrices(PriceCalculationMode.INSTANT_CONSERVATIVE, false);

		Assert.assertEquals(1L, wikiPriceService.getEndpointRequestCount("/latest"));
		Assert.assertEquals("Ordinary refresh must not download /5m",
			0L, wikiPriceService.getEndpointRequestCount("/5m"));
		Assert.assertEquals("Ordinary refresh must not download /1h",
			0L, wikiPriceService.getEndpointRequestCount("/1h"));
		Assert.assertEquals("Ordinary refresh must not download /timeseries",
			0L, wikiPriceService.getEndpointRequestCount("/timeseries"));

		// Now switch to FIVE_MINUTE_AVERAGE -> only /5m is fetched on demand
		wikiPriceService.fetchAllHerblorePrices(PriceCalculationMode.FIVE_MINUTE_AVERAGE, false);
		Assert.assertEquals(1L, wikiPriceService.getEndpointRequestCount("/5m"));
		Assert.assertEquals(0L, wikiPriceService.getEndpointRequestCount("/1h"));

		// Now switch to ONE_HOUR_AVERAGE -> only /1h is fetched on demand
		wikiPriceService.fetchAllHerblorePrices(PriceCalculationMode.ONE_HOUR_AVERAGE, false);
		Assert.assertEquals(1L, wikiPriceService.getEndpointRequestCount("/1h"));

		// Request timeseries twice for same item -> fetched once, second served from cache
		wikiPriceService.fetchTimeseries(ItemID.PRAYER_POTION4, "5m");
		wikiPriceService.fetchTimeseries(ItemID.PRAYER_POTION4, "5m");
		Assert.assertEquals(1L, wikiPriceService.getEndpointRequestCount("/timeseries"));
	}

	/**
	 * TEST 15: Cache expiration.
	 * Expected: Expired data is refreshed according to the central scheduler without duplicate requests.
	 */
	@Test
	public void test15_ExpiredCacheRefreshesOnceAndResetsTtl() throws Exception
	{
		priceCacheService.setLatestTtlSeconds(60L);

		// First fetch at T=0
		wikiPriceService.fetchAllHerblorePrices();
		Assert.assertEquals(1L, wikiPriceService.getEndpointRequestCount("/latest"));

		// At T=30s (still fresh) -> 0 new requests
		simulatedClockMs.addAndGet(30_000L);
		wikiPriceService.fetchAllHerblorePrices();
		Assert.assertEquals(1L, wikiPriceService.getEndpointRequestCount("/latest"));

		// At T=65s (expired) -> 1 refresh request
		simulatedClockMs.addAndGet(35_000L);
		wikiPriceService.fetchAllHerblorePrices();
		Assert.assertEquals(2L, wikiPriceService.getEndpointRequestCount("/latest"));

		// At T=80s (fresh again after T=65s refresh) -> 0 new requests
		simulatedClockMs.addAndGet(15_000L);
		wikiPriceService.fetchAllHerblorePrices();
		Assert.assertEquals(2L, wikiPriceService.getEndpointRequestCount("/latest"));
	}

	@Test
	public void testTrainingRecommendationsAndAllSixUiTabsLayoutWithoutOverlap() throws Exception
	{
		HerbloreDataService dataService = new HerbloreDataService(
			wikiPriceService,
			new WikiRecipeService(),
			priceCacheService,
			new PlayerSkillService(),
			null, null, null, null, null, null, null, null, null
		);

		Set<Integer> uniqueIds = dataService.getWikiRecipeService().getRequiredUniqueItemIds();
		Assert.assertFalse("Unique required item ID set must not be empty", uniqueIds.isEmpty());

		Map<Category, TrainingRecommendation> recs = dataService.getCurrentRecommendations();
		Assert.assertEquals(6, recs.size());
		for (Category cat : Category.values())
		{
			TrainingRecommendation rec = recs.get(cat);
			Assert.assertNotNull("Missing recommendation for " + cat, rec);
			Assert.assertNotNull("Null result for " + cat, rec.getResult());
			Assert.assertFalse("Empty reasons for " + cat, rec.getReasons().isEmpty());
			Assert.assertFalse("Unfinished potions must not be recommended",
				rec.getResult().getRecipe().isUnfinishedPotionRecipe());
		}

		HerbWisePanel panel = new HerbWisePanel(dataService);
		panel.setSize(new Dimension(580, 820));
		panel.doLayout();

		String[] tabs = new String[]{
			HerbWisePanel.TAB_DASHBOARD,
			HerbWisePanel.TAB_DATABASE,
			HerbWisePanel.TAB_BUDGET,
			HerbWisePanel.TAB_XP_CALC,
			HerbWisePanel.TAB_MARKET,
			HerbWisePanel.TAB_SETTINGS
		};

		for (String tab : tabs)
		{
			panel.switchTab(tab);
			panel.doLayout();
			assertNoSiblingOverlap(panel);
		}

		PotionProfitResult sampleResult = dataService.getAllEvaluatedResults().get(0);
		panel.openRecipeDetail(sampleResult);
		panel.doLayout();
		Assert.assertEquals(HerbWisePanel.TAB_RECIPE_DETAIL, panel.getCurrentActiveTab());
		assertNoSiblingOverlap(panel);
	}

	@Test
	public void testPluginHubComplianceAndIconAssets() throws Exception
	{
		File propFile = new File("runelite-plugin.properties");
		Assert.assertTrue("runelite-plugin.properties must exist", propFile.exists());
		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(propFile))
		{
			props.load(fis);
		}
		Assert.assertEquals("com.herbwise.HerbWisePlugin", props.getProperty("plugins"));

		BufferedImage toolbarIcon = HerbWiseTheme.createToolbarIcon();
		Assert.assertNotNull(toolbarIcon);
		Assert.assertEquals(16, toolbarIcon.getWidth());
		Assert.assertEquals(16, toolbarIcon.getHeight());

		File resIconFile = new File("src/main/resources/com/herbwise/icon.png");
		resIconFile.getParentFile().mkdirs();
		ImageIO.write(toolbarIcon, "png", resIconFile);
		Assert.assertTrue(resIconFile.exists());

		BufferedImage hubIcon = HerbWiseTheme.createPluginHubIcon();
		Assert.assertEquals(48, hubIcon.getWidth());
		Assert.assertEquals(72, hubIcon.getHeight());

		File rootIconFile = new File("icon.png");
		ImageIO.write(hubIcon, "png", rootIconFile);
		Assert.assertTrue(rootIconFile.exists());
	}

	private void assertNoSiblingOverlap(Container container)
	{
		container.doLayout();
		if (container instanceof javax.swing.JScrollPane)
		{
			javax.swing.JScrollPane sp = (javax.swing.JScrollPane) container;
			if (sp.getViewport() != null)
			{
				sp.getViewport().setSize(sp.getWidth() - 16, sp.getHeight());
				sp.getViewport().doLayout();
				Component view = sp.getViewport().getView();
				if (view instanceof Container)
				{
					Dimension pref = view.getPreferredSize();
					view.setSize(Math.max(200, sp.getViewport().getWidth()), Math.max(sp.getHeight(), pref.height));
					assertNoSiblingOverlap((Container) view);
				}
			}
			return;
		}

		Component[] children = container.getComponents();
		if (!(container.getLayout() instanceof java.awt.CardLayout))
		{
			for (int i = 0; i < children.length; i++)
			{
				Component a = children[i];
				if (!a.isVisible() || a.getWidth() <= 0 || a.getHeight() <= 0) continue;
				Rectangle ra = a.getBounds();
				for (int j = i + 1; j < children.length; j++)
				{
					Component b = children[j];
					if (!b.isVisible() || b.getWidth() <= 0 || b.getHeight() <= 0) continue;
					Rectangle rb = b.getBounds();
					Rectangle inter = ra.intersection(rb);
					if (inter.width > 2 && inter.height > 2)
					{
						Assert.fail("Overlapping sibling UI components in " + container.getClass().getSimpleName()
							+ ": " + a.getClass().getSimpleName() + " " + ra + " and "
							+ b.getClass().getSimpleName() + " " + rb);
					}
				}
			}
		}

		for (Component c : children)
		{
			if (c instanceof Container && c.isVisible())
			{
				assertNoSiblingOverlap((Container) c);
			}
		}
	}
}
