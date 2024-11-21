
package com.tsl.service;


import com.tsl.model.EmailVerificationResponse;
import com.tsl.repository.EmailRecordRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;

@Service
public class EmailVerifierService {

    @Autowired
    private EmailRecordRepository emailRecordRepository;

    public byte[] verifyAndSaveEmails(InputStream inputStream) {
        List<Map<String, String>> emailResults = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream);
             Workbook outputWorkbook = new XSSFWorkbook()) {
            Sheet inputSheet = workbook.getSheetAt(0);
            Sheet outputSheet = outputWorkbook.createSheet("Results");

            // Add header row to output sheet
            Row headerRow = outputSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Email");
            headerRow.createCell(1).setCellValue("Status");

            int rowIndex = 1; // Start after the header row
            for (Row row : inputSheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                Cell emailCell = row.getCell(0);
                if (emailCell != null && emailCell.getCellType() == CellType.STRING) {
                    String email = emailCell.getStringCellValue().trim();
                    boolean isValid = verifyEmail(email);

                    // Save to database
                    EmailVerificationResponse record = new EmailVerificationResponse();
                    record.setEmail(email);
                    record.setStatus(isValid ? "Valid" : "Invalid");
                    emailRecordRepository.save(record);

                    // Add to output sheet
                    Row outputRow = outputSheet.createRow(rowIndex++);
                    outputRow.createCell(0).setCellValue(email);
                    outputRow.createCell(1).setCellValue(isValid ? "Valid" : "Invalid");
                }
            }

            // Write workbook to a byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputWorkbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error processing Excel file", e);
        }
    }

    private boolean verifyEmail(String email) {
        // Email verification logic
        String domain = email.substring(email.indexOf("@") + 1);
        List<String> mxRecords = getMXRecords(domain);

        if (mxRecords == null || mxRecords.isEmpty()) {
            return false;
        }

        for (String mx : mxRecords) {
            if (checkWithSMTP(mx, email)) {
                return true;
            }
        }

        return false;
    }

    private List<String> getMXRecords(String domain) {
        try {
            Record[] records = new Lookup(domain, Type.MX).run();
            List<String> mxRecords = new ArrayList<>();
            for (Record record : records) {
                mxRecords.add(((MXRecord) record).getTarget().toString());
            }
            return mxRecords;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean checkWithSMTP(String smtpHost, String email) {
      try (Socket socket = new Socket(smtpHost, 25);
          InputStream reader = socket.getInputStream();
          OutputStream writer = socket.getOutputStream()) {

          String response = readResponse(reader);
          if (!response.startsWith("220")) {
              return false;
          }

          sendCommand(writer, "HELO " + InetAddress.getLocalHost().getHostName());
          readResponse(reader);

          sendCommand(writer, "MAIL FROM:<valid@example.com>");
          readResponse(reader);

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
