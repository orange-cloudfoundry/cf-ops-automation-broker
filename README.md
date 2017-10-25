# cf-ops-automation-broker [![CI](https://circleci.com/gh/orange-cloudfoundry/cf-ops-automation-broker.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/orange-cloudfoundry/cf-ops-automation-broker)
Open Service Broker API for Orange cf-ops-automation pipelines generation

Status:
Latest news:
- we have developed and used a concourse base contiuous delivery CD mechanism. https://github.com/orange-cloudfoundry/cf-ops-automation
- this mechanism, coupled with best pratices paas templates can deploy and maintain bosh masnifests, and terraform spec files
- secrets generation, retrieval, and management can be handled with credhub

Evaluating a new scenario:
- develop a service broker which generates cf-ops-automation compatible manifests files.
- the service broker relies on concourse pipelines to apply manifests file, deploy and update concourse and terraform resources

