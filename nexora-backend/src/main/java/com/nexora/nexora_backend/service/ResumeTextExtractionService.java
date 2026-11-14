package com.nexora.nexora_backend.service;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class ResumeTextExtractionService {

    public ExtractionResult extractText(Path pdfPath)
            throws IOException {

        try (PDDocument document =
                     Loader.loadPDF(pdfPath.toFile())) {

            if (document.isEncrypted()) {
                throw new IOException(
                        "Password-protected PDFs are not supported"
                );
            }

            PDFTextStripper textStripper = new PDFTextStripper();

            String extractedText = textStripper
                    .getText(document)
                    .trim();

            if (extractedText.isBlank()) {
                throw new IOException(
                        "No readable text found in the PDF"
                );
            }

            return new ExtractionResult(
                    extractedText,
                    document.getNumberOfPages()
            );
        }
    }

    public record ExtractionResult(
            String extractedText,
            int pageCount
    ) {}
}