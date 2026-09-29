package com.facturx.app.extraction;

import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin HTTP wrapper around the internal Python extractor (F11). The extractor
 * has no auth of its own - by the time this is called, the controller has
 * already checked the user, the organisation and the document's permissions.
 */
@Component
public class ExtractionClient {

    private static final Logger log = LoggerFactory.getLogger(ExtractionClient.class);

    private final RestClient restClient;

    public ExtractionClient(@Value("${app.extractor.url}") String extractorUrl) {
        // Pin HTTP/1.1: the JDK HttpClient defaults to attempting an h2c (cleartext
        // HTTP/2) upgrade, which uvicorn/python-multipart does not handle - it still
        // replies, but fails to parse the chunked multipart body ("file" reported as
        // missing even though it was sent). Plain HTTP/1.1 avoids the upgrade dance.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(extractorUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    public ExtractionResponse extract(byte[] file, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        try {
            // No explicit .contentType(MULTIPART_FORM_DATA): the FormHttpMessageConverter
            // derives it from this MultiValueMap body and generates the boundary itself.
            return restClient.post()
                    .uri("/extract")
                    .body(body)
                    .retrieve()
                    .body(ExtractionResponse.class);
        } catch (RestClientException e) {
            log.error("Extraction call failed", e);
            throw new ExtractionFailedException(
                    "Le service d'extraction est indisponible ou a échoué à traiter ce document.", e);
        }
    }
}
