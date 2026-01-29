package com.example.sftp_test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SftpService {

	@Value("${sftp.host}")
	private String sftpHost;

	@Value("${sftp.port:22}")
	private int sftpPort;

	@Value("${sftp.user}")
	private String sftpUser;

	@Value("${sftp.password}")
	private String password;

	private SftpRemoteFileTemplate template;

	public void uploadFile(String filePath, String remoteDirectory) {
		if (template == null) {
			connect();
		}

		File file = new File(filePath);
		if (!file.exists()) {
			log.error("파일이 존재하지 않습니다: {}", filePath);
			return;
		}

		try {
			String remoteFilePath = Paths.get(remoteDirectory, file.getName()).toString().replace("\\", "/");
			template.execute(session -> {
				log.info("SFTP session connected: {}", session.isOpen());
				try {
					// 디렉토리 생성 (없을 경우) (여러 depth 폴더까지 전부 생성)
					session.mkdir(remoteDirectory);

					// FileInputStream으로 파일 스트림 열기
					try (FileInputStream fis = new FileInputStream(file)) {
						// 파일 업로드
						session.write(fis, remoteFilePath);
					}

					log.info("파일 업로드 성공: {}", remoteFilePath);
					return true;
				} catch (Exception e) {
					log.error("파일 업로드 실패: {}", e.getMessage(), e);
					throw new RuntimeException(e);
				}
			});

		} catch (Exception e) {
			log.error("SFTP 업로드 중 오류 발생: {}", e.getMessage(), e);
		}
	}

	private void connect() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		factory.setHost(sftpHost);
		factory.setPort(sftpPort);
		factory.setUser(sftpUser);
		factory.setPassword(password);
		factory.setAllowUnknownKeys(true);

		this.template = new SftpRemoteFileTemplate(new CachingSessionFactory<>(factory));

		// 디렉토리가 없으면 자동으로 생성하는 옵션
		this.template.setAutoCreateDirectory(true);
	}
}
