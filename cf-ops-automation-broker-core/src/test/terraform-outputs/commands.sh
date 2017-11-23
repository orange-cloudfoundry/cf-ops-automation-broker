#!/usr/bin/env bash

mkdir -p /tmp/non-writeable/
chmod -w /tmp/non-writeable/; sudo rm /tmp/non-writeable/file
rm terraform.tfstate
terraform init -input=false -verify-plugins=false   ; terraform apply  -input=false -auto-approve; terraform  output; terraform show
#expect "tf apply" status code error and outputs to be:
# 4567.started = successfully received module invocation
chmod +w /tmp/non-writeable/
terraform init -input=false -verify-plugins=false   ; terraform apply  -input=false -auto-approve; terraform  output; terraform show
#expect "tf apply" status code status and outputs to be:
# 4567.completed = successfully provisionned no
# 4567.started = successfully received module invocation

