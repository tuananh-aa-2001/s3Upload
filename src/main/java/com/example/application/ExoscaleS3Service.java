package com.example.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExoscaleS3Service {

    @Autowired
    private S3Client s3Client;

    public void uploadFile(String bucketName, String key, String filePath) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, Paths.get(filePath));
    }
    public String getUrl(String bucketName, String objectKey) {
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(objectKey)).toExternalForm();
    }

    public byte[] downloadResource(String bucketName,String objectKey){
        byte[] fileContent  = new byte[0];
        ResponseInputStream<GetObjectResponse> s3object = s3Client.getObject(builder -> builder.bucket(bucketName).key(objectKey));
        try {
            fileContent  = s3object.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileContent ;
    }

    public ResponseInputStream<GetObjectResponse> getObject(String bucketName, String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public List<String> listObjects(String bucketName) {
        ListObjectsRequest request = ListObjectsRequest.builder().bucket(bucketName).build();
        ListObjectsResponse response;
        try {
            response = s3Client.listObjects(request);
        } catch (NoSuchKeyException e){
            System.err.println(e.getMessage());
            return null;
        }

        List<S3Object> objects = response.contents();
        ListIterator<S3Object> listIterator = objects.listIterator();
        List<String> objectKeys = new ArrayList<>();
        while (listIterator.hasNext()) {
            S3Object object = listIterator.next();
            String encodedKey = urlEncode(object.key());
            objectKeys.add(encodedKey);
            System.out.println(encodedKey + " - " + object.size());
        }

        return objectKeys;
    }

    public void deleteObject(String bucketName, String objectKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    public Set<String> listBucketLists() {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets(listBucketsRequest);
        return listBucketsResponse.buckets().stream().map(Bucket::name).collect(Collectors.toSet());
    }

    public boolean doesObjectExistByListObjects(String bucketName, String key) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response listObjectsV2Response;
        try {
            listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        } catch (NoSuchKeyException e) {
            System.err.println(e.getMessage());
            return false;
        }
        return listObjectsV2Response.contents()
                .stream()
                .anyMatch(s3ObjectSummary -> s3ObjectSummary.getValueForField("Key", String.class)
                        .equals(key));
    }

    public String urlEncode(String key) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

}