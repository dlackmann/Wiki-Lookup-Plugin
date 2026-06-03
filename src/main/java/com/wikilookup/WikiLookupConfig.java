package com.wikilookup;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("wikilookup")
public interface WikiLookupConfig extends Config
{
	@ConfigItem(
		keyName = "showOnGroundItems",
		name = "Show on ground items",
		description = "Add Wiki Lookup to the right-click menu of items on the ground",
		position = 0
	)
	default boolean showOnGroundItems()
	{
		return true;
	}

	@Range(min = 11, max = 28)
	@ConfigItem(
		keyName = "fontSize",
		name = "Text size",
		description = "Font size for the wiki article text (11–28)",
		position = 1
	)
	default int fontSize()
	{
		return 14;
	}
}
