
package com.tsl.controller;

import com.tsl.service.EmailVerifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/emails")
public class EmailVerifierController {

    @Autowired
    private EmailVerifierService emailVerifierService;

    @PostMapping("/verify")
    public ResponseEntity<byte[]> verifyEmails(@RequestParam("file") MultipartFile file) {
        try {
            byte[] fileContent = emailVerifierService.verifyAndSaveEmails(file.getInputStream());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=verified_emails.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
