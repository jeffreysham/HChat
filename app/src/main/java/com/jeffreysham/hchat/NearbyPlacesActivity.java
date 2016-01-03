package com.jeffreysham.hchat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.trnql.smart.base.SmartCompatActivity;
import com.trnql.smart.location.LocationEntry;
import com.trnql.smart.people.PersonEntry;
import com.trnql.smart.places.PlaceEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class NearbyPlacesActivity extends SmartCompatActivity {

    List<PersonEntry> peopleList;
    List<PlaceEntry> placesList;
    ListView placeListView;
    LocationEntry myLocation;
    private EditText placeInputSearch;
    private PlaceListViewAdapter listViewAdapter;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO: Add a map of the people and the places
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_places);
        placesList = new ArrayList<>();
        peopleList = new ArrayList<>();
        placeListView = (ListView) findViewById(R.id.place_list);

        //Search through list
        placeInputSearch = (EditText) findViewById(R.id.placeInputSearch);
        placeInputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                NearbyPlacesActivity.this.listViewAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        context = this;

        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle("Error: Location Services");
            dialog.setMessage("Location Services are disabled. Please enable it on your phone.");
            dialog.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                }
            });
            dialog.show();
        }
    }

    @Override
    protected void smartPlacesChange(List<PlaceEntry> places) {
        Toast.makeText(this,"Loading Places...",Toast.LENGTH_LONG).show();
        if (places.size() > 0) {
            placesList = places;
        } else {
            placesList = new ArrayList<>();
        }
        sortPlaces();
        displayPlaces();
    }

    @Override
    protected void smartLatLngChange(LocationEntry location) {
        if (location != null) {
            myLocation = location;
            Log.i("My location", location.toString());
        }
    }

    @Override
    protected void smartPeopleChange(List<PersonEntry> people) {
        //TODO: need to remove duplicates
        if (people.size() > 0) {
            peopleList = people;
        } else {
            peopleList = new ArrayList<>();
        }
    }

    public void sortPlaces() {
        Collections.sort(placesList, new Comparator<PlaceEntry>() {
            @Override
            public int compare(PlaceEntry lhs, PlaceEntry rhs) {
                int dist1 = lhs.getDistanceFromUser();
                int dist2 = rhs.getDistanceFromUser();
                if (dist1 < dist2) {
                    return -1;
                } else if (dist1 == dist2) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
    }

    public void displayPlaces() {
        Log.i("Display Places", "in display places");
        if (listViewAdapter == null) {
            listViewAdapter = new PlaceListViewAdapter(this, R.layout.places_list_item, placesList, peopleList);
            placeListView.setAdapter(listViewAdapter);
            placeListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            placeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    handleItemClick(placeListView, view, position, id);
                }
            });
        } else {
            listViewAdapter.updateLists(placesList, peopleList);
            listViewAdapter.notifyDataSetChanged();
        }

    }

    private void handleItemClick(ListView l, View v, int position, long id) {
        final PlaceEntry thePlace = (PlaceEntry) l.getItemAtPosition(position);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Navigate")
                .setMessage("Are you sure you want to navigate to " + thePlace.getName())
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        navigateToLocation(thePlace);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialogBuilder.setCancelable(false);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void navigateToLocation(PlaceEntry place) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?saddr="+myLocation.getLatitude()+ "," +
                        myLocation.getLongitude() + "&daddr=" + place.getLatitude() +"," +
                        place.getLongitude()));
        startActivity(intent);
    }
}
