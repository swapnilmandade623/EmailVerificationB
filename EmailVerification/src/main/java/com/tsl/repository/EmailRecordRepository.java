package com.tsl.repository;


import com.tsl.model.EmailVerificationResponse;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRecordRepository extends JpaRepository<EmailVerificationResponse, Long> {
	

}


