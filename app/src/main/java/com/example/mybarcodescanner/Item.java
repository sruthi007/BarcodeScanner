package com.example.mybarcodescanner;

public class Item {
    private String barcode;
    private String name;

    public Item() {
        // Default constructor required for calls to DataSnapshot.getValue(Item.class)
    }

    public Item(String barcode, String name) {
        this.barcode = barcode;
        this.name = name;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
