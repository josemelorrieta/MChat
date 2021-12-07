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
    public ArrayList<String> recuperarContactostelefono() {

        ArrayList<String> phonesList = new ArrayList<String>();

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

                        phonesList.add(PhoneNumberUtils.formatNumber(phoneNo, "ES"));

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

    public void cargarContactosApp (ArrayList<String> phonesList, ArrayList<String> phonesFB, Modelo modelo) {
        modelo.contactos.clear();
        for (String phoneFB : phonesFB) {
            for (String phoneNo : phonesList) {
                if (PhoneNumberUtils.compare(phoneFB, phoneNo)) {
                    modelo.contactos.add(phoneNo);
                }
            }
        }
    }
}
