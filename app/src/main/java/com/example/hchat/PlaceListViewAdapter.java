package com.example.hchat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.javadocmd.simplelatlng.util.LengthUnit;
import com.trnql.smart.people.PersonEntry;
import com.trnql.smart.places.PlaceEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeffrey Sham on 5/25/2015.
 */
public class PlaceListViewAdapter extends ArrayAdapter<PlaceEntry>{
    private Context context;
    private PlacesListFilter filter;
    private List<PlaceEntry> originalList;
    private List<PlaceEntry> filteredList;
    private List<PersonEntry> peopleList;

    public PlaceListViewAdapter(Context context, int resource, List<PlaceEntry> items, List<PersonEntry> people) {
        super(context, resource, items);
        this.context = context;
        this.filteredList = items;
        this.peopleList = people;
        this.originalList = new ArrayList<>();
        this.originalList.addAll(items);
    }

    private class PlaceViewHolder {
        ImageView placeImage;
        TextView placeNameText;
        TextView placeAddressText;
        TextView placeNumPeopleText;
        TextView placeDistanceText;
    }

    public void updateLists(List<PlaceEntry> list, List<PersonEntry> peopleList) {
        this.filteredList = list;
        this.originalList = new ArrayList<>();
        this.originalList.addAll(list);
        this.peopleList = peopleList;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        PlaceViewHolder holder;
        PlaceEntry rowItem = filteredList.get(position);
        LayoutInflater rowViewInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if(convertView == null){
            convertView = rowViewInflater.inflate(R.layout.places_list_item,null);
            holder = new PlaceViewHolder();
            holder.placeImage = (ImageView) convertView.findViewById(R.id.place_image);
            holder.placeNameText = (TextView) convertView.findViewById(R.id.placeNameTextView);
            holder.placeAddressText = (TextView) convertView.findViewById(R.id.placeAddressTextView);
            holder.placeDistanceText = (TextView) convertView.findViewById(R.id.placeDistanceToTextView);
            holder.placeNumPeopleText = (TextView) convertView.findViewById(R.id.placeNumPeopleTextView);
            convertView.setTag(holder);
        } else {
            holder = (PlaceViewHolder) convertView.getTag();
        }
        if (rowItem != null) {
            List<Bitmap> images = rowItem.getImages();
            if (images != null && images.size() > 0) {
                Bitmap resized = Bitmap.createScaledBitmap(images.get(0), 100, 150, true);
                holder.placeImage.setImageBitmap(resized);
            } else {
                holder.placeImage.setImageDrawable(context.getResources().getDrawable(R.drawable.hatchat_icon));
            }

            holder.placeNameText.setText(rowItem.getName());
            holder.placeAddressText.setText(rowItem.getAddress());
            double dist = rowItem.getDistanceFromUser(LengthUnit.METER)* .000621371;

            String distString = String.format("%.2f miles away", dist);
            holder.placeDistanceText.setText(distString);
            holder.placeNumPeopleText.setText(getNumPeople(rowItem) + " HatChatters near here");
        }

        return convertView;
    }

    public int getNumPeople(PlaceEntry place) {
        double longitude = place.getLongitude();
        double latitude = place.getLatitude();

        int numPeople = 0;

        for (int i = 0; i < peopleList.size(); i++) {
            PersonEntry person = peopleList.get(i);
            float[] results = new float[1];
            Location.distanceBetween(person.getLatitude(),person.getLongitude(), latitude, longitude, results);
            if (results[0] > 0 && results[0] < 100) {
                numPeople++;
            }
        }
        return numPeople;
    }


    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new PlacesListFilter();
        }
        return filter;
    }

    private class PlacesListFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            String prefix = constraint.toString().toLowerCase().trim();

            if (prefix == null || prefix.length() == 0) {
                results.values = originalList;
                results.count = originalList.size();
            } else {
                ArrayList<PlaceEntry> newList = new ArrayList<>();

                for (int i = 0; i < originalList.size(); i++) {
                    PlaceEntry place = originalList.get(i);
                    String name = place.getName().toLowerCase();
                    String address = place.getAddress();
                    if (name.contains(prefix) || address.contains(prefix)) {
                        newList.add(place);
                    }
                }

                results.values = newList;
                results.count = newList.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList = (ArrayList<PlaceEntry>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0; i < filteredList.size(); i++) {
                PlaceEntry place = filteredList.get(i);
                add(place);
            }
            notifyDataSetInvalidated();
        }
    }
}
