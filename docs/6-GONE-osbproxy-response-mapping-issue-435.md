
Pb: GONE response status is always mapped to 500 errors, even when wrapped into our exception mechanism
* root cause is sc-osb lib mapping unknown exceptions to 500 including spring ResponseStatusException

Pb: the response code and message mapping is using spring4 annotation based mechanism and therefore handling individually each status code

Use of ReResponseStatusException requires integration testing to make sure Spring does not swallow the configured user-facing broker error message

See https://stackoverflow.com/questions/62459836/exception-message-not-included-in-response-when-throwing-responsestatusexception

https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.3-Release-Notes#changes-to-the-default-error-pages-content 
```
Changes to the Default Error Page’s Content

The error message and any binding errors are no longer included in the default error page by default. This reduces the risk of leaking information to a client. server.error.include-message and server.error.include-binding-errors can be used to control the inclusion of the message and binding errors respectively. Supported values are always, on-param, and never.
```

How to test it ?
* [ ] Add a test case in BoshServiceProvisionningTest with a wiremock binding response returning 400 Bad request status with an associated message