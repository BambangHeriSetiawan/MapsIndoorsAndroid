package com.mapsindoors;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class VenueListAdapter extends ArrayAdapter<String>
{

	private ArrayList<IconTextElement> itemList;
	private Context context;

	public VenueListAdapter(Context context) {
		super(context, R.layout.mainmenuitem);
		this.context = context;
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

	@Override
	public View getDropDownView(int index, View view, ViewGroup parent)
	{
		IconTextElement element = itemList.get(index);
		TextView single_row = new TextView(context);
		single_row.setText(element.name);
		single_row.setFocusable(false);
		single_row.setTextColor(ContextCompat.getColor(context, R.color.black));
		single_row.setPadding(32,16,32,16);
		single_row.setHorizontallyScrolling(true);
		single_row.setGravity(Gravity.CENTER_VERTICAL);
		return single_row;
	}

	public View getView(int index, View view, ViewGroup parent)
	{
		IconTextElement element = itemList.get(index);
		TextView single_row = new TextView(context);
		single_row.setText(element.name);
		single_row.setFocusable(false);
		single_row.setTextColor(ContextCompat.getColor(context, R.color.white));
		return single_row;
	}
}