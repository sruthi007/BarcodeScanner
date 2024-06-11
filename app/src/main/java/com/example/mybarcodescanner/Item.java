package com.example.mybarcodescanner;

public class Item {
    private String barcode;
    private String name;
    private String mfdDate;
    private String expiryDate;

    public Item() {
        // Default constructor required for calls to DataSnapshot.getValue(Item.class)
    }

    public Item(String barcode, String name, String mfdDate, String expiryDate) {
        this.barcode = barcode;
        this.name = name;
        this.mfdDate = mfdDate;
        this.expiryDate = expiryDate;
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

    public String getMfdDate() {
        return mfdDate;
    }

    public void setMfdDate(String mfdDate) {
        this.mfdDate = mfdDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
}
