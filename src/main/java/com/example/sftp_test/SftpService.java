package com.example.sftp_test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
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

	public void uploadFileOrDir(String filePathStr, String remoteDirectory) {
		if (template == null) {
			connect();
		}

		Path filePath = Paths.get(filePathStr);
		if (!Files.exists(filePath)) {
			log.error("파일 또는 디렉토리가 존재하지 않습니다: {}", filePathStr);
			return;
		}

		try {
			template.execute(session -> {
				log.info("SFTP session connected: {}", session.isOpen());
				try {
					// 디렉토리 생성 (없을 경우) (여러 depth 폴더까지 전부 생성)
					session.mkdir(remoteDirectory);
					log.info("디렉토리 생성: {}", remoteDirectory);

					if (Files.isDirectory(filePath)) {
						// 디렉토리 업로드
						String remoteDirPath = Paths.get(remoteDirectory, filePath.getFileName().toString())
							.toString()
							.replace("\\", "/");
						log.info("디렉토리 생성: {}", remoteDirPath);
						session.mkdir(remoteDirPath);
						uploadDirectoryContents(session, filePath.toFile(), remoteDirPath);
						log.info("디렉토리 업로드 성공: {} -> {}", filePathStr, remoteDirPath);
						return true;
					}

					// 파일 업로드
					String remoteFilePath = Paths.get(remoteDirectory, filePath.getFileName().toString())
						.toString()
						.replace("\\", "/");
					uploadSingleFile(session, filePath.toFile(), remoteFilePath);
					log.info("파일 업로드 성공: {}", remoteFilePath);
					return true;
				} catch (Exception e) {
					log.error("업로드 실패: {}", e.getMessage(), e);
					throw new RuntimeException(e);
				}
			});

		} catch (Exception e) {
			log.error("SFTP 업로드 중 오류 발생: {}", e.getMessage(), e);
		}
	}

	private void uploadDirectoryContents(
		Session<SftpClient.DirEntry> session,
		File localDir,
		String remoteParentDir
	) throws IOException {
		File[] files = localDir.listFiles();
		if (files == null) {
			return;
		}

		for (File file : files) {
			String remotePath = Paths.get(remoteParentDir, file.getName()).toString().replace("\\", "/");

			if (file.isDirectory()) {
				// 하위 디렉토리 생성
				session.mkdir(remotePath);
				log.info("디렉토리 업로드 중 - 하위 디렉토리 생성: {}", remotePath);
				// 재귀적으로 하위 디렉토리 내용 업로드
				uploadDirectoryContents(session, file, remotePath);
				continue;
			}

			// 파일 업로드
			uploadSingleFile(session, file, remotePath);
			log.info("디렉토리 업로드 중 - 파일 업로드 성공: {}", remotePath);
		}
	}

	private void uploadSingleFile(Session<SftpClient.DirEntry> session, File file, String remotePath) throws
		IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			session.write(fis, remotePath);
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
