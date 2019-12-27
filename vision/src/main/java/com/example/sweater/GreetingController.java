package com.example.sweater;


import java.io.*;
import okhttp3.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.services.codecommit.model.File;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

import java.util.HashMap;

@RestController
@RequestMapping("/buckets")
class MainController {
    private static final String accesskey = "mrG4naV9D4wQKpbhKVULzV";
    private static final String secret = "9nHJhYGd38w4FExPjE5JDWQLM4y27ZSRPz2cxv7hVfjN";
    private static AmazonS3 s3;
    private final static String SERVICE = "https://smarty.mail.ru";
    private final static String DETECT = "/api/v1/objects/detect";
    private final static String RECOGNIZE = "/api/v1/persons/recognize";
    private final static String TOKEN = "HGS3tyoUxREQbiCqi5LjekSsoBUn68SHzqefcGxetLwvBR3Ru";
    private final static String FILE_NAME = "people.jpeg";
    private final static String PROVIDER = "mcs";
    MainController() {
        ClientConfiguration config = new ClientConfiguration();
        config.setProtocol(Protocol.HTTP);
        s3 = new AmazonS3Client(new BasicAWSCredentials(accesskey, secret), config);
        S3ClientOptions options = new S3ClientOptions();
        options.setPathStyleAccess(true);
        s3.setS3ClientOptions(options);
        s3.setEndpoint("hb.bizmrg.com");

    }

    @GetMapping("/add")
    public Bucket addBucket(@RequestParam(name = "name", defaultValue = "randomBucketName") String bucketName) {
        if (!s3.doesBucketExist(bucketName)) {
            try {
                Bucket bucket = s3.createBucket(bucketName);
                return bucket;
            } catch (Exception e) {
            }
        }
        return null;
    }

    @GetMapping
    public List<Bucket> getBucketList() {
        List<Bucket> bucketList = s3.listBuckets();
        return bucketList;
    }

    @GetMapping("/object-list")
    List<S3ObjectSummary> getListObjects(@RequestParam(name = "name") String bucketName) {
        ListObjectsV2Result result = s3.listObjectsV2(bucketName);
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        return objects;
    }

    @DeleteMapping("/{name}/{key}")
    void deleteObject(@PathVariable("name") String bucketName, @PathVariable("key") String key) {
        try {
            s3.deleteObject(bucketName, key);
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    @DeleteMapping("{name}")
    void deleteBucket(@PathVariable("name") String bucketName) {
        ObjectListing objectListing = s3.listObjects(bucketName);
        while (true) {
            Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
            while (objIter.hasNext()) {
                s3.deleteObject(bucketName, objIter.next().getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        s3.deleteBucket(bucketName);
    }

    private Request buildRequest(File file) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(SERVICE + DETECT).newBuilder();
        urlBuilder.addQueryParameter("oauth_provider", PROVIDER);
        urlBuilder.addQueryParameter("oauth_token", TOKEN);

        String url = urlBuilder.build().toString();

        okhttp3.RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("meta", "{\"mode\":[\"pedestrian\"],\"images\":[{\"name\":\"file_0\"}]}")
                .addFormDataPart("file_0", FILE_NAME, okhttp3.RequestBody.create(MediaType.parse("multipart/form-data"), file))
                .build();

        return new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
    }

    @RequestMapping (value = "/upload", method = RequestMethod.POST)
    String addPhoto(@RequestParam("file") MultipartFile file)
            throws IOException, InterruptedException {
        {
            File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
            file.transferTo(convFile);
            Resource resourceCapture = new ClassPathResource(FILE_NAME);
            OkHttpClient client = new OkHttpClient();
            Request request = buildRequest(convFile);
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка: " + response);
            }
            return response.body().string();
        }


    }
}