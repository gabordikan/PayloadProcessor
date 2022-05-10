package com.abehrdigital.payloadprocessor.utils;

import com.abehrdigital.payloadprocessor.models.PdfTextWithCoordinates;
import com.abehrdigital.payloadprocessor.models.PdfTextWithCoordinatesStripper;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.sql.Blob;
import java.sql.SQLException;

public class PDFUtils {
    public static PDDocument extractPdfDocumentFromBytes(byte[] binaryPDF) throws IOException { //TODO: Check if PDDocument.load() solves this instead
        PDFParser pdfParser = null;
        PDDocument pdDocument = null;

        RandomAccessRead pdfData = new RandomAccessBuffer(binaryPDF);

        try {
            pdfParser = new PDFParser(pdfData);
            pdfParser.parse();
            pdDocument = pdfParser.getPDDocument();
        } catch (IOException ex) {
            throw ex;
        }
        return pdDocument;
    }

    public static PDDocument extractPdfDocumentFromBlob(Blob binaryBlop) throws SQLException, IOException {
        int blopLength = (int) binaryBlop.length();
        return extractPdfDocumentFromBytes(binaryBlop.getBytes(1, blopLength));
    }

    public static byte[] extractByteArrayFromBlop(Blob binaryBlop) throws SQLException {
        int blopLength = (int) binaryBlop.length();
        return binaryBlop.getBytes(1, blopLength);
    }

    public static PDDocument extractPdfDocumentFromBlob(byte[] binaryBlop) throws SQLException, IOException {
        return extractPdfDocumentFromBytes(binaryBlop);
    }

    public static void savePdf(PDDocument pdf, String filepath) {
        try {
            pdf.save(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //returns the width of the page in points (1pt = 1/72")
    public static double pageWidth(PDPage page) {
        return page.getMediaBox().getWidth();
    }

    //returns the height of the page in points (1pt = 1/72")
    public static double pageHeight(PDPage page) {
        return page.getMediaBox().getHeight();
    }

    public static String getTextFromCoordinate(double x, double y, byte[] pdf) throws IOException {
        PDDocument document = PDDocument.load(pdf);
        PdfTextWithCoordinatesStripper stripper = new PdfTextWithCoordinatesStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(0);
        stripper.setEndPage(document.getNumberOfPages());

        Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        stripper.writeText(document, dummy);

        for (PdfTextWithCoordinates pdfTextWithCoordinates : stripper.pdfTextsWithCoordinates) {
            if (pdfTextWithCoordinates.coordinatesMatchText(x, y)) {
                return pdfTextWithCoordinates.getText();
            }
        }

        return null;
    }
}