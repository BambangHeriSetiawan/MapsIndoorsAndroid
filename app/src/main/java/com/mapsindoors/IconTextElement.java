package com.mapsindoors;

import android.graphics.Bitmap;

public class IconTextElement
{
	public String name = null;
	public String subText = null;
	public String distText = null;
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

	public IconTextElement(String name, String subline, double distance, Integer imgId, Object obj, IconTextListAdapter.Objtype type)
	{
		this.name = name;
		this.subText = subline;
		this.distText = (distance < 1000) ? ""+(int)Math.round(distance) +"m" : ""+(int)Math.round(distance/1000)+"km";
		this.imgId = imgId;
		this.img = null;
		this.obj = obj;
		this.type = type;
	}

	public IconTextElement(String name, String subline, double distance, Bitmap img, Object obj, IconTextListAdapter.Objtype type)
	{
		this.name = name;
		this.subText = subline;
		this.distText = (distance < 1000) ? ""+(int)Math.round(distance) +"m" : ""+(int)Math.round(distance/1000)+"km";
		this.imgId = -1;
		this.img = img;
		this.obj = obj;
		this.type = type;
	}
}
