package com.huaan.doorbar.model;

/**
 * @description
 * @author: litrainy
 * @create: 2019-05-07 13:41
 **/
public class PeopleFaceInfo {
    private String faceImage;
    private String name;
    private String id;

    public PeopleFaceInfo(String faceImage, String name, String id) {
        this.faceImage = faceImage;
        this.name = name;
        this.id = id;
    }

    public String getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(String faceImage) {
        this.faceImage = faceImage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
