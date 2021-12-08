package com.chemanu.mchat;

import android.app.Application;

import java.util.ArrayList;

public class Modelo extends Application {

    ArrayList<User> contactos = new ArrayList<User>();
    ArrayList<String> chats = new ArrayList<String>();
    String  userId;

    User user;

}
