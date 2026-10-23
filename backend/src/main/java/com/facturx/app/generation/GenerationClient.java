package com.facturx.app.generation;

import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * F13 : appel du service Python interne (meme conteneur que l'extraction F11) qui
 * produit le fichier Factur-X. Comme pour l'extraction, les droits ont deja ete
 * verifies cote Java avant l'appel.
 */
@Component
public class GenerationClient {

    private static final Logger log = LoggerFactory.getLogger(GenerationClient.class);

    private final RestClient restClient;

    public GenerationClient(@Value("${app.extractor.url}") String extractorUrl) {
        // HTTP/1.1 force : voir ExtractionClient (uvicorn ne gere pas l'upgrade h2c).
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(extractorUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    /** Retourne le PDF Factur-X genere. */
    public byte[] generate(GenerationPayload payload) {
        try {
            byte[] pdf = restClient.post()
                    .uri("/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(byte[].class);
            if (pdf == null || pdf.length == 0) {
                throw new GenerationFailedException("Le service de génération a renvoyé un fichier vide.", null);
            }
            return pdf;
        } catch (RestClientException e) {
            log.error("Generation call failed", e);
            throw new GenerationFailedException(
                    "Le service de génération est indisponible ou a échoué à produire la facture.", e);
        }
    }
}
