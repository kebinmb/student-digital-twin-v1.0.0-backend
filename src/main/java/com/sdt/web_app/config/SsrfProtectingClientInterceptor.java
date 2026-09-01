package com.sdt.web_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

@Configuration
public class SsrfProtectingClientInterceptor {
    private static final Set<String> FORBIDDEN_HOSTS = Set.of(
            "169.254.169.254",
            "metadata.google.internal",
            "instance-data"
    );

    @Bean
    public RestClient hardenedRestClient() {
        return RestClient.builder()
                .requestInterceptor(new SsrfInterceptor())
                .build();
    }

    private static class SsrfInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            String host = request.getURI().getHost();
            if (host == null || isDisallowedIpOrHost(host)) {
                throw new SecurityException("SSRF blocked: outbound target resolves to a forbidden or private address [" + host + "]");
            }
            return execution.execute(request, body);
        }

        private boolean isDisallowedIpOrHost(String host) {
            if (FORBIDDEN_HOSTS.contains(host.toLowerCase())) {
                return true;
            }
            try {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                for (InetAddress address : addresses) {
                    if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                        return true;
                    }
                }
            } catch (UnknownHostException e) {
                return true;
            }
            return false;
        }
    }
}
