package com.chemanu.mchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;

    private Modelo modelo;

    private EditText edtPhone, edtCode;
    private Button btnRegistrar;
    private ImageView imgBackground;

    private FirebaseFirestore db;

    private static final int PERMISO_CONTACTOS = 0;

    private ArrayList<String> phonesList = new ArrayList<String>(),
                                phonesFB = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modelo = (Modelo) getApplication();

        edtPhone = findViewById(R.id.editPhone);
        edtCode = findViewById(R.id.edtCode);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        imgBackground = findViewById(R.id.imgBackground);

        btnRegistrar.setEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        //mAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);

        db = FirebaseFirestore.getInstance();

        if (getIntent().getBooleanExtra("EXIT", false))
        {
            finish();
        }

        mCallBacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                //signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                edtCode.setEnabled(true);
                btnRegistrar.setEnabled(true);
            }
        };

    }

    public void startPhoneVerification(View view) {
        String phoneNumber = "+34" + edtPhone.getText().toString();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(MainActivity.this)
                .setCallbacks(mCallBacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void verifyPhoneNumberWithCode(View view) {
        String code = edtCode.getText().toString();

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        signInWithPhoneAuthCredential(credential);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("TAG", "On Start iniciado");

        //Comprobar si tenemos permiso para acceder a los contactos
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            //Pedir permiso para acceder a los contactos
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[] {Manifest.permission.READ_CONTACTS},
                    PERMISO_CONTACTOS);
        } else {
            // Comprobar si el usuario ya se registró
            FirebaseUser currentUser = mAuth.getCurrentUser();

            updateUI(currentUser);
        }

    }

    private void validar() {
        //Pasar a la aplicación
        Log.d("TAG", "Pasar a la aplicación");
        Intent i = new Intent(this, ChatMain.class);
        startActivity(i);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "Registro con código correcto");

                            FirebaseUser userFB = task.getResult().getUser();

                            updateUI(userFB);
                        } else {
                            // Sign in failed, display a message and update the UI
                            //Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Log.d("TAG", "Error en la validación del código");
                                showError("Error en el registro del usuario");
                            }
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser userFB) {
        if (userFB == null) {
            //Ocultar imagen de carga para que se muestre el registro
            imgBackground.setVisibility(View.INVISIBLE);
            Log.d("TAG", "Empezar registro");
        } else {
            //Comprobar si ya está el usuario guardado en BD
            modelo.userId = userFB.getUid();
            readUserFromDB(modelo.userId);

        }
    }

    private void showError(String error) {
        Toast.makeText( this , error, Toast.LENGTH_SHORT).show();
    }

    private void readUserFromDB(String userId) {
        Log.d("TAG", "Leyendo usuario de la BD");
        db.collection("users").document(userId).get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        //Usuario existe en BD. Pasar a App
                        Log.d("TAG", "Usuario ya existe en BD");
                        modelo.user = (User) document.toObject(User.class);
                        cargarContactos();
                    } else {
                        //Usuario NO existe en BD. Guardarlo
                        //Log.d("TAG", "Error al leer teléfono de la BD");
                        Log.d("TAG", "Usuario No existe en BD");
                        saveUserInDB(userId);
                    }
                } else {
                    Log.d("TAG", "Error al leer de la BD: "
                            + task.getException().getMessage());
                }
            }
        });

    }

    private void saveUserInDB(String userId) {
        String phone = "+34" + edtPhone.getText().toString();
        phone = PhoneNumberUtils.formatNumber(phone, "ES");

        User user = new User(phone);

        db.collection("users").document(userId).set(user)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    modelo.user = user;
                    cargarContactos();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("TAG", "Error al guardar usuario en BD: "
                        + e.getMessage().toString());
                }
            });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISO_CONTACTOS) {

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Comprobar si el usuario ya se registró
                FirebaseUser currentUser = mAuth.getCurrentUser();

                updateUI(currentUser);
            }

        }
    }

    @SuppressLint("Range")
    private void cargarContactos() {
        //Cargar contactos del teléfono
        Log.d("TAG", "Cargando contactos...");
        ContentResolver cr = getContentResolver();
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

                        phonesList.add(phoneNo);

                    }
                    pCur.close();
                }
            }
        }
        if(cur!=null){
            cur.close();
        }

        //Cargar teléfonos de usuarios de la base de datos
        db.collection("users")
            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot doc : task.getResult()) {
                            phonesFB.add(doc.getString("phone"));
                    }
                    //Cargar contactos de la aplicación
                    for (String phoneFB : phonesFB) {
                        if (phonesList.contains(phoneFB)) {
                            modelo.contactos.add(phoneFB);
                        }
                    }
                    Log.d("TAG", "Contactos cargados de la BD");
                    cargarChats();
                } else {
                    Log.d("TAG", "Error cargando contactos de la BD");
                }
            }
        });
    }

    private void cargarChats() {
        //Cargar los chats para el usuario
        Log.d("TAG", "Cargando chats de la BD...");
        db.collection("users").document(modelo.userId).collection("chats")
            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        //recuperar ID de Chat
                        modelo.chats.add(doc.getString("chatWith"));
                    }
                    Log.d("TAG", "Chats cargados de la BD");
                    validar();
                } else {
                    Log.d("TAG", "Error cargando chats de la BD");
                }
            }
        });
    }

}