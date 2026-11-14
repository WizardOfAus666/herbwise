package com.herbwise.services;

import com.google.gson.Gson;
import com.herbwise.HerbWisePanel;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.MarketTrendAnalysis;
import com.herbwise.models.MarketTrendAnalysis.TimeseriesPoint;
import com.herbwise.models.MarketTrendAnalysis.TrendDirection;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.TrainingRecommendation;
import com.herbwise.models.TrainingRecommendation.Category;
import com.herbwise.ui.HerbWiseTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import net.runelite.api.ItemID;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link WikiPriceService}, {@link PriceCacheService},
 * {@link HerbloreDataService}, UI layout verification, and Plugin Hub packaging compliance.
 */
public class WikiPriceServiceTest
{
	private WikiPriceService wikiPriceService;
	private PriceCacheService priceCacheService;

	@Before
	public void setUp()
	{
		priceCacheService = new PriceCacheService();
		wikiPriceService = new WikiPriceService(new OkHttpClient(), new Gson(), priceCacheService);
	}

	@Test
	public void testParseAndMergeLatestMapping5mAnd1hPayloads() throws Exception
	{
		String mappingJson = "["
			+ "{\"id\":2434,\"name\":\"Prayer potion(4)\",\"limit\":2000},"
			+ "{\"id\":139,\"name\":\"Prayer potion(3)\",\"limit\":2000},"
			+ "{\"id\":99,\"name\":\"Ranarr potion (unf)\",\"limit\":10000}"
			+ "]";

		String latestJson = "{\"data\":{"
			+ "\"2434\":{\"high\":9320,\"highTime\":1760000100,\"low\":9240,\"lowTime\":1760000090},"
			+ "\"139\":{\"high\":7010,\"highTime\":1760000105,\"low\":6940,\"lowTime\":1760000095},"
			+ "\"99\":{\"high\":6510,\"highTime\":1760000110,\"low\":6440,\"lowTime\":1760000100}"
			+ "}}";

		String fiveMinJson = "{\"data\":{"
			+ "\"2434\":{\"avgHighPrice\":9310,\"highPriceVolume\":420,\"avgLowPrice\":9250,\"lowPriceVolume\":390},"
			+ "\"139\":{\"avgHighPrice\":7000,\"highPriceVolume\":310,\"avgLowPrice\":6950,\"lowPriceVolume\":280}"
			+ "}}";

		String oneHourJson = "{\"data\":{"
			+ "\"2434\":{\"avgHighPrice\":9295,\"highPriceVolume\":3100,\"avgLowPrice\":9235,\"lowPriceVolume\":2950},"
			+ "\"139\":{\"avgHighPrice\":6990,\"highPriceVolume\":2400,\"avgLowPrice\":6935,\"lowPriceVolume\":2150}"
			+ "}}";

		Map<Integer, ItemPrice> mapping = wikiPriceService.parseMappingJson(mappingJson);
		Assert.assertEquals(3, mapping.size());

		Map<Integer, ItemPrice> merged = wikiPriceService.parseLatestJson(latestJson, 1760000120L);
		wikiPriceService.merge5mJson(merged, fiveMinJson, 1760000120L);
		wikiPriceService.merge1hJson(merged, oneHourJson, 1760000120L);

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
		String risingTimeseriesJson = "{\"data\":["
			+ "{\"timestamp\":1760000000,\"avgHighPrice\":9000,\"avgLowPrice\":8900,\"highPriceVolume\":200,\"lowPriceVolume\":180},"
			+ "{\"timestamp\":1760000300,\"avgHighPrice\":9150,\"avgLowPrice\":9050,\"highPriceVolume\":250,\"lowPriceVolume\":210},"
			+ "{\"timestamp\":1760000600,\"avgHighPrice\":9350,\"avgLowPrice\":9250,\"highPriceVolume\":300,\"lowPriceVolume\":280}"
			+ "]}";

		List<TimeseriesPoint> points = wikiPriceService.parseTimeseriesJson(risingTimeseriesJson);
		MarketTrendAnalysis rising = wikiPriceService.analyzeTimeseries(
			ItemID.PRAYER_POTION4, "Prayer potion(4)", "5m", points);

		Assert.assertEquals(3, rising.getPoints().size());
		Assert.assertEquals(TrendDirection.RISING, rising.getDirection());
		Assert.assertTrue(rising.getPriceChangeGp() > 0L);
		Assert.assertTrue(rising.getPriceChangePercent() > 2.0);
		Assert.assertEquals(1_420L, rising.getTotalVolumeInWindow());
	}

	@Test
	public void testMalformedJsonThrowsDescriptiveExceptionAndCacheFallbackWorks()
	{
		try
		{
			wikiPriceService.parseLatestJson("{bad_json", 1760000000L);
			Assert.fail("Expected IOException on malformed JSON");
		}
		catch (IOException expected)
		{
			Assert.assertTrue(expected.getMessage().contains("/latest"));
		}

		Map<Integer, ItemPrice> fallback = priceCacheService.getOrLoadFallbackPrices();
		Assert.assertNotNull(fallback);
		Assert.assertFalse("Fallback cache must never be empty", fallback.isEmpty());
	}

	@Test
	public void testTrainingRecommendationsAndAllSixUiTabsLayoutWithoutOverlap() throws Exception
	{
		HerbloreDataService dataService = new HerbloreDataService(
			wikiPriceService,
			new WikiRecipeService(),
			priceCacheService,
			new PlayerSkillService(),
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		// Verify all 6 recommendation categories are generated with valid "Why?" reasons
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

		// Instantiate full HerbWisePanel and verify all 6 tabs + Recipe Detail View layout cleanly at 580px width
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

		// Also test opening Recipe Detail View
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

		// Generate/verify 16x16 toolbar icon at src/main/resources/com/herbwise/icon.png
		// and 48x72 Plugin Hub badge icon at root icon.png
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
