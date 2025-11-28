package com.trend_now.backend.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host}")
    private String host;

    @Value("${opensearch.port}")
    private int port;

    @Value("${opensearch.schema}")
    private String schema;

    @Bean
    public OpenSearchClient openSearchClient() {
        RestClient restClient = RestClient.builder(new HttpHost(host, port, schema)).build();
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        return new OpenSearchClient(transport);
    }
}
