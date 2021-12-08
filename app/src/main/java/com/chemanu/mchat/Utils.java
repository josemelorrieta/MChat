package com.chemanu.mchat;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import java.util.ArrayList;

public class Utils {

    private Context context;

    public Utils (Context context) {
        this.context = context;
    }

    @SuppressLint("Range")
    public ArrayList<User> recuperarContactostelefono() {

        ArrayList<User> phonesList = new ArrayList<User>();

        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                 String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));

                        phoneNo = PhoneNumberUtils.formatNumber(phoneNo, "ES");

                        String name = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.Data.DISPLAY_NAME));

                        phonesList.add(new User(name, phoneNo));

                    }
                    pCur.close();
                }
            }
        }
        if(cur!=null){
            cur.close();
        }

        return phonesList;
    }

    public ArrayList<User> generarContactosApp (ArrayList<User> phonesList, ArrayList<User> usersFB) {
        ArrayList<User> contactos = new ArrayList<User>();

        for (User userFB : usersFB) {
            for (int i = 0; i< phonesList.size();i++) {
                if (PhoneNumberUtils.compare(userFB.getPhone(), phonesList.get(i).getPhone())) {
                    contactos.add(userFB);
                }
            }
        }

        return contactos;
    }
}
