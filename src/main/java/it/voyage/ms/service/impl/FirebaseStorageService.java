package it.voyage.ms.service.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

@Service
public class FirebaseStorageService {

	private final Storage storage;
	//    private final String bucketName = "voyage-ed2d0.appspot.com";
	private final String bucketName = "voyage-ed2d0.firebasestorage.app";



	public FirebaseStorageService(Storage storage) {
		this.storage = storage;
	}


	public String uploadFile(MultipartFile file, String userId, String travelId, String category) throws IOException {
		String originalFileName = file.getOriginalFilename();

		String filePath = String.format("travel-files/%s/%s/%s/%s_%s", 
				userId, 
				travelId, 
				category, 
				UUID.randomUUID().toString(), 
				originalFileName);

		BlobId blobId = BlobId.of(bucketName, filePath);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
				.setContentType(file.getContentType())
				.setAcl(Collections.singletonList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
				.build();

		storage.create(blobInfo, file.getBytes());

		return filePath;
	}

	public String getPublicUrl(String fileId) {
		if (fileId == null || fileId.isEmpty()) {
			return null;
		}
		return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileId);
	}

}