package com.wikilookup;

import java.awt.image.BufferedImage;
import net.runelite.http.api.item.ItemStats;

/**
 * Holds all fetched data for a single item lookup.
 * Built on the background thread, consumed on the EDT.
 */
class WikiItemInfo
{
	final String title;
	final String extract;
	final String wikiUrl;
	final boolean questItem;
	final boolean members;
	final int gePrice;
	final int highAlch;
	final BufferedImage icon;
	final ItemStats itemStats;       // stats of the looked-up item  (may be null)
	final ItemStats equippedStats;   // stats of whatever is in that slot (may be null)
	final String equippedItemName;   // name of that equipped item (may be null)

	WikiItemInfo(String title, String extract, String wikiUrl,
		boolean questItem, boolean members, int gePrice, int highAlch,
		BufferedImage icon,
		ItemStats itemStats, ItemStats equippedStats, String equippedItemName)
	{
		this.title = title;
		this.extract = extract;
		this.wikiUrl = wikiUrl;
		this.questItem = questItem;
		this.members = members;
		this.gePrice = gePrice;
		this.highAlch = highAlch;
		this.icon = icon;
		this.itemStats = itemStats;
		this.equippedStats = equippedStats;
		this.equippedItemName = equippedItemName;
	}
}
