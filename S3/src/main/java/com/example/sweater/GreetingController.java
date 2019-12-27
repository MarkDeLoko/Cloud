package com.example.sweater;


import java.io.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.services.codecommit.model.File;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping("/buckets")
class MainController {
    private static final String accesskey = "mrG4naV9D4wQKpbhKVULzV";
    private static final String secret = "9nHJhYGd38w4FExPjE5JDWQLM4y27ZSRPz2cxv7hVfjN";
    private static AmazonS3 s3;

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


    @RequestMapping (value = "/upload", method = RequestMethod.POST)
    void addObject(@RequestParam("file") MultipartFile file,
                   @RequestParam(name = "bucketName", defaultValue = "randomBucketName") String bucketName)
            throws IOException {
        {
            System.out.println(bucketName);
            File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
            file.transferTo(convFile);
            String objectKey = file.getOriginalFilename();
            PutObjectResult result = s3.putObject(bucketName, objectKey, convFile);
            //return result.getContentMd5();
        }


    }
}