package com.oves.app.entity;

import java.io.Serializable;

public class PhoneDomain implements Serializable {


    private String phoneNumber;

    private String name;


    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
