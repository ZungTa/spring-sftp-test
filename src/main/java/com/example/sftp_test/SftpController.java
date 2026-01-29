package com.example.sftp_test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SftpController {

	private final SftpSampleService sftpSampleService;

	@GetMapping("/sftp")
	public void sftp() {
		sftpSampleService.sftp();
	}

}
