#!/usr/bin/env bash

curl http://user:secret@localhost:8080/v2/catalog

curl -vvv http://user:secret@localhost:8080/v2/service_instances/111?accepts_incomplete=true -d '{
  "context": {
    "platform": "cloudfoundry",
    "some_field": "some-contextual-data"
  },
  "service_id": "service_id",
  "plan_id": "plan_id",
  "organization_guid": "org-guid-here",
  "space_guid": "space-guid-here",
  "parameters": {
    "parameter1": 1,
    "parameter2": "foo"
  }
}' -X PUT -H "X-Broker-API-Version: 2.12" -H "Content-Type: application/json" -u user:secret


curl -vvv http://user:secret@localhost:8080/v2/service_instances/111/service_bindings/222?accepts_incomplete=true -d '{
  "context": {
    "platform": "cloudfoundry",
    "some_field": "some-contextual-data"
  },
  "service_id": "service_id",
  "plan_id": "plan_id",
  "organization_guid": "org-guid-here",
  "space_guid": "space-guid-here",
  "parameters": {
    "parameter1": 1,
    "parameter2": "foo"
  }
}' -X PUT -H "X-Broker-API-Version: 2.12" -H "Content-Type: application/json" -u user:secret
