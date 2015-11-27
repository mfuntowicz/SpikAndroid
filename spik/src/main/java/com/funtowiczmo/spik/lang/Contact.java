package com.funtowiczmo.spik.lang;

/**
 * Created by momo- on 25/11/2015.
 */
public class Contact {

    private long id;
    private String name;
    private String phone;
    private byte[] photo;

    public Contact(long id, String name, String phone) {
        this(id, name, phone, null);
    }

    public Contact(long id, String name, String phone, byte[] photo) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.photo = photo;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String phone() {
        return phone;
    }


    public boolean hasPhoto() {
        return phone != null;
    }

    public byte[] photo() {
        return photo;
    }
}
