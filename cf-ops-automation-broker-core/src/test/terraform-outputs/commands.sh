#!/usr/bin/env bash

chmod -w /tmp/non-writeable/; sudo rm /tmp/non-writeable/file
rm terraform.tfstate
terraform init -input=false -verify-plugins=false   ; terraform apply  -input=false ; terraform  output; terraform show
chmod +w /tmp/non-writeable/
terraform init -input=false -verify-plugins=false   ; terraform apply  -input=false ; terraform  output; terraform show
