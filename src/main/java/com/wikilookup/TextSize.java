package com.wikilookup;

public enum TextSize
{
	SMALL(11),
	MEDIUM(13),
	LARGE(16);

	private final int size;

	TextSize(int size)
	{
		this.size = size;
	}

	public int getSize()
	{
		return size;
	}
}
