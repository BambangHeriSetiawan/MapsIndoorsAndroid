package com.mapsindoors.mapsindoorsdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class IconTextListAdapter extends ArrayAdapter<String> {

	private ArrayList<IconTextElement> itemList;
	private Context context;
	private String tintColor = null;
	public enum Objtype { LOCATION, TYPE, ROUTE, OPENINGHOURS, PHONE, URL }

	public IconTextListAdapter(Context context) {
		super(context, R.layout.mainmenuitem);
		this.context = context;
	}

	public void setTint(String tintColor)
	{
		this.tintColor = tintColor;
	}

	public void setList(ArrayList<IconTextElement> itemList )
	{
		clear();
		this.itemList = itemList;
		ArrayList<String> collectionList = new ArrayList<>(itemList.size());
		for (IconTextElement element : itemList )
			collectionList.add(element.name);
		addAll(collectionList);
	}

	public void addToList(IconTextElement newElement)
	{
		itemList.add(newElement);
		add(newElement.toString());
	}

	public Object getItemObj(int index)
	{
		return itemList.get(index).obj;
	}

	public Objtype getObjType(int index)
	{
		return itemList.get(index).type;
	}

	public View getView(int index, View view,ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View single_row = inflater.inflate(R.layout.mainmenuitem, null,true);
		TextView txtTitle = (TextView) single_row.findViewById(R.id.textitem);
		ImageView imageView = (ImageView) single_row.findViewById(R.id.iconitem);
		ImageView imageViewTint = (ImageView) single_row.findViewById(R.id.iconitem_tint);
		IconTextElement element = itemList.get(index);

		txtTitle.setText(element.name);
		if ( tintColor == null )
		{
			setImage( imageView, imageViewTint, element.img, element.imgId);
		}
		else
		{
			setImage(imageViewTint, imageView , element.img, element.imgId);
		}
		single_row.setFocusable(false);
		return single_row;
	}

	private void setImage(ImageView visible, ImageView invisible, Bitmap img, Integer imgId)
	{
		invisible.setVisibility(View.INVISIBLE);
		visible.setVisibility(View.VISIBLE);
		if (img != null)
		{
			visible.setImageBitmap(img);
		}
		else
		{
			visible.setImageResource(imgId);
		}
	}
}