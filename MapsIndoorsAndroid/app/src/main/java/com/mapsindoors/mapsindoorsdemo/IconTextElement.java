package com.mapsindoors.mapsindoorsdemo;

import android.graphics.Bitmap;

public class IconTextElement
{
	public String name;
	public Integer imgId;
	public Bitmap img;
	public Object obj;
	public IconTextListAdapter.Objtype type;

	public IconTextElement(String name, Integer imgId, Object obj, IconTextListAdapter.Objtype type)
	{
		this.name = name;
		this.imgId = imgId;
		this.img = null;
		this.obj = obj;
		this.type = type;
	}

	public IconTextElement(String name, Bitmap img, Object obj, IconTextListAdapter.Objtype type)
	{
		this.name = name;
		this.imgId = -1;
		this.img = img;
		this.obj = obj;
		this.type = type;
	}
}
