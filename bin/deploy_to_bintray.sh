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

echo "CIRCLE_BRANCH: <$CIRCLE_BRANCH> - CIRCLE_TAG: <$CIRCLE_TAG>"

#Download dependencies
mvn -q help:evaluate -Dexpression=project.version --settings settings.xml
# Capture execution of maven command - It looks like grep cannot be used like this on circle
export VERSION_SNAPSHOT=$(mvn help:evaluate -Dexpression=project.version --settings settings.xml |grep '^[0-9].*')

echo "Current version extracted from pom.xml: $VERSION_SNAPSHOT"

export VERSION_PREFIX=$(expr "$VERSION_SNAPSHOT" : "\(.*\)-SNAP.*")

#We are on master without PR
prefix_number=$(($CIRCLE_BUILD_NUM % 10))
export RELEASE_CANDIDATE_VERSION=$VERSION_PREFIX.${prefix_number}
export RELEASE_CANDIDATE_SNAPSHOT_VERSION=${RELEASE_CANDIDATE_VERSION}-SNAPSHOT

echo "Release candidate snapshot version: $RELEASE_CANDIDATE_SNAPSHOT_VERSION"

echo "Setting new version old: $VERSION_SNAPSHOT"

#Download dependencies
mvn -q versions:help --settings settings.xml
mvn -e versions:set -DnewVersion=${RELEASE_CANDIDATE_SNAPSHOT_VERSION} -DgenerateBackupPoms=false -DallowSnapshots=true --settings settings.xml

echo "Compiling and deploying to OSS Jfrog"

mvn -q deploy:help --settings settings.xml
mvn clean deploy --settings settings.xml -P ojo-build-info

echo $RELEASE_CANDIDATE_SNAPSHOT_VERSION > RELEASE_CANDIDATE_VERSION