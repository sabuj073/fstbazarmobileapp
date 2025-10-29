package com.app.fstbazar;


public class SiteModel {
    private String name, logo, link;

    public SiteModel(String name, String logo, String link) {
        this.name = name;
        this.logo = logo;
        this.link = link;
    }

    public String getName() { return name; }
    public String getLogo() { return logo; }
    public String getLink() { return link; }
}

