package com.abehrdigital.payloadprocessor.models;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfTextWithCoordinatesStripper extends PDFTextStripper {

    public List<PdfTextWithCoordinates> pdfTextsWithCoordinates;

    public PdfTextWithCoordinatesStripper() throws IOException {
        pdfTextsWithCoordinates = new ArrayList<PdfTextWithCoordinates>();
    }

    /**
     * Override the default functionality of PDFTextStripper.writeString()
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        TextPosition firstTextPosition = textPositions.get(0);
        float bottomLeftX = firstTextPosition.getXDirAdj();
        float bottomLeftY = firstTextPosition.getYDirAdj();

        TextPosition lastTextPosition = textPositions.get(textPositions.size() - 1);

        float topRightX = lastTextPosition.getXDirAdj() + lastTextPosition.getWidthDirAdj();
        float topRightY = lastTextPosition.getYDirAdj() - lastTextPosition.getHeightDir();

        PdfTextWithCoordinates pdfTextWithCoordinates = new PdfTextWithCoordinates(bottomLeftX, bottomLeftY, topRightX, topRightY, string);
        pdfTextsWithCoordinates.add(pdfTextWithCoordinates);
    }
}
