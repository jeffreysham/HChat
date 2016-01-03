package com.jeffreysham.hchat;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.trnql.smart.base.SmartCompatActivity;
import com.trnql.smart.people.PersonEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class NearbyFriendsActivity extends SmartCompatActivity {

    List<PersonEntry> peopleList;
    ListView peopleListView;
    ImageButton placesButton;
    private EditText inputSearch;
    private FriendListViewAdapter listViewAdapter;

    private ContactsDataService contactsDataService;
    private List<String> numbersList;
    private Context context = this;
    ServiceConnection contactsConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ContactsDataService.ContactBinder binder = (ContactsDataService.ContactBinder) service;
            contactsDataService = binder.getService();
            numbersList = contactsDataService.getNumbers();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            contactsDataService = null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = new Intent(this,ContactsDataService.class);
        bindService(i, contactsConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Stop", "In Splash: onStop()");
        unbindService(contactsConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_friends);
        peopleListView = (ListView) findViewById(R.id.contact_list);
        peopleList = new ArrayList<>();
        final Context context = this;

        //Search through list
        inputSearch = (EditText) findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                NearbyFriendsActivity.this.listViewAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        placesButton = (ImageButton) findViewById(R.id.imageButton);
        placesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, NearbyPlacesActivity.class);
                startActivity(intent);
            }
        });

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
    protected void smartPeopleChange(List<PersonEntry> people) {
        //TODO: test if activity thing works
        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String tempActivity = pref.getString("activity", null);
        String useActivity = pref.getString("useActivity", "false");

        Log.i("place", "in smart people change");

        if (useActivity.equals("true")) {
            peopleList = new ArrayList<>();
            for (int i = 0; i < people.size(); i++) {
                PersonEntry person = people.get(i);
                if (person.getActivityString().equals(tempActivity)) {
                    peopleList.add(person);
                }
            }
        } else {
            if (people != null) {
                /*for (PersonEntry person: people) {
                    if (!peopleList.contains(person)) {
                        peopleList.add(person);
                    }
                }*/
                peopleList = people;
            } else {
                peopleList = new ArrayList<>();
            }
        }
        Log.i("people list", peopleList.toString());
        sortUsers();
        displayUsers();
    }

    public void sortUsers() {
        Collections.sort(peopleList, new Comparator<PersonEntry>() {
            @Override
            public int compare(PersonEntry lhs, PersonEntry rhs) {
                int dist1 = lhs.getDistanceFromUser();
                int dist2 = rhs.getDistanceFromUser();
                if (dist1 < dist2) {
                    return -1;
                } else if (dist1 > dist2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    public void displayUsers() {
        if (listViewAdapter == null) {
            listViewAdapter = new FriendListViewAdapter(this, R.layout.contacts_list_item, peopleList);
            peopleListView.setAdapter(listViewAdapter);
            peopleListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            peopleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    handleItemClick(peopleListView, view, position, id);
                }
            });
        } else {
            listViewAdapter.updateList(peopleList);
            listViewAdapter.notifyDataSetChanged();
        }
    }

    private void handleItemClick(ListView l, View v, int position, long id) {
        final PersonEntry theContact = (PersonEntry) l.getItemAtPosition(position);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Add to Contacts?")
                .setMessage("Are you sure you want to add " + getName(theContact))
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addContact(theContact);
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


    public void addContact(PersonEntry person) {
        String name = getName(person);
        String phone = getPhoneNumber(person);
        if (!numbersList.contains(phone)) {
            try {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build());
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(
                                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                                name).build());
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,
                                phone)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build());

                getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

                Toast.makeText(this, "Added contact successfully",Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error adding contact",Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Contact already in phone",Toast.LENGTH_SHORT).show();
        }



    }

    public String getPhoneNumber(PersonEntry person) {
        String phoneNumber = person.getDataPayload();

        try {
            JSONObject jsonObject = new JSONObject(phoneNumber);
            phoneNumber = jsonObject.getString("number");
        } catch (JSONException e) {
            e.printStackTrace();
            phoneNumber = "";
        }
        return phoneNumber;
    }

    public String getName(PersonEntry person) {
        String name = person.getDataPayload();

        try {
            JSONObject jsonObject = new JSONObject(name);
            name = jsonObject.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
            name = "";
        }
        return name;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_nearby_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            editSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void editSettings() {
        LayoutInflater li = LayoutInflater.from(this);
        View alertView = li.inflate(R.layout.settings_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(alertView);
        final EditText nameInput = (EditText) alertView.findViewById(R.id.editNameInput);
        final EditText descriptionInput = (EditText) alertView.findViewById(R.id.editDescriptionInput);
        final CheckBox checkBox = (CheckBox) alertView.findViewById(R.id.checkBox);
        final TextView activityInput = (TextView) alertView.findViewById(R.id.editActivityInput);

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String result = pref.getString("useActivity", "false");
        String desc = pref.getString("description", "");
        String name = pref.getString("name", "");
        String activity = pref.getString("activity", "");
        if (result.equals("true")) {
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }

        if (desc.length() > 0) {
            descriptionInput.setText(desc);
        }

        if (name.length() > 0) {
            nameInput.setText(name);
        }

        if (activity.length() > 0) {
            activityInput.setText("Activity Status: " + activity);
        } else {
            activityInput.setText("Activity Status: Not Available");
        }

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setUserPhoneNumber(nameInput.getText().toString(),
                                        descriptionInput.getText().toString(),
                                        checkBox);
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
        alertDialogBuilder.create().show();
    }

    public void setUserPhoneNumber(String name, String description, CheckBox checkBox) {

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);

        if (name.length() > 0) {
            String tempNumber = pref.getString("phone number", null);
            String tempDesc = pref.getString("description", "");
            getPeopleManager().setUserToken(tempNumber);

            String dataString = "";
            JSONObject object = new JSONObject();
            try {
                object.put("name", name);
                object.put("number", tempNumber);
                object.put("description", tempDesc);
                dataString = object.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            getPeopleManager().setDataPayload(dataString);

            //getPeopleManager().setDataPayload("name=" + name + ", number=" + tempNumber + ", description="+tempDesc);
            pref.edit().putString("name", name).apply();
        }

        if (description.length() > 0) {
            String tempNumber = pref.getString("phone number", null);
            String tempName = pref.getString("name", null);
            getPeopleManager().setUserToken(tempNumber);

            String dataString = "";
            JSONObject object = new JSONObject();
            try {
                object.put("name", tempName);
                object.put("number", tempNumber);
                object.put("description", description);
                dataString = object.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            getPeopleManager().setDataPayload(dataString);

            pref.edit().putString("description", description).apply();
        }

        if (checkBox.isChecked()) {
            pref.edit().putString("useActivity", "true").apply();
        } else {
            pref.edit().putString("useActivity", "false").apply();
        }

    }
}
