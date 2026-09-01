
| Features  |                                                           |                                                                                                 |                                                               |                 |                          |
| --------- | --------------------------------------------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------- | --------------- | ------------------------ |
| `Backend` | `Concurrency`,`Virtual Threads` and `Context Propagation` | `Unified Async Context Propagation`, `Virtual Thread Pinning in Legacy Auth Providers`          | [[00 Spring  Security - Virtual Threads Context Propagation]] | **IMPLEMENTED** | `com.sdt.web_app.config` |
|           | `Built-in Outbound SSRF Mitigations`                      | `Outbound HTTP Client Hardening`, `Block internal Metadata & RFC-1918 Ranges`                   | [[01 Outbound SSRF Mitigation in RestClient]]                 | **IMPLEMENTED** | `com.sdt.web_app.config` |
|           | `Oauth 2.1 & Token Security`                              | `Oauth 2.1 Strict Compliance`,`JWT Audience & Clock Skew Validation`, `Opaque Token Revocation` | [[02 OAuth 2.1 Strict Compliance & JWT Validation]]           | **IMPLEMENTED** | `com.sdt.web_app.config` |
|           | `Zero-Trust API & gRPC Layer Security`                    | `Spring gRPC 1.1 Security Interceptors`,`JSpecify Null-Safety Annotations`                      | [[03 gRPC 1.1 Security Interceptors]]                         | **IMPLEMENTED** | `com.sdt.web_app.config` |
|           | `Serialization and Deserialization Hardening`             | `Jackson 3 Integration`                                                                         | [[04 Jackson 3 Polymorphic Deserialization Hardening]]        | **IMPLEMENTED** | `com.sdt.web_app.config` |
|           | `Actuator & Edge Isolation`                               | `Actuator Port Segregation`,`Authorization Rules by Port`                                       | [[05 Actuator Port Segregation & Filter Chains]]              | **IMPLEMENTED** | `com.sdt.web_app.config` |
|           | `Modern Header Policies & Cookie Protections`             | `Partitioned Cookies (CHIPS)`,`Permission-Policy and CSP`                                       | [[06 Modern Security Headers & CHIPS Cookies]]                | **IMPLEMENTED** | `com.sdt.web_app.config` |
|           | `Cors Configuration`                                      |                                                                                                 | [[07 Cors Configuration]]                                     | **IMPLEMENTED** | `com.sdt.web_app.config` |

## Login and Authentication
| Entities       |     |
| -------------- | --- |
| `User`         |     |
| `RefreshToken` |     |


## Frontend Implementation
PrimeNG v18+