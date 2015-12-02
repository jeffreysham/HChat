package com.example.hchat;

import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.javadocmd.simplelatlng.util.LengthUnit;
import com.trnql.smart.base.SmartCompatActivity;
import com.trnql.smart.people.PersonEntry;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_friends);
        peopleListView = (ListView) findViewById(R.id.contact_list);

        final Context context = this;

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String tempNumber = pref.getString("phone number", null);
        getPeopleManager().setUserToken(tempNumber);

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
    }

    @Override
    protected void smartPeopleChange(List<PersonEntry> people) {
        //TODO: test if .contains works to remove duplicates
        //TODO: test if activity thing works
        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String tempActivity = pref.getString("activity", null);
        String useActivity = pref.getString("useActivity", "false");

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
                } else if (dist1 == dist2) {
                    return 0;
                } else {
                    return 1;
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


    }

    public String getPhoneNumber(PersonEntry person) {
        String phoneNumber = person.getDataPayload();
        String tempString = phoneNumber.substring(phoneNumber.indexOf(",") + 1);
        phoneNumber = tempString.substring(tempString.indexOf("=") + 1, tempString.indexOf(","));
        return phoneNumber;
    }

    public String getName(PersonEntry person) {
        String name = person.getDataPayload();
        name = name.substring(name.indexOf("=")+1, name.indexOf(","));
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

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String result = pref.getString("useActivity", "false");
        String desc = pref.getString("description", "");
        String name = pref.getString("name", "");
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
            getPeopleManager().setDataPayload("name=" + name + ", number=" + tempNumber + ", description="+tempDesc);
            pref.edit().putString("name", name).commit();
        }

        if (description.length() > 0) {
            String tempNumber = pref.getString("phone number", null);
            String tempName = pref.getString("name", null);
            getPeopleManager().setUserToken(tempNumber);
            getPeopleManager().setDataPayload("name=" + tempName + ", number=" + tempNumber+", description=" + description);
            pref.edit().putString("description", description).commit();
        }

        if (checkBox.isChecked()) {
            pref.edit().putString("useActivity", "true").commit();
        } else {
            pref.edit().putString("useActivity", "false").commit();
        }

    }
}