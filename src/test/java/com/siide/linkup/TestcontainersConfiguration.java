package com.siide.linkup;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    }

    /**
     * MinIO container for the profile photo storage. Starts on a random port and
     * exposes its S3 endpoint via the {@code linkup.profile.storage.*} properties
     * so {@link com.siide.linkup.feature.profile.infrastructure.storage.MinioConfig}
     * can connect at boot.
     */
    @Bean
    public MinIOContainer minioContainer(ConfigurableEnvironment env) {
        MinIOContainer container = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
                .withUserName("testminio")
                .withPassword("testminio123");
        container.start();
        env.getPropertySources().addFirst(new MapPropertySource("linkup-minio-it", Map.of(
                "linkup.profile.storage.endpoint",   container.getS3URL(),
                "linkup.profile.storage.access-key", container.getUserName(),
                "linkup.profile.storage.secret-key", container.getPassword(),
                "linkup.profile.storage.bucket",     "linkup-profiles-it"
        )));
        return container;
    }
}
