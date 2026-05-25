package com.siide.linkup.feature.profile.infrastructure.storage;

import com.siide.linkup.feature.profile.application.ProfileStorageProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Wires the AWS S3 SDK v2 against MinIO (dev) or AWS S3 (prod). Uses
 * path-style access because MinIO doesn't support virtual-host-style by default.
 * Ensures the photos bucket exists at boot — failing fast is preferable to
 * runtime 500s when the profile module is non-functional without storage.
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    private final ProfileStorageProperties properties;
    private final S3Client client;
    private final S3Presigner presigner;

    public MinioConfig(ProfileStorageProperties properties) {
        this.properties = properties;
        ProfileStorageProperties.Storage storage = properties.storage();
        AwsBasicCredentials creds = AwsBasicCredentials.create(storage.accessKey(), storage.secretKey());

        // forcePathStyle MUST live on exactly one of {builder, S3Configuration}, not both.
        // MinIO defaults to path-style — leave it on the builder. AWS S3 supports both.
        this.client = S3Client.builder()
                .endpointOverride(URI.create(storage.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.US_EAST_1) // MinIO ignores it; AWS overridden by env
                .forcePathStyle(true)
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(storage.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return client;
    }

    @Bean
    public S3Presigner s3Presigner() {
        return presigner;
    }

    @PostConstruct
    public void ensureBucket() {
        String bucket = properties.storage().bucket();
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.debug("S3 bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException missing) {
            try {
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created S3 bucket '{}'", bucket);
            } catch (BucketAlreadyOwnedByYouException raceLost) {
                // Concurrent boot of another node won — fine, just continue.
                log.debug("S3 bucket '{}' created concurrently", bucket);
            }
        }
    }
}
