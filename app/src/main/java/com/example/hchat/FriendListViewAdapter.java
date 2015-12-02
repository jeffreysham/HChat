package com.example.hchat;

import android.app.Activity;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeffrey Sham on 5/25/2015.
 */
public class FriendListViewAdapter extends ArrayAdapter<PersonEntry>{
    private Context context;
    private FriendsListFilter filter;
    private List<PersonEntry> originalList;
    private List<PersonEntry> filteredList;

    public FriendListViewAdapter(Context context, int resource, List<PersonEntry> items) {
        super(context, resource, items);
        this.context = context;
        this.filteredList = items;
        this.originalList = new ArrayList<>();
        this.originalList.addAll(items);
    }

    private class FriendViewHolder {
        TextView friendNameText;
        TextView friendNumberText;
        TextView friendDistanceText;
        TextView friendDescriptionText;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        FriendViewHolder holder;

        if (position < filteredList.size()) {

            PersonEntry rowItem = filteredList.get(position);
            LayoutInflater rowViewInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = rowViewInflater.inflate(R.layout.contacts_list_item, null);
                holder = new FriendViewHolder();
                holder.friendNameText = (TextView) convertView.findViewById(R.id.contactNameTextView);
                holder.friendNumberText = (TextView) convertView.findViewById(R.id.contactNumberTextView);
                holder.friendDistanceText = (TextView) convertView.findViewById(R.id.contactDistanceToTextView);
                holder.friendDescriptionText = (TextView) convertView.findViewById(R.id.contactDescriptionTextView);
                convertView.setTag(holder);
            } else {
                holder = (FriendViewHolder) convertView.getTag();
            }
            if (rowItem != null) {
                holder.friendNameText.setText(getName(rowItem));
                holder.friendNumberText.setText(getPhoneNumber(rowItem));
                String distString = String.format("%.2f miles away", getDistanceTo(rowItem));
                holder.friendDistanceText.setText(distString);
                holder.friendDescriptionText.setText(getDescription(rowItem));
            }
        }

        return convertView;

    }

    public void updateList(List<PersonEntry> items) {
        this.filteredList = items;
        this.originalList = new ArrayList<>();
        this.originalList.addAll(items);
    }

    public String getPhoneNumber(PersonEntry person) {
        String phoneNumber = person.getDataPayload();

        if (phoneNumber != null) {
            String tempString = phoneNumber.substring(phoneNumber.indexOf(",") + 1);

            phoneNumber = tempString.substring(tempString.indexOf("=") + 1, tempString.indexOf(","));
        } else {
            phoneNumber = "";
        }

        return phoneNumber;
    }

    public String getDescription(PersonEntry person) {
        String desc = person.getDataPayload();
        if (desc != null) {
            desc = desc.substring(desc.lastIndexOf("=")+1);
        } else {
            desc = "";
        }

        return desc;
    }

    public String getName(PersonEntry person) {
        String name = person.getDataPayload();
        if (name!=null) {
            name = name.substring(name.indexOf("=")+1, name.indexOf(","));
        } else {
            name = "";
        }

        return name;
    }

    public double getDistanceTo(PersonEntry person) {
        int distance = person.getDistanceFromUser(LengthUnit.METER);
        double dist = distance * .000621371;
        return dist;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new FriendsListFilter();
        }
        return filter;
    }

    private class FriendsListFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            String prefix = constraint.toString().toLowerCase().trim();

            if (prefix == null || prefix.length() == 0) {
                results.values = originalList;
                results.count = originalList.size();
            } else {
                ArrayList<PersonEntry> newList = new ArrayList<>();

                for (int i = 0; i < originalList.size(); i++) {
                    PersonEntry contact = originalList.get(i);
                    String name = getName(contact).toLowerCase();
                    String number = getPhoneNumber(contact);
                    String desc = getDescription(contact);
                    if (name.contains(prefix) || number.contains(prefix) || desc.contains(prefix)) {
                        newList.add(contact);
                    }
                }

                results.values = newList;
                results.count = newList.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList = (ArrayList<PersonEntry>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0; i < filteredList.size(); i++) {
                PersonEntry contact = filteredList.get(i);
                add(contact);
            }
            notifyDataSetInvalidated();
        }
    }
}
