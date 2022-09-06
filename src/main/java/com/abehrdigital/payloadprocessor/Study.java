package com.abehrdigital.payloadprocessor;

import com.abehrdigital.payloadprocessor.utils.DicomBlobUtils;
import com.abehrdigital.payloadprocessor.utils.PDFUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.dcm4che3.data.Sequence;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Map;

public class Study {

    private PDDocument pdfDoc;
    private byte[] attachmentBytes;
    private byte[] imageBytes;
    private Blob dicomBlob;

    private Map<String, String> nonSequenceDicomElements;
    private Map<Integer, Sequence> sequenceDicomElements;

    private static final String IMAGE_TYPE_FOR_MANUAL_EXTRACTION = "IJG (jpeg-6b) library with lossless patch";
    private static final String FILE_DESCRIPTION_FOR_MANUAL_EXTRACTION = "OphthalmicPhotography8BitImage";

    public Study() {

    }

    public String getHeaderJsonString() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(nonSequenceDicomElements);
    }

    public PDDocument getPdfDoc() {
        return pdfDoc;
    }

    public SerialBlob getAttachmentAsBlob() throws SQLException {
        return new SerialBlob(attachmentBytes);
    }

    public SerialBlob getImageAsBlob() throws SQLException, IOException {
        return DicomBlobUtils.convertDicomBlobToSingleImage(dicomBlob);
    }

    public SerialBlob getImageAsBlob(String imageDerivationDescription) throws SQLException, IOException {
        if(imageDerivationDescription.equals(IMAGE_TYPE_FOR_MANUAL_EXTRACTION) ||
                imageDerivationDescription.equals(FILE_DESCRIPTION_FOR_MANUAL_EXTRACTION)) {
            return new SerialBlob(attachmentBytes);
        } else {
            return getImageAsBlob();
        }
    }


    public SerialBlob getImagesAsPdfBlob() throws Exception {
        return DicomBlobUtils.convertDicomImagesToPdf(dicomBlob);
    }

    public void setDicomBlob(Blob dicomBlob) {
        this.dicomBlob = dicomBlob;
    }


    public void setAttachmentBytes(byte[] attachmentBytes) {
        this.attachmentBytes = attachmentBytes;
    }

    //Simple elements includes non-pdf and non-sequence elements only
    public void setSimpleDicomElements(Map<String, String> elements) {
        nonSequenceDicomElements = elements;
    }


    public void savePdf(String filepath) {
        PDFUtils.savePdf(pdfDoc, filepath);
    }

    // for testing - do not use in final project
    // TODO: consider using Linked HashMap to maintain order
    public String dumpData() {
        String output = "";
        for (Map.Entry<String, String> entry : nonSequenceDicomElements.entrySet()) {
            output += entry.getKey().toString() + " " + entry.getValue() + "\n";
        }
        return output;
    }

    public Boolean attachmentsCanBeExtracted() {
        if(attachmentBytes != null) {
            return true;
        } else {
            try {
                DicomBlobUtils.convertDicomBlobToSingleImage(dicomBlob);
                return true;

            } catch (Exception exception) {
                return false;
            }
        }
    }
}
