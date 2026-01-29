package com.example.sftp_test;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SftpSampleService {

	private final SftpService sftpService;

	public void sftp() {
		log.info("sftp sample");

		String localFilePath = "C:\\Users\\user\\Documents\\dev\\temp\\asd.pdf";
		String remoteDirectory = "/ntle/asset/";
		sftpService.uploadFile(localFilePath, remoteDirectory);
	}
}
