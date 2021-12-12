package com.chemanu.mchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class NuevoChat extends AppCompatActivity {

    private Modelo modelo;

    private RecyclerView contactList;

    private FirebaseFirestore db;
    private StorageReference storageReference;
    private Utils utils;

    public ArrayList<User> phonesList = new ArrayList<User>();
    public ArrayList<User> usersFB = new ArrayList<User>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_chat);

        modelo = (Modelo) getApplication();
        utils = new Utils(this);

        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        contactList = (RecyclerView) findViewById(R.id.rvCotntactList);
        contactList.setLayoutManager(new LinearLayoutManager(this));
        contactList.setHasFixedSize(true);
        contactList.setAdapter(new ContactsAdapter(modelo.contactos));

        getSupportActionBar().setTitle("Contactos");
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

        private ArrayList<User> contactos;

        public ContactsAdapter(ArrayList<User> contactos) {
            this.contactos = contactos;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            public TextView txtName, txtState;
            public ImageView imgContact;

            public ViewHolder (Context context, View itemView) {
                super(itemView);

                txtName = (TextView) itemView.findViewById(R.id.txtContactName);
                txtState = (TextView) itemView.findViewById(R.id.txtState);
                imgContact = (ImageView) itemView.findViewById(R.id.imgContact);
            }

            @Override
            public void onClick(View v) {

            }
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.contact_list_item;
        }

        @Override
        public ContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();

            LayoutInflater inflater = LayoutInflater.from(context);
            View contactListItem = inflater.inflate(viewType, parent, false);

            ContactsAdapter.ViewHolder vh = new ContactsAdapter.ViewHolder(context, contactListItem);
            return vh;
        }

        @Override
        public void onBindViewHolder(ContactsAdapter.ViewHolder holder, int position) {

            User contact = contactos.get(position);

            holder.txtName.setText(contact.getName());
            holder.txtState.setText(contact.getState());
            String imagen = getApplicationContext().getFilesDir() + "/MChat/UserProfileImg/" + contact.getId() + ".png";
            holder.imgContact.setImageBitmap(utils.cargarImagen(imagen, 100, 100));

        }

        @Override
        public int getItemCount() {
            return contactos.size();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nuevo_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.back:
                onBackPressed();
                return true;
            case R.id.actualizar:
                actualizarContactos();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void actualizarContactos() {

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
                    contactList.getAdapter().notifyDataSetChanged();
                } else {
                    Log.d("TAG", "Error cargando contactos de la BD");
                }
            }
        });

    }
}
