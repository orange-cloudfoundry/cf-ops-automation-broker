#!/usr/bin/env bash
#
# Copyright (C) 2015-2016 Orange
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -ev

echo "CIRCLE_TAG: <$CIRCLE_TAG>"

#Promoting cf-ops-automation-broker-framework
BROKER_FRAMEWORK_JFROG_PROMOTION_URL=http://oss.jfrog.org/api/plugins/build/promote/snapshotsToBintray/cf-ops-automation-broker-framework/${CIRCLE_BUILD_NUM}
echo "Promoting build on JFrog to Bintray (Promotion URL: $BROKER_FRAMEWORK_JFROG_PROMOTION_URL)"
curl --silent -X POST -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -d "" $BROKER_FRAMEWORK_JFROG_PROMOTION_URL

#Promoting cf-ops-automation-broker-core
BROKER_CORE_JFROG_PROMOTION_URL=http://oss.jfrog.org/api/plugins/build/promote/snapshotsToBintray/cf-ops-automation-broker-core/${CIRCLE_BUILD_NUM}
echo "Promoting build on JFrog to Bintray (Promotion URL: $BROKER_CORE_JFROG_PROMOTION_URL)"
curl --silent -X POST -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -d "" $BROKER_CORE_JFROG_PROMOTION_URL

#Promoting spring-boot-starter-servicebroker-catalog
STARTER_CATALOG_JFROG_PROMOTION_URL=http://oss.jfrog.org/api/plugins/build/promote/snapshotsToBintray/spring-boot-starter-servicebroker-catalog/${CIRCLE_BUILD_NUM}
echo "Promoting build on JFrog to Bintray (Promotion URL: $STARTER_CATALOG_JFROG_PROMOTION_URL)"
curl --silent -X POST -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -d "" $STARTER_CATALOG_JFROG_PROMOTION_URL

#Promoting cf-ops-automation-sample-broker
SAMPLE_BROKER_JFROG_PROMOTION_URL=http://oss.jfrog.org/api/plugins/build/promote/snapshotsToBintray/cf-ops-automation-sample-broker/${CIRCLE_BUILD_NUM}
echo "Promoting build on JFrog to Bintray (Promotion URL: $SAMPLE_BROKER_JFROG_PROMOTION_URL)"
curl --silent -X POST -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -d "" $SAMPLE_BROKER_JFROG_PROMOTION_URL

#Promoting cf-ops-automation-cloudflare-broker
CLOUDFLARE_BROKER_JFROG_PROMOTION_URL=http://oss.jfrog.org/api/plugins/build/promote/snapshotsToBintray/cf-ops-automation-cloudflare-broker/${CIRCLE_BUILD_NUM}
echo "Promoting build on JFrog to Bintray (Promotion URL: $CLOUDFLARE_BROKER_JFROG_PROMOTION_URL)"
curl --silent -X POST -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -d "" $CLOUDFLARE_BROKER_JFROG_PROMOTION_URL
