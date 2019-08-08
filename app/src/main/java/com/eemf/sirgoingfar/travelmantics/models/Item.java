package com.eemf.sirgoingfar.travelmantics.models;

public class Item {
    String id, label, desc, value, url;

    public Item() {
    }

    public Item(String label, String desc, String value) {
        this.label = label;
        this.desc = desc;
        this.value = value;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDesc() {
        return desc;
    }

    public String getValue() {
        return value;
    }

    public String getUrl() {
        return url;
    }
}
