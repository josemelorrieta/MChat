package com.chemanu.mchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    private Modelo modelo;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;

    private EditText edtName,edtPhone, edtCode;
    private Button btnRegistrar;
    private AlertDialog avisoDatos;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        modelo = (Modelo) getApplication();

        edtName = (EditText) findViewById(R.id.edtName);
        edtPhone = (EditText) findViewById(R.id.editPhone);
        edtCode = (EditText) findViewById(R.id.edtCode);
        btnRegistrar = (Button) findViewById(R.id.btnRegistrar);

        btnRegistrar.setEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
        if (edtName.getText().toString().equals("") ||
            edtPhone.getText().toString().equals("")) {
            avisoDatos = new AlertDialog.Builder(RegisterActivity.this)
                    .setMessage("Los campos de nombre y teléfono no pueden estar vacíos")
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create();
            avisoDatos.show();
        } else {
            String phoneNumber = "+34" + edtPhone.getText().toString();

            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(mCallBacks)
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        }
    }

    public void verifyPhoneNumberWithCode(View view) {
        String code = edtCode.getText().toString();

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "Registro con código correcto");
                            modelo.userName = edtName.getText().toString();
                            modelo.userPhone = edtPhone.getText().toString();
                            saveUserInDB();
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

    private void saveUserInDB() {
        String userId = mAuth.getCurrentUser().getUid();

        String phone = modelo.userPhone;
        phone = PhoneNumberUtils.formatNumber(phone, "ES");

        String state = "Hi, i'm using MChat";

        User user = new User(userId, modelo.userName, phone, state);

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        finish();
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

    private void showError(String error) {
        Toast.makeText( this , error, Toast.LENGTH_SHORT).show();
    }
}