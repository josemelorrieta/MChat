package com.chemanu.mchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;


    private Modelo modelo;

    private AlertDialog avisoPermisos;

    private FirebaseFirestore db;

    private static final int PERMISO_CONTACTOS = 0;

    private ArrayList<User> phonesList = new ArrayList<User>();
    private ArrayList<User> usersFB = new ArrayList<User>();

    private Utils utils;

    final long ONE_MEGABYTE = 1024 * 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modelo = (Modelo) getApplication();
        utils = new Utils(this);

        mAuth = FirebaseAuth.getInstance();
        //mAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);

        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("TAG", "On Start iniciado");

        //Comprobar si tenemos los permisos para acceder a los contactos y al almacenamiento
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            //Pedir permiso para acceder a los contactos
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.READ_CONTACTS,
                                 Manifest.permission.READ_EXTERNAL_STORAGE,
                                 Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISO_CONTACTOS);
        } else {
            comprobarUsuario();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean permisoRechazado = false;

        if (requestCode == PERMISO_CONTACTOS) {
            for (int i = 0;i < grantResults.length;i++) {
                if (grantResults[i] == -1) {
                    permisoRechazado = true;
                }
            }

            if (permisoRechazado) {
                mostrarAvisoPermisos();
            } else {
                comprobarUsuario();
            }

        }
    }

    private void mostrarAvisoPermisos() {
        avisoPermisos = new AlertDialog.Builder(this)
            .setMessage("Los permisos requeridos son necesarios para el correcto " +
                    "funcionamiento de la aplicación.\nPor favor, acéptelos.")
            .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISO_CONTACTOS);
                }
            })
            .setNegativeButton("No aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cerrarDialogo();
                }
            }).create();

    }

    private void cerrarDialogo() {
        avisoPermisos.cancel();
    }

    private void comprobarUsuario() {
        // Comprobar si el usuario ya se registró
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            pasarARegistro();
        } else {
            modelo.userId = currentUser.getUid();
            readUserFromDB(modelo.userId);
        }
    }

    private void pasarARegistro() {
        //Pasar a registro
        Intent i = new Intent(this, RegisterActivity.class);
        startActivity(i);
    }

    private void validar() {
        File file = new File( getApplicationContext().getFilesDir(), "MChat/UserProfileImg");

        if(!file.exists()) {
            file.mkdirs();
        }

        //Pasar a la aplicación
        Log.d("TAG", "Pasar a la aplicación");
        Intent i = new Intent(this, ChatMain.class);
        startActivity(i);
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
                        modelo.userName = document.getString("name");
                        modelo.userPhone = document.getString("phone");
                        modelo.userState = document.getString("state");
                        cargarContactos();
                    } else {
                        pasarARegistro();
                    }
                } else {
                    Log.d("TAG", "Error al leer de la BD: "
                            + task.getException().getMessage());
                }
            }
        });

    }

    @SuppressLint("Range")
    private void cargarContactos() {
        //Cargar contactos del teléfono
        Log.d("TAG", "Cargando contactos...");

        phonesList = utils.recuperarContactostelefono();

        //Cargar teléfonos de usuarios de la base de datos
        db.collection("users")
            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot doc : task.getResult()) {
                        User userFB = doc.toObject(User.class);
                        userFB.setId(doc.getId());
                        usersFB.add(userFB);
                    }
                    //Cargar contactos de la aplicación
                    modelo.contactos = utils.generarContactosApp(phonesList, usersFB);

                    Log.d("TAG", "Contactos cargados de la BD");
                    descargarImagenesContactos(modelo.contactos);
                } else {
                    Log.d("TAG", "Error cargando contactos de la BD");
                }
            }
        });
    }

    private void descargarImagenesContactos(ArrayList<User> contactos) {
        for (User contacto : contactos) {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            StorageReference pathReference = storageReference.child("UserProfileImg/" + contacto.getId() + ".jpg");

            pathReference.getBytes(ONE_MEGABYTE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        final BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = false;
                        Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                        String filename = getApplicationContext().getFilesDir() + "/MChat/UserProfileImg/" + contacto.getId() + ".png";
                        try (FileOutputStream out = new FileOutputStream(filename)) {
                            image.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        }
        cargarChats();
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