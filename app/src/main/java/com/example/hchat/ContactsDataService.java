package com.example.hchat;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeffrey Sham on 12/2/2015.
 */
public class ContactsDataService extends Service {
    private IBinder binder = new ContactBinder();

    List<String> contactNumberList;

    @Override
    public void onCreate() {
        super.onCreate();
        contactNumberList = new ArrayList<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class ContactBinder extends Binder {
        public ContactsDataService getService() {
            return ContactsDataService.this;
        }
    }

    public void storeContactNumbers(List<String> numbers) {
        this.contactNumberList = numbers;
    }

    public List<String> getNumbers() {
        return this.contactNumberList;
    }
}
