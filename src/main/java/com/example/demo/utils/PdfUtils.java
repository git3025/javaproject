package com.example.demo.utils;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

public class PdfUtils {
    public static int getPdfPageCount(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.getNumberOfPages();
        }
    }
}