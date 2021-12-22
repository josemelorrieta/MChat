package com.chemanu.mchat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;

public class ChatMain extends AppCompatActivity {

    private Modelo modelo;

    private RecyclerView chatList;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_main);

        storage = FirebaseStorage.getInstance();

        modelo = (Modelo) getApplication();

        chatList = findViewById(R.id.rvChatList);
        chatList.setLayoutManager(new LinearLayoutManager(this));
        chatList.setHasFixedSize(true);
        chatList.setAdapter(new ChatsAdapter(modelo.chats));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_chat, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        //Minimizar la aplicación sin volver a la actividad ppal
        moveTaskToBack(true);
    }

    private class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {

        private ArrayList<String> chats;

        public ChatsAdapter(ArrayList<String> chats) {
            this.chats = chats;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            public TextView txtUserName;

            public ViewHolder (Context context, View itemView) {
                super(itemView);

                txtUserName = (TextView) itemView.findViewById(R.id.txtContactName);
            }

            @Override
            public void onClick(View v) {

            }
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.chat_list_item;
        }

        @Override
        public ChatsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();

            LayoutInflater inflater = LayoutInflater.from(context);
            View chatListItem = inflater.inflate(viewType, parent, false);

            ChatsAdapter.ViewHolder vh = new ChatsAdapter.ViewHolder(context, chatListItem);
            return vh;
        }

        @Override
        public void onBindViewHolder(ChatsAdapter.ViewHolder holder, int position) {
            String phoneNo = chats.get(position);

            holder.txtUserName.setText(phoneNo);

        }

        @Override
        public int getItemCount() {
            return chats.size();
        }
    }

    public void nuevoChat(View view) {
        Intent i = new Intent(this, NuevoChat.class);
        startActivity(i);
    }

}
