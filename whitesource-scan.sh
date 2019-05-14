#!/bin/bash

set -x
pwd

productName=COAB
# targetFolderName is where the jar file/s will be created.
# This is usually target or bin
targetFolderName=target

cd ~/cf-ops-automation-broker

#Extracted from  https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss_agent.sh
if [ ! -f wss-unified-agent.jar ]; then
    curl -LJO https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss-unified-agent.jar
fi
if [ ! -f wss-unified-agent.config ]; then
    curl -LJO https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss-unified-agent.config
fi

# Fallback searching jars manually
#echo "--------------------------"
#echo "Start of explicit analysis of individual jars"
#for currentFolderName in $(ls -d1 cf-ops-automation*/)
#do
#  projectName=${currentFolderName%?}
#  #Finds the 1st jar file
#  jarCount=`find ${currentFolderName}/${targetFolderName} -type f -name "*.jar" | head -1 | wc -l`
#  if [ $jarCount -gt 0 ]; then
#    jarFile=`find ${currentFolderName}/${targetFolderName} -type f -name "*.jar" | head -1`
#    java -jar wss-unified-agent.jar -appPath ${jarFile} -d ${currentFolderName} -product $productName -project $projectName -apiKey $WHITESOURCE_API_KEY -c whitesource_config.properties
#  fi
#done
#
#echo "End of explicit analysis of individual jars"
#echo "--------------------------"

#Add api key in config file to avoid leaking it
sed "s#^apiKey=.*#apiKey=${WHITESOURCE_API_KEY}#g" -i whitesource_config.properties

# https://whitesource.atlassian.net/wiki/spaces/WD/pages/651919363/EUA+Support+for+Multi-Module+Analysis
#First discover submodules and save it into a properties file
java -jar wss-unified-agent.jar -d $(pwd) -analyzeMultiModule whitesource-multi-module.properties

echo "whitesource-multi-module.properties content:"
cat whitesource-multi-module.properties

curl -LJ https://s3.amazonaws.com/file-system-agent/xModuleAnalyzer/xModuleAnalyzer-19.2.2.jar -o xModuleAnalyzer.jar

#Then invoke analysis on all modules
java -jar xModuleAnalyzer.jar -xModulePath whitesource-multi-module.properties -fsaJarPath wss-unified-agent.jar -c whitesource_config.properties -statusDisplay dynamic 

echo "Whitesource-Logs:"
LOG_FILES=$(find ./Whitesource-Logs/ -type f)
for f in $LOG_FILES; do echo "----- start content of :$f -----"; cat $f; echo "----- end content of :$f -----"; done



