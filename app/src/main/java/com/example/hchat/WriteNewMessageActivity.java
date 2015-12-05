package com.example.hchat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.trnql.smart.base.SmartCompatActivity;
import com.trnql.smart.people.PersonEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WriteNewMessageActivity extends SmartCompatActivity {

    private EditText editText;
    private ImageButton sendButton;
    private ImageButton callButton;
    private List<PersonEntry> peopleList;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write_message_screen);
        context = this;
        sendButton = (ImageButton) findViewById(R.id.sendButton);
        callButton = (ImageButton) findViewById(R.id.callButton);
        editText = (EditText)findViewById(R.id.editText);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callUser();
            }
        });
    }

    @Override
    protected void smartPeopleChange(List<PersonEntry> people) {
        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String tempActivity = pref.getString("activity", null);
        String useActivity = pref.getString("useActivity", "false");
        peopleList = new ArrayList<>();
        
        if (useActivity.equals("true")) {

            for (int i = 0; i < people.size(); i++) {
                PersonEntry person = people.get(i);
                if (person.getActivityString().equals(tempActivity)) {
                    peopleList.add(person);
                }
            }
        } else {
            if (people != null) {
                peopleList = people;
            } else {
                peopleList = new ArrayList<>();
            }
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

    //Calls a random user
    public void callUser() {
        if (peopleList.size() > 0) {
            Random ran = new Random();
            int randomNum = ran.nextInt(peopleList.size());
            PersonEntry theContact = peopleList.get(randomNum);

            try {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + getPhoneNumber(theContact)));
                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(this,
                        "Call failed, please try again later!",
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle("Error");
            alertDialog.setMessage("No HatChatters nearby.")
                    .setCancelable(false)
                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialog = alertDialog.create();
            dialog.show();
        }

    }

    //Sends the message to the contact.
    public void sendMessage() {

        if (peopleList.size() > 0) {
            Random ran = new Random();
            int randomNum = ran.nextInt(peopleList.size());
            PersonEntry theContact = peopleList.get(randomNum);

            final String theMessageToSend = editText.getText().toString().trim();

            if (theMessageToSend.length() > 0) {
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(getPhoneNumber(theContact), null, "HatChat: " + theMessageToSend, null, null);
                    Toast.makeText(this, "SMS Sent to " + getName(theContact) + "!",
                            Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Toast.makeText(this,
                            "SMS failed, please try again later!",
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setTitle("Error");
                alertDialog.setMessage("Please write a message before sending.")
                        .setCancelable(false)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                AlertDialog dialog = alertDialog.create();
                dialog.show();
            }


        } else {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle("Error");
            alertDialog.setMessage("No HatChatters nearby.")
                    .setCancelable(false)
                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialog = alertDialog.create();
            dialog.show();

        }
    }
}
