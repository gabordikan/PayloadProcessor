package com.abehrdigital.payloadprocessor.models;

public class PdfTextWithCoordinates {
    private float bottomLeftX;
    private float bottomLeftY;
    private float topRightX;
    private float topRightY;
    private String text;

    public PdfTextWithCoordinates(float bottomLeftX, float bottomLeftY, float topRightX, float topRightY, String text) {
        this.bottomLeftX = bottomLeftX;
        this.bottomLeftY = bottomLeftY;
        this.topRightX = topRightX;
        this.topRightY = topRightY;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public boolean coordinatesMatchText(double x, double y) {
        if(x >= bottomLeftX && x <= topRightX && y <= bottomLeftY && y >= topRightY) {
            return true;
        }

        return false;
    }
}
