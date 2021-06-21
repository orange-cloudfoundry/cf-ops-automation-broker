#!/bin/sh

FLY_CMD=${FLY_CMD:-fly}

if [ -z "$FLY_TARGET" ]; then
  echo "ERROR: FLY_TARGET is missing. Please set FLY_TARGET to your fly target"
  exit 1
fi

echo "Deploy on ${FLY_TARGET}"
set -e

wget https://raw.githubusercontent.com/orange-cloudfoundry/cf-ops-automation-broker/develop/concourse/coab-cleanup-pipeline.yml -O coab-cleanup-pipeline.yml
wget https://raw.githubusercontent.com/orange-cloudfoundry/cf-ops-automation-broker/develop/concourse/credentials-coab-cleanup-pipeline.yml -o credentials-coab-cleanup-pipeline.yml

${FLY_CMD} -t ${FLY_TARGET} edit-target -n coab-depls
${FLY_CMD} -t ${FLY_TARGET} set-pipeline -p coab-cleanup-pipeline -c coab-cleanup-pipeline.yml \
  -l ../coa/config/credentials-slack-config.yml \
  -l credentials-coab-cleanup-pipeline.yml
${FLY_CMD} -t ${FLY_TARGET} unpause-pipeline -p coab-cleanup-pipeline
${FLY_CMD} -t ${FLY_TARGET} edit-target -n main
if [ -f coab-cleanup-pipeline.yml ];then
 rm coab-cleanup-pipeline.yml
fi
if [ -f credentials-coab-cleanup-pipeline.yml ];then
 rm credentials-coab-cleanup-pipeline.yml
fi
