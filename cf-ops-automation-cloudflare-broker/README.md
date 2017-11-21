
### Introduction

Creates an internet-facing domain protected by cloudflare DOS prevention, and potentially other features such as analytics, rate limiting. The route without hostname ( created in the space where the service instance is created) needs to be bound applications.

This is similar to https://github.com/cloudflare/Cloudflare-Pivotal-Cloud-Foundry but leverages Terraform [cloudflare provider](https://www.terraform.io/docs/providers/cloudflare/index.html) instead

### Deploying

```bash
# deploy the broker    
cf push 

# Register the broker system-wise (requires cloudcontroller.admin i.e. admin access to the CloudFoundry instance)
# refer to http://docs.cloudfoundry.org/services/managing-service-brokers.html#register-broker
cf create-service-broker cloudflarebroker user secret http://cloudflare-broker.cf.redacted-domain.org/
# Then make individual services visibles in desired orgs or in all orgs,
# see  http://docs.cloudfoundry.org/services/access-control.html#enable-access
cf enable-service-access cloudflare
```

### Using the broker

```bash
cf marketplace -s cloudflare
cf create-service  cloudflare
cf cs cloudflare default myroute -c '{"route":"myroute"}'
cf service myroute

```