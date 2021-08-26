
Pb: GONE response status is always mapped to 500 errors, even when wrapped into our exception mechanism
* one cause is ourselves wrapping as ServiceBrokerException which gets mapped to 500 errors.
* second cause is sc-osb missing support for programmatic status code return (only support named exceptions)
* third cause is sc-osb lib mapping unknown exceptions to 500 including spring5 ResponseStatusException designed for .
* Alternative fixes
  * [ ] map to sc-osb named exceptions: 
    * pb: would display possibly misleading exception names to coab operators, e.g. 422 would be mapped to ServiceInstanceBindingDoesNotExistException
    * pb: would display misleading user exception messages to osb client platform users 
      * [ ] with distinct exception depending on the current operation: provision vs bind ...
  * [ ] Contribute programmatic error status support in sc-osb
  * [x] Override ServiceBrokerWebMvcExceptionHandler with a custom bean with higher precedence
     * See https://www.baeldung.com/exception-handling-for-rest-with-spring#controlleradvice
     * This also addresses default spring leak prevention mechanism. See https://stackoverflow.com/questions/62459836/exception-message-not-included-in-response-when-throwing-responsestatusexception and https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.3-Release-Notes#changes-to-the-default-error-pages-content 
```
Changes to the Default Error Page’s Content

The error message and any binding errors are no longer included in the default error page by default. This reduces the risk of leaking information to a client. server.error.include-message and server.error.include-binding-errors can be used to control the inclusion of the message and binding errors respectively. Supported values are always, on-param, and never.
```

How to test it ?
* [x] Add a test case in BoshServiceProvisionningTest with a wiremock binding response returning 400 Bad request status with an associated message