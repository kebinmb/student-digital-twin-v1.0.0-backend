---
title: Outbound SSRF Mitigation for RestClient and WebCLientq
tags:
  - spring-boot
  - appsec
  - ssrf
  - restclient
  - networking
---
## Security Risk & Mechanism
Server-Side Request Forgery (SSRF) occurs when an application makes outbound HTTP requests to user-supplied URLs, without restricting target IP addresses. Attackers exploit this to query:
- Cloud metadata services (e.g., `http://169.254.169.254/latest/meta-data/`)
- Internal loopbacks (`127.0.0.1`,`localhost`)
- Private RFC-1918 internal subnets (`10.0.0.0/8`,`172.16.0.0/12`,`192.168.0.0/16`)

## Implementation Steps
1. Create a `ClientHttpRequestInterceptor` that inspects the destination URI.
2. Perform DNS resolution on the hostname **before** the TCP connection opens.
3. Validate all resolved `InetAddress` entries against internal, loopback, and metadata ranges.
4. Abort execution immediately if a forbidden subnet is targeted.

## Code Example
``` java
package com.example.security.ssrf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.netURI;
import java.net.UnknownHostException;
import java.util.Set;

@Configuration
public class SafeRestClientConfig{
	private static final Set<String> BLOCKED_HOSTS = Set.of(
	"169.254.169.254", // Cloud Instance Metadata
	"metadata.google.internal"
	);
	
	@Bean
	public RestClient safeRestClient(RestClient.Builder builder){
		return builder
		.requestInterceptor(ssrfProtectionInterceptor())
		.build();
	}
	
	private ClientHttpRequestInterceptor ssrfProtectionInterceptor(){
		return (request, body, execution) ->{
			URI targetUri = request.getURI();
			String host = targetUri.getHost();
			
			if(host == null || isBlockedAddress(host)){
				throw new SecurityException("Blocked target host / IP range: " + host);
			}
			return execution.execute(request,body);
		}
	}
	
	private boolean isBlockedAddress(String host){
		if(BLOCKED_HOSTS.contains(host.toLowerCase())){
			return true; 
		}
		try {
			InetAddress[] addresses = InetAddress.getAllByName(host);
			for(InetAddress addr : addresses){
				if(addr.isLoopbackAddress() ||
					addr.isSiteLocalAddress() ||
					addr.isLinkLocalAddress() ||
					addr.isAnyLocalAddress()){
					return true;
					}
			}
		}catch(UnknownHostException e){
			return true;
		}
		return false;
	}
}
```