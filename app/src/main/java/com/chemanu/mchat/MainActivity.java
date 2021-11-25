package com.chemanu.mchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;

    private EditText edtPhone, edtCode;
    private Button btnRegistrar;
    private ImageView imgBackground;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtPhone = findViewById(R.id.editPhone);
        edtCode = findViewById(R.id.edtCode);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        imgBackground = findViewById(R.id.imgBackground);

        btnRegistrar.setEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        mAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);

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
                    // Invlaid request
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

        // Comprobar si el usuario ya se registró
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null ) {
            //Pruebas de lectura y escritura
            readFromDB(new UserCallback() {
                @Override
                public void onCallBack(User user) {
                    String phone = "";

                    if (user != null) {
                        phone = user.phone.toString();
                    }
                    Log.d("TAG", phone);
                    updateUI(currentUser, phone);
                }
            });
        } else {
            updateUI(currentUser, "");
        }
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

                            updateUI(userFB, "");
                        } else {
                            // Sign in failed, display a message and update the UI
                            //Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Log.d("TAG", "Error en la validación del código");
                            }
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser userFB, String phone) {
        if (userFB == null) {
            //Ocultar imagen de carga para que se muestre el registro
            imgBackground.setVisibility(View.INVISIBLE);
            Log.d("TAG", "Empezar registro");
        } else {
            //Si no existe el teléfono en la base de datos lo guardamos
            if (phone == "") {
                Log.d("TAG", "Usuario ya registrado. Guardar en BD");
                User user = new User();
                user.phone = edtPhone.getText().toString();
                db.collection("users").add(user)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("TAG", "Usuario guardado en BD");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("TAG", "Error al guardar usuario en BD");
                    }
                });

            }

            //Pasar a la aplicación
            validar();
        }
    }

    private void showError(String error) {
        Toast.makeText( this , error, Toast.LENGTH_SHORT).show();
    }

    private void validar() {
        Intent i = new Intent(this, ChatMain.class);
        startActivity(i);

    }

    private void readFromDB(UserCallback userCallback) {
        Log.d("TAG", "Leyendo de la BD");
        db.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                User user = null;
                if (task.isSuccessful()) {
                    QuerySnapshot document = task.getResult();
                    if (!document.isEmpty()) {
                        for (DocumentSnapshot doc : document) {
                            user = doc.toObject(User.class);
                            Log.d("TAG", "Dato leído correctamente. Llamada al Callback");
                            userCallback.onCallBack(user);
                        }
                    }
                } else {
                    Log.d("TAG", "Error al leer teléfono de la BD");
                    Log.d("TAG", task.getException().getMessage().toString());
                    userCallback.onCallBack(user);
                }
            }
        });

    }

    public interface UserCallback {
        void onCallBack(User user);
    }

}