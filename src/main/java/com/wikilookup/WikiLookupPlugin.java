package com.wikilookup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
	name = "Wiki Lookup",
	description = "Right-click any item to look it up on the OSRS Wiki",
	tags = {"wiki", "item", "lookup", "info", "accessibility"}
)
public class WikiLookupPlugin extends Plugin
{
	private static final String MENU_OPTION = "Wiki Lookup";

	private static final String WIKI_API = "https://oldschool.runescape.wiki/api.php"
		+ "?action=query"
		+ "&prop=extracts|revisions"
		+ "&exintro=1&explaintext=1"
		+ "&rvprop=content"
		+ "&rvslots=main"
		+ "&format=json"
		+ "&titles=";

	private static final String WIKI_BASE_URL = "https://oldschool.runescape.wiki/w/";

	private static final Pattern QUEST_PATTERN =
		Pattern.compile("\\|\\s*quest\\s*=\\s*([^|\\}\\n]+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern MEMBERS_PATTERN =
		Pattern.compile("\\|\\s*members\\s*=\\s*([^|\\}\\n]+)", Pattern.CASE_INSENSITIVE);

	@Inject
	private Client client;

	@Inject
	private WikiLookupConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OkHttpClient okHttpClient;

	private WikiLookupPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		panel = new WikiLookupPanel(config);

		navButton = NavigationButton.builder()
			.tooltip("Wiki Lookup")
			.icon(buildIcon())
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		log.debug("Wiki Lookup started");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		log.debug("Wiki Lookup stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("wikilookup") && panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.refreshFont());
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getOption().equals("Examine")
			&& (event.getActionParam1() >> 16) == InterfaceID.INVENTORY)
		{
			client.createMenuEntry(-1)
				.setOption(MENU_OPTION)
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.setParam0(event.getActionParam0())
				.setParam1(event.getActionParam1())
				.setIdentifier(event.getIdentifier());
			return;
		}

		if (config.showOnGroundItems()
			&& event.getOption().equals("Examine")
			&& event.getType() == MenuAction.EXAMINE_ITEM_GROUND.getId())
		{
			client.createMenuEntry(-1)
				.setOption(MENU_OPTION)
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.setParam0(event.getActionParam0())
				.setParam1(event.getActionParam1())
				.setIdentifier(event.getIdentifier());
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!event.getMenuOption().equals(MENU_OPTION))
		{
			return;
		}

		int slot = event.getParam0();
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		int itemId = -1;

		if (inventory != null && slot >= 0)
		{
			Item item = inventory.getItem(slot);
			if (item != null)
			{
				itemId = item.getId();
			}
		}
		if (itemId == -1)
		{
			itemId = event.getId();
		}
		if (itemId == -1)
		{
			return;
		}

		final int canonId = itemManager.canonicalize(itemId);

		String itemName = itemManager.getItemComposition(canonId).getName();
		BufferedImage icon = itemManager.getImage(canonId);

		ItemStats itemStats = itemManager.getItemStats(canonId);
		ItemStats equippedStats = null;
		String equippedItemName = null;

		if (itemStats != null && itemStats.getEquipment() != null)
		{
			int equipSlot = itemStats.getEquipment().getSlot();
			ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
			if (equipment != null)
			{
				Item equipped = equipment.getItem(equipSlot);
				if (equipped != null && equipped.getId() != -1)
				{
					equippedStats = itemManager.getItemStats(equipped.getId());
					equippedItemName = itemManager.getItemComposition(equipped.getId()).getName();
				}
			}
		}

		final int gePrice = itemManager.getItemPrice(canonId);
		final int highAlch = (int)(itemManager.getItemComposition(canonId).getPrice() * 0.6f);

		final ItemStats finalItemStats = itemStats;
		final ItemStats finalEquippedStats = equippedStats;
		final String finalEquippedName = equippedItemName;
		final String finalItemName = itemName;

		SwingUtilities.invokeLater(() ->
		{
			clientToolbar.openPanel(navButton);
			panel.setLoading(finalItemName, icon);
		});

		new Thread(() -> fetchWikiData(
			canonId, finalItemName, icon, gePrice, highAlch,
			finalItemStats, finalEquippedStats, finalEquippedName
		), "wiki-lookup").start();
	}

