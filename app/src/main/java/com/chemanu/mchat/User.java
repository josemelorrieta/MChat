package com.chemanu.mchat;

import java.util.ArrayList;

public class User {

    public String phone;
    public ArrayList<String> chats;

    public User() {

    };

    public User(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
      return this.phone;
    }

}
