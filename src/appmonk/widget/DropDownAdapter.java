package appmonk.widget;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.SpinnerAdapter;

public class DropDownAdapter implements SpinnerAdapter {
    Adapter mBaseAdapter;
    Adapter mDropDownAdapter;
    
    public DropDownAdapter(Adapter baseAdapter, Adapter dropDownAdapter) {
        mBaseAdapter = baseAdapter;
        mDropDownAdapter = dropDownAdapter;
    }
    
    public int getCount() {
        return mBaseAdapter.getCount();
    }

    public Object getItem(int position) {
        return mBaseAdapter.getItem(position);
    }

    public long getItemId(int position) {
        return mBaseAdapter.getItemId(position);
    }

    public int getItemViewType(int position) {
        return mBaseAdapter.getItemViewType(position);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        return mBaseAdapter.getView(position, convertView, parent);
    }

    public int getViewTypeCount() {
        return mBaseAdapter.getViewTypeCount();
    }

    public boolean hasStableIds() {
        return mBaseAdapter.hasStableIds();
    }

    public boolean isEmpty() {
        return mBaseAdapter.isEmpty();
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        mBaseAdapter.registerDataSetObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mBaseAdapter.unregisterDataSetObserver(observer);
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return mDropDownAdapter.getView(position, convertView, parent);
    }

}
