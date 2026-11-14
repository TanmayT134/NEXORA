package com.nexora.nexora_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.ResumeUploadResponse;
import com.nexora.nexora_backend.entity.Resume;
import com.nexora.nexora_backend.entity.ResumeStatus;
import com.nexora.nexora_backend.entity.User;
import com.nexora.nexora_backend.repository.ResumeRepository;
import com.nexora.nexora_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeTextExtractionService textExtractionService;

    @Value("${app.file.upload-dir}")
    private String uploadDirectory;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Transactional
    public ResumeUploadResponse uploadResume(
            String email,
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please select a resume to upload"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resume file size must not exceed 10 MB"
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PDF files are allowed"
            );
        }

        User user = userRepository.findByEmail(
                email.trim().toLowerCase(Locale.ROOT)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));

        String storedFilename = UUID.randomUUID() + ".pdf";

        Path uploadPath = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();

        Path destination = uploadPath.resolve(storedFilename).normalize();

        if (!destination.startsWith(uploadPath)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file destination"
            );
        }

        boolean fileStored = false;

        try {
            Files.createDirectories(uploadPath);

            // Verify the PDF file signature.
            try (InputStream inputStream = file.getInputStream()) {
                byte[] header = inputStream.readNBytes(5);

                if (!"%PDF-".equals(new String(
                        header,
                        java.nio.charset.StandardCharsets.US_ASCII))) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "The uploaded file is not a valid PDF"
                    );
                }
            }

            // Store the file using a generated filename.
            file.transferTo(destination);
            fileStored = true;

            ResumeTextExtractionService.ExtractionResult extractionResult;

            try {
                extractionResult = textExtractionService.extractText(destination);
            } catch (IOException exception) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unable to extract readable text from the resume"
                );
            }

            Resume resume = new Resume();

            resume.setUser(user);
            resume.setOriginalFilename(
                    Paths.get(originalFilename.replace("\\", "/"))
                            .getFileName()
                            .toString()
            );
            resume.setStoredFilename(storedFilename);
            resume.setStoragePath(destination.toString());
            resume.setContentType("application/pdf");
            resume.setFileSize(file.getSize());

            resume.setExtractedText(
                    extractionResult.extractedText()
            );

            resume.setPageCount(
                    extractionResult.pageCount()
            );

            resume.setProcessedAt(LocalDateTime.now());

            resume.setStatus(ResumeStatus.COMPLETED);

            Resume savedResume = resumeRepository.save(resume);

            return new ResumeUploadResponse(
                    savedResume.getId(),
                    savedResume.getOriginalFilename(),
                    savedResume.getContentType(),
                    savedResume.getFileSize(),
                    savedResume.getStatus(),
                    savedResume.getUploadedAt(),
                    "Resume uploaded successfully"
            );

        } catch (ResponseStatusException exception) {
            deleteStoredFile(destination, fileStored);
            throw exception;

        } catch (IOException exception) {
            deleteStoredFile(destination, fileStored);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to process the uploaded PDF"
            );

        } catch (RuntimeException exception) {
            deleteStoredFile(destination, fileStored);
            throw exception;
        }
    }

    private void deleteStoredFile(Path path, boolean fileStored) {
        if (fileStored) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                // Log this failure in a production logging setup.
            }
        }
    }
}