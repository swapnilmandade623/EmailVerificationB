package com.tsl.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailVerifierService {

    // Method to verify emails from an uploaded Excel file
    public List<Map<String, String>> verifyEmailsFromExcel(InputStream inputStream) {
        List<Map<String, String>> emailResults = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Iterate over rows
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                Cell emailCell = row.getCell(0);
                if (emailCell != null && emailCell.getCellType() == CellType.STRING) {
                    String email = emailCell.getStringCellValue().trim();
                    if (!email.isEmpty() && email.contains("@")) {
                        boolean isValid = verifyEmail(email);

                        Map<String, String> result = new HashMap<>();
                        result.put("email", email);
                        result.put("status", isValid ? "Valid" : "Invalid");
                        emailResults.add(result);
                    } else {
                        System.out.println("Invalid email format in row " + row.getRowNum());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading Excel file: " + e.getMessage());
        }

        return emailResults;
    }

    // Verify a single email
    public boolean verifyEmail(String email) {
        if (email == null || !email.contains("@")) {
            System.err.println("Invalid email format: " + email);
            return false;
        }

        String domain = email.substring(email.indexOf("@") + 1);

        try {
            List<String> mxRecords = getMXRecords(domain);
            if (mxRecords == null || mxRecords.isEmpty()) {
                System.out.println("No MX Records found for: " + domain);
                return false;
            }

            for (String mxRecord : mxRecords) {
                if (checkWithSMTP(mxRecord, email)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error verifying email: " + e.getMessage());
            return false;
        }
    }

    // Get MX records for a domain
    private List<String> getMXRecords(String domain) {
        try {
            Record[] records = new Lookup(domain, Type.MX).run();
            if (records == null || records.length == 0) {
                return null;
            }

            List<String> mxRecords = new ArrayList<>();
            for (Record record : records) {
                MXRecord mx = (MXRecord) record;
                mxRecords.add(mx.getTarget().toString());
            }
            return mxRecords;
        } catch (Exception e) {
            System.err.println("Error retrieving MX records for " + domain + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Check email validity using SMTP
    private boolean checkWithSMTP(String smtpHost, String email) {
        try (Socket socket = new Socket(smtpHost, 25);
             InputStream reader = socket.getInputStream();
             OutputStream writer = socket.getOutputStream()) {

            // Read server response
            String response = readResponse(reader);
            if (!response.startsWith("220")) {
                System.out.println("Connection failed with server: " + smtpHost);
                return false;
            }

            // Send HELO/EHLO command
            sendCommand(writer, "HELO " + InetAddress.getLocalHost().getHostName());
            readResponse(reader);

            // Send MAIL FROM command
            sendCommand(writer, "MAIL FROM:<valid@example.com>");
            readResponse(reader);

            // Send RCPT TO command
            sendCommand(writer, "RCPT TO:<" + email + ">");
            response = readResponse(reader);

            return response.startsWith("250");

        } catch (Exception e) {
            System.err.println("SMTP connection failed: " + e.getMessage());
            return false;
        }
    }

    private void sendCommand(OutputStream writer, String command) throws Exception {
        writer.write((command + "\r\n").getBytes());
        writer.flush();
    }

    private String readResponse(InputStream reader) throws Exception {
        StringBuilder response = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            response.append((char) ch);
            if (response.toString().endsWith("\r\n")) {
                break;
            }
        }
        return response.toString();
    }
}
//package com.tsl.service;
//
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.springframework.stereotype.Service;
//import org.xbill.DNS.Lookup;
//import org.xbill.DNS.MXRecord;
//import org.xbill.DNS.Record;
//import org.xbill.DNS.Type;
//
//import java.io.*;
//import java.net.InetAddress;
//import java.net.Socket;
//import java.util.*;
//
//@Service
//public class EmailVerifierService {
//
//    // Method to verify emails from an uploaded Excel file
//    public ByteArrayOutputStream verifyEmailsFromExcel(InputStream inputStream) {
//        List<Map<String, String>> emailResults = new ArrayList<>();
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
//        try (Workbook workbook = WorkbookFactory.create(inputStream);
//             Workbook resultWorkbook = new XSSFWorkbook()) {
//            
//            Sheet sheet = workbook.getSheetAt(0);
//            Sheet resultSheet = resultWorkbook.createSheet("Email Verification Result");
//
//            // Copy header row
//            Row headerRow = resultSheet.createRow(0);
//            Cell headerCell = headerRow.createCell(0);
//            headerCell.setCellValue("Email");
//            headerCell = headerRow.createCell(1);
//            headerCell.setCellValue("Status");
//            headerCell = headerRow.createCell(2);
//            headerCell.setCellValue("Comment");
//
//            // Iterate over rows and validate emails
//            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
//                Row row = sheet.getRow(i);
//                if (row != null) {
//                    Cell emailCell = row.getCell(0);
//                    if (emailCell != null && emailCell.getCellType() == CellType.STRING) {
//                        String email = emailCell.getStringCellValue().trim();
//                        String status = "Invalid";
//                        String comment = "Email format invalid";
//
//                        if (!email.isEmpty() && email.contains("@")) {
//                            boolean isValid = verifyEmail(email);
//                            if (isValid) {
//                                status = "Valid";
//                                comment = "Email exists";
//                            } else {
//                                status = "Unsafe";
//                                comment = "No MX records or failed SMTP check";
//                            }
//                        }
//
//                        // Write to result sheet
//                        Row resultRow = resultSheet.createRow(i);
//                        resultRow.createCell(0).setCellValue(email);
//                        resultRow.createCell(1).setCellValue(status);
//                        resultRow.createCell(2).setCellValue(comment);
//                    }
//                }
//            }
//
//            // Write the result workbook to the output stream
//            resultWorkbook.write(outputStream);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return outputStream;
//    }
//
//    // Verify a single email
//    public boolean verifyEmail(String email) {
//        if (email == null || !email.contains("@")) {
//            return false;
//        }
//
//        String domain = email.substring(email.indexOf("@") + 1);
//
//        try {
//            List<String> mxRecords = getMXRecords(domain);
//            if (mxRecords == null || mxRecords.isEmpty()) {
//                return false;
//            }
//
//            for (String mxRecord : mxRecords) {
//                if (checkWithSMTP(mxRecord, email)) {
//                    return true;
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error verifying email: " + e.getMessage());
//        }
//        return false;
//    }
//
//    // Get MX records for a domain
//    private List<String> getMXRecords(String domain) {
//        List<String> mxRecords = new ArrayList<>();
//        try {
//            Lookup lookup = new Lookup(domain, Type.MX);
//            Record[] records = lookup.run();
//
//            if (records == null || records.length == 0) {
//                return mxRecords; // Return empty list if no MX records
//            }
//
//            for (Record record : records) {
//                if (record instanceof MXRecord) {
//                    MXRecord mx = (MXRecord) record;
//                    mxRecords.add(mx.getTarget().toString());
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving MX records for domain " + domain + ": " + e.getMessage());
//        }
//        return mxRecords;
//    }
//
//    // Check email validity using SMTP
//    private boolean checkWithSMTP(String smtpHost, String email) {
//        try (Socket socket = new Socket(smtpHost, 25);
//             InputStream reader = socket.getInputStream();
//             OutputStream writer = socket.getOutputStream()) {
//
//            String response = readResponse(reader);
//            if (!response.startsWith("220")) {
//                return false;
//            }
//
//            sendCommand(writer, "HELO " + InetAddress.getLocalHost().getHostName());
//            readResponse(reader);
//
//            sendCommand(writer, "MAIL FROM:<valid@example.com>");
//            readResponse(reader);
//
//            sendCommand(writer, "RCPT TO:<" + email + ">");
//            response = readResponse(reader);
//
//            return response.startsWith("250");
//
//        } catch (Exception e) {
//            System.err.println("SMTP connection failed: " + e.getMessage());
//            return false;
//        }
//    }
//
//    private void sendCommand(OutputStream writer, String command) throws Exception {
//        writer.write((command + "\r\n").getBytes());
//        writer.flush();
//    }
//
//    private String readResponse(InputStream reader) throws Exception {
//        StringBuilder response = new StringBuilder();
//        int ch;
//        while ((ch = reader.read()) != -1) {
//            response.append((char) ch);
//            if (response.toString().endsWith("\r\n")) {
//                break;
//            }
//        }
//        return response.toString();
//    }
//}
