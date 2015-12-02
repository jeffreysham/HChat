package com.example.hchat;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.trnql.smart.activity.ActivityEntry;
import com.trnql.smart.base.SmartCompatActivity;

public class MainActivity extends SmartCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getAppData().setApiKey("93101c2d-2bea-4ae6-8979-9386e4a559b6");

        getPeopleManager().setSearchRadius(50000);
        getPlacesManager().setRadius(5000);
        getPlacesManager().setIncludeImages(true);
        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String phoneNumber = pref.getString("phone number", null);
        String name = pref.getString("name", null);
        if (phoneNumber == null || name == null) {
            getUserPhoneNumber();
        }

        getPeopleManager().setUserToken(phoneNumber);

        final Context context = this;

        //Go to WriteNewMessageActivity
        Button writeNewMessageButton = (Button) findViewById(R.id.writeNewMessageButton);
        writeNewMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, WriteNewMessageActivity.class);
                startActivity(intent);
            }
        });

        //Go to FriendsActivity
        Button friendsButton = (Button) findViewById(R.id.viewFriendsButton);
        friendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, NearbyFriendsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void smartActivityChange(ActivityEntry userActivity) {
        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        pref.edit().putString("activity", userActivity.getActivityString()).commit();
    }

    public void getUserPhoneNumber() {

        LayoutInflater li = LayoutInflater.from(this);
        View alertView = li.inflate(R.layout.phone_prompt, null);

        AlertDialog.Builder getPhoneNumberDialog = new AlertDialog.Builder(this);

        getPhoneNumberDialog.setView(alertView);
        final EditText nameInput = (EditText) alertView.findViewById(R.id.nameInput);
        final EditText userInput = (EditText) alertView.findViewById(R.id.phoneNumberInput);
        final EditText descInput = (EditText) alertView.findViewById(R.id.phoneDescriptionInput);

        getPhoneNumberDialog.setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setUserPhoneNumber(nameInput.getText().toString(), userInput.getText().toString(),
                                        descInput.getText().toString());
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
        getPhoneNumberDialog.create().show();
    }

    public void setUserPhoneNumber(String name, String number, String description) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        String realPhoneNumber = "";
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(number, "US");
            realPhoneNumber = "" + phoneNumber.getNationalNumber();
        } catch (NumberParseException e) {
            e.printStackTrace();
        }

        if (realPhoneNumber.length() > 0 && name.length() > 0 && description.length() > 0) {
            getPeopleManager().setUserToken(realPhoneNumber);
            getPeopleManager().setDataPayload("name="+name+", number="+realPhoneNumber+", description="+description);
            SharedPreferences pref = this.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
            pref.edit().putString("name", name).commit();
            pref.edit().putString("phone number", realPhoneNumber).commit();
            pref.edit().putString("description", description).commit();
            pref.edit().putString("useActivity", "false").commit();
        } else {
            Toast.makeText(this, "Error with input.", Toast.LENGTH_SHORT);
            getUserPhoneNumber();
        }


    }


}
