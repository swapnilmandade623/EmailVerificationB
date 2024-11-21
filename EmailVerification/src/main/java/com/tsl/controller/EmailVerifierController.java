//package com.tsl.controller;
//
//import com.tsl.service.EmailVerifierService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/email")
//public class EmailVerifierController {
//
//    @Autowired
//    private EmailVerifierService emailVerifierService;
//    @GetMapping("/verify")
//    public ResponseEntity<String> verifyEmail(@RequestParam("email") String email) {
//        boolean isValid = emailVerifierService.verifyEmail(email);
//
//        if (isValid) {
//            return ResponseEntity.ok("valid: " + email);
//        } else {
//            return ResponseEntity.badRequest().body("Invalid: " + email);
//        }
//    }
//}
//package com.tsl.controller;
//
//import com.tsl.service.EmailVerifierService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//@RestController
//@RequestMapping("/email-verifier")
//public class EmailVerifierController {
//
//    @Autowired
//    private EmailVerifierService emailVerifierService;
//
//    @PostMapping("/verify")
//    public ResponseEntity<byte[]> verifyEmails(@RequestParam("file") MultipartFile file) {
//        try {
//            InputStream inputStream = file.getInputStream();
//            ByteArrayOutputStream result = emailVerifierService.verifyEmailsFromExcel(inputStream);
//
//            byte[] content = result.toByteArray();
//            HttpHeaders headers = new HttpHeaders();
//            headers.add("Content-Disposition", "attachment; filename=email_verification_result.xlsx");
//
//            return new ResponseEntity<>(content, headers, HttpStatus.OK);
//        } catch (IOException e) {
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//}
package com.tsl.controller;

import com.tsl.service.EmailVerifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
public class EmailVerifierController {

    @Autowired
    private EmailVerifierService emailVerifierService;

    
    @PostMapping("/verify")
    public ResponseEntity<List<Map<String, String>>> verifyEmailsFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            List<Map<String, String>> results = emailVerifierService.verifyEmailsFromExcel(file.getInputStream());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
