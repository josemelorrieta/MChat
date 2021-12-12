package com.chemanu.mchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;

import java.io.File;
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

    public Bitmap cargarImagen (String imagen, int reqWidth, int reqHeight) {
        File file = new File(imagen);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.toString(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(file.toString(), options);

    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