	private void fetchWikiData(int itemId, String itemName, BufferedImage icon, int gePrice, int highAlch,
		ItemStats itemStats, ItemStats equippedStats, String equippedItemName)
	{
		try
		{
			String encoded = URLEncoder.encode(itemName, StandardCharsets.UTF_8)
				.replace("+", "_");
			String url = WIKI_API + encoded;

			Request request = new Request.Builder()
				.url(url)
				.header("User-Agent", "RuneLite-WikiLookupPlugin/1.0")
				.build();

			try (Response response = okHttpClient.newCall(request).execute())
			{
				if (!response.isSuccessful() || response.body() == null)
				{
					SwingUtilities.invokeLater(() ->
						panel.setError("Wiki request failed (" + response.code() + ")"));
					return;
				}

				String body = response.body().string();
				JsonObject root = new JsonParser().parse(body).getAsJsonObject();
				JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
				JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();

				if (page.has("missing"))
				{
					SwingUtilities.invokeLater(() ->
						panel.setError("No wiki article found for \"" + itemName + "\""));
					return;
				}

				String title = page.get("title").getAsString();
				String extract = page.has("extract")
					? page.get("extract").getAsString().trim()
					: "No description available.";
				String wikiUrl = WIKI_BASE_URL + encoded;

				String wikitext = extractWikitext(page);
				boolean questItem = parseQuestItem(wikitext);
				boolean members = parseMembers(wikitext);

				WikiItemInfo info = new WikiItemInfo(
					title, extract, wikiUrl,
					questItem, members, gePrice, highAlch,
					icon, itemStats, equippedStats, equippedItemName
				);

				SwingUtilities.invokeLater(() -> panel.setContent(info));
			}
		}
		catch (Exception e)
		{
			log.error("Error fetching wiki data for {}", itemName, e);
			SwingUtilities.invokeLater(() ->
				panel.setError("Error: " + e.getClass().getSimpleName() + " -- " + e.getMessage()));
		}
	}

	private static String extractWikitext(JsonObject page)
	{
		if (!page.has("revisions"))
		{
			return "";
		}
		JsonArray revisions = page.getAsJsonArray("revisions");
		if (revisions.size() == 0)
		{
			return "";
		}
		JsonObject rev = revisions.get(0).getAsJsonObject();
		if (rev.has("*"))
		{
			return rev.get("*").getAsString();
		}
		if (rev.has("slots"))
		{
			JsonObject slots = rev.getAsJsonObject("slots");
			if (slots.has("main"))
			{
				JsonObject main = slots.getAsJsonObject("main");
				if (main.has("*"))
				{
					return main.get("*").getAsString();
				}
			}
		}
		return "";
	}

	private static boolean parseQuestItem(String wikitext)
	{
		Matcher m = QUEST_PATTERN.matcher(wikitext);
		if (!m.find())
		{
			return false;
		}
		String val = m.group(1).trim().toLowerCase();
		return !val.isEmpty() && !val.equals("no") && !val.equals("n/a") && !val.equals("none");
	}

	private static boolean parseMembers(String wikitext)
	{
		Matcher m = MEMBERS_PATTERN.matcher(wikitext);
		if (!m.find())
		{
			return false;
		}
		return m.group(1).trim().toLowerCase().startsWith("y");
	}

	private static BufferedImage buildIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(50, 100, 200));
		g.fillOval(1, 1, 13, 13);
		g.setColor(Color.WHITE);
		g.setFont(new Font("SansSerif", Font.BOLD, 11));
		FontMetrics fm = g.getFontMetrics();
		g.drawString("?", (16 - fm.stringWidth("?")) / 2, 12);
		g.dispose();
		return img;
	}

	@Provides
	WikiLookupConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WikiLookupConfig.class);
	}
}
