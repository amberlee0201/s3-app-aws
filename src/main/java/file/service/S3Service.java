package file.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;

import file.entity.AttachmentFile;
import file.repository.AttachmentFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {

    private final AmazonS3 amazonS3;
    private final AttachmentFileRepository fileRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final String DIR_NAME = "s3_data";

    // 파일 업로드
    @Transactional
    public void uploadS3File(MultipartFile file) throws Exception {
        log.info("upload file");

        if (file == null) {
            throw new Exception("파일 전달 오류 발생");
        }

        // DB 저장
        String filePath = "/Users/ksj/CloudEngineering/97.data/" + DIR_NAME;
        String attachmentOriginalFileName = file.getOriginalFilename();
        UUID uuid = UUID.randomUUID();
        String attachmentFileName = uuid + "_" + attachmentOriginalFileName;
        Long attachmentFileSize = file.getSize();

        AttachmentFile attachmentFile = AttachmentFile.builder()
                .filePath(filePath)
                .attachmentOriginalFileName(attachmentOriginalFileName)
                .attachmentFileName(attachmentFileName)
                .attachmentFileSize(attachmentFileSize)
                .build();

        Long attachmentFileNo = fileRepository.save(attachmentFile).getAttachmentFileNo();

        if (attachmentFileNo != null) {
            // C:/CE/97.data/s3_data에 파일 저장 -> S3 전송 및 저장 (putObject)
            File uploadFile = new File(attachmentFile.getFilePath() + File.separator + attachmentFileName);
            log.info(uploadFile.getAbsolutePath());
            file.transferTo(uploadFile);

			// S3 전송 및 저장 (putObject)
			// bucket name
			// key: bucket 내부에 객체가 저장되는 경로 + 파일(객체)명
			// file

			// 임시적으로 파일을 물리적으로 존재하지 않으면 전달할 수 없음
			String s3Key = DIR_NAME + "/" + uploadFile.getName();
			log.info(s3Key);
            amazonS3.putObject(bucketName, s3Key, uploadFile);

			// delete local file
			if (uploadFile.exists()) {
				uploadFile.delete();
			}
        }
    }

    // 파일 다운로드
    @Transactional
    public ResponseEntity<Resource> downloadS3File(long fileNo) {
        AttachmentFile attachmentFile = null;
        Resource resource = null;

        try {
            // DB에서 파일 검색 -> S3의 파일 가져오기 (getObject) -> 전달
            attachmentFile = fileRepository.findById(fileNo).orElseThrow(() -> new NoSuchElementException("파일 없음!"));

            S3Object s3Object = amazonS3.getObject(bucketName, DIR_NAME + "/" + attachmentFile.getAttachmentFileName());

            S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
            resource = new InputStreamResource(s3ObjectInputStream);

        } catch (NoSuchElementException | SdkClientException e) {
            return new ResponseEntity<>(resource, HttpStatus.NO_CONTENT);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", attachmentFile.getAttachmentOriginalFileName());

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

}