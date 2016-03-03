package com.jeffreysham.hchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.List;


public class SplashActivity extends Activity {

    private ContactsDataService contactsDataService;
    private List<String> numbersList;
    private Context context = this;
    ServiceConnection contactsConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ContactsDataService.ContactBinder binder = (ContactsDataService.ContactBinder) service;
            contactsDataService = binder.getService();
            FindContactsInBackground f = new FindContactsInBackground();
            f.execute();
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
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    private class FindContactsInBackground extends AsyncTask<Void,Void,Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            contactsDataService.storeContactNumbers(numbersList);
            SharedPreferences pref = context.getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
            String phoneNumber = pref.getString("phone number", null);
            if (phoneNumber != null) {
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            } else {
                String message = "HatChat Messaging asks users for their location, name, and phone number. " +
                        "This information is used in order to create your profile and locate nearby users. " +
                        "Also, your phone number will be used so that you can call or message nearby users. " +
                        "Next, your contacts are accessed in order to ensure that no duplicate contacts will be " +
                        "added. This application will never collect or transmit your personal data without " +
                        "your consent.";

                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Privacy Policy")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(context,MainActivity.class);
                                startActivity(intent);
                            }
                        });
                alert.create().show();
            }

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params) {
            findAndStoreContacts();

            return null;
        }
    }

    public void findAndStoreContacts() {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        numbersList = new ArrayList<String>();
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));

                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    //Query phone here.  Covered next
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

                    if (pCur.moveToNext()) {
                        // Do something with phones
                        String number = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        String realPhoneNumber = "";

                        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                        try {
                            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(number, "US");
                            realPhoneNumber = "" + phoneNumber.getNationalNumber();
                        } catch (NumberParseException e) {
                            e.printStackTrace();
                        }

                        if (realPhoneNumber.length() > 0) {
                            if (!numbersList.contains(realPhoneNumber)) {
                                numbersList.add(realPhoneNumber);
                            }
                        }
                    }
                    pCur.close();
                }


            }
        }
        cur.close();
    }
}
