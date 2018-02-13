package com.epam.catgenome.util.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DownloadObject {

    private String AccessKeyID = "xxxxxxxxxxxxxxxxxxxx";
    private String SecretAccessKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static String bucketName = "documentcontainer";
    private static String keyName = "test";

    BasicAWSCredentials credentials = new BasicAWSCredentials(AccessKeyID, SecretAccessKey);


    void downloadFile() throws IOException {

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

        try {
            System.out.println("Downloading an object...");
            S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, keyName));

            System.out.println("Content-Type: " + s3object.getObjectMetadata().getContentType());
            InputStream input = s3object.getObjectContent();


        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which" +
                    " means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());

        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means" +
                    " the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());

        }

    }
}