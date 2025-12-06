package com.trend_now.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host}")
    private String host;

    @Value("${opensearch.port}")
    private int port;

    @Value("${opensearch.schema}")
    private String schema;

    @Value("${opensearch.username:#{null}}")
    private String username;

    @Value("${opensearch.password}:#{null}}")
    private String password;

    @Bean
    public OpenSearchClient openSearchClient() {
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(host, port, schema));
        // 배포 환경 (AWS OpenSearch)
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            log.info("OpenSearchConfig - 배포 환경 설정 적용 완료");
        }
        RestClient restClient = restClientBuilder.build();
        RestClientTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());

        return new OpenSearchClient(transport);
    }
}
