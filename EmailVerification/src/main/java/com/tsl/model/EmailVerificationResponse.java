package com.tsl.model;

import lombok.Data;

@Data
public class EmailVerificationResponse {

    private boolean isSyntaxValid;
    private boolean isDomainValid;
    private boolean isSmtpValid;
    private String remarks;
	
	
}
