---
title: Jackson 3 Deserialization Hardening & Polymorphic Type Validators
tags:
  - jackson
  - json-security
  - rce-prevention
  - spring-boot
---
## Security Risk & Mechanism
Polymorphic type handling (`@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)`) allows incoming JSON to dictate what Java class the parser instantiates. Without explicit type whitelisting, attackers send gadget classes to achieve Remote Code Execution (RCE) during JSON payload parsing.

### Implementation Steps
1. Replace broad default typing with `BasicPolymorphicTypeValidator`.
2. Explicitly whitelist trusted base classes and safe package prefixes.
3. Configurate the custom `PolymorphicTypeValidator` on the global `ObjectMapper`/ `JsonMapper` bean.

### Code Example
```java
package com.example.security.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databin.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsonType.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator; import org.springframework.context.annotation.Bean; 
import org.springframework.context.annotation.Configuration; 
import org.springframework.context.annotation.Primary; 

@Configuration
public class JacksonSecurityConfig{
	@Bean
	@Primary
	public ObjectMapper secureObjectMapper(){
		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
		..allowIfBaseType("com.example.app.dto.events.") .allowIfSubType("com.example.app.dto.events.") .allowIfBaseType(java.util.List.class) .allowIfBaseType(java.util.Map.class) .build();
	return JsonMapper.builder()
		.polymorphicTypeValidator(ptv)
		.build();	
	}
}


```