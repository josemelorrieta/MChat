package com.chemanu.mchat;

import java.util.ArrayList;

public class User {

    private String id, name, phone, state;
    private ArrayList<String> chats;

    public User () {}

    public User(String id, String name, String phone, String state) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.state = state;
    }

    public User(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ArrayList<String> getChats() {
        return chats;
    }

    public void setChats(ArrayList<String> chats) {
        this.chats = chats;
    }
}
