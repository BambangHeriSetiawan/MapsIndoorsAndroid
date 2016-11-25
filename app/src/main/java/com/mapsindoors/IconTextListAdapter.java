package com.mapsindoors;

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
	public enum Objtype { LOCATION, TYPE, CATEGORY, ROUTE, OPENINGHOURS, PHONE, URL, LANGUAGE, VENUE, PLACE}

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
		IconTextElement element = itemList.get(index);
		if (element.type == Objtype.VENUE)
		{
			TextView single_row = new TextView(context);
			single_row.setText(element.name);
			single_row.setFocusable(false);
			return single_row;
		}
		else if (element.type == Objtype.LANGUAGE)
		{
			View single_row = inflater.inflate(R.layout.mainmenu_languageitem, null,true);
			TextView txtTitle = (TextView) single_row.findViewById(R.id.textitem);
			txtTitle.setText(element.name);
			ImageView imageView = (ImageView)single_row.findViewById(R.id.flagimage);
			setImage(imageView, imageView, element.img, element.imgId);
			ImageView SelectorView = (ImageView)single_row.findViewById(R.id.selector);
			String language = ((MapsIndoorsActivity)context).getMapControl().getLangugage();
			Integer radioImage = language.contentEquals((String)element.obj) ? android.R.drawable.radiobutton_on_background : android.R.drawable.radiobutton_off_background;
			setImage(SelectorView, SelectorView, null, radioImage);
			single_row.setFocusable(false);
			return single_row;
		}
		else if (element.type == Objtype.LOCATION)
		{
			View dual_row = inflater.inflate(R.layout.mainmenu_twolineitem, null, true);

			TextView txtTitleMain = (TextView)dual_row.findViewById(R.id.textitem_main);
			TextView txtTitleSub = (TextView)dual_row.findViewById(R.id.textitem_sub);
			TextView txtTitleDist = (TextView)dual_row.findViewById(R.id.textitem_dist);
			txtTitleMain.setText(element.name);
			txtTitleSub.setText(element.subText);
			txtTitleDist.setText(element.distText);

			ImageView imageView = (ImageView)dual_row.findViewById(R.id.iconitem);
			ImageView imageViewTint = (ImageView)dual_row.findViewById(R.id.iconitem_tint);
			if (tintColor == null)
			{
				setImage(imageView, imageViewTint, element.img, element.imgId);
			}
			else
			{
				setImage(imageViewTint, imageView, element.img, element.imgId);
			}
			dual_row.setFocusable(false);
			return dual_row;
		}
		else
		{
			//Category element
			View single_row = inflater.inflate(R.layout.mainmenuitem, null, true);
			TextView txtTitle = (TextView)single_row.findViewById(R.id.textitem);
			ImageView imageView = (ImageView)single_row.findViewById(R.id.iconitem);
			ImageView imageViewTint = (ImageView)single_row.findViewById(R.id.iconitem_tint);
			txtTitle.setText(element.name);
			if (tintColor == null)
			{
				setImage(imageView, imageViewTint, element.img, element.imgId);
			}
			else
			{
				setImage(imageViewTint, imageView, element.img, element.imgId);
			}
			single_row.setFocusable(false);
			return single_row;
		}
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