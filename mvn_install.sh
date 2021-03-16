#!/bin/sh

echo "Copying jars to almf and cba server..."

IJ_UTILS="0.6.1"
IMFLOW="0.2.3"
IJ_CLUSTER="0.6.7"

mvn clean package

rm /g/almf/software/Fiji-versions/Fiji-BDVulcan.app/jars/imflow-*
rm /g/almf/software/Fiji-versions/Fiji-BDVulcan.app/jars/imagej-utils-*

rm /g/cba/exchange/imflow/jars/imflow-*
rm /g/cba/exchange/imflow/jars/imagej-utils-*
rm /g/cba/exchange/imflow/jars/imagej-cluster-*

cp /Users/tischer/Documents/fccf/target/imflow-$IMFLOW.jar /g/almf/software/Fiji-versions/Fiji-BDVulcan.app/jars/ 
cp /Users/tischer/.m2/repository/de/embl/cba/imagej-utils/$IJ_UTILS/imagej-utils-$IJ_UTILS.jar /g/almf/software/Fiji-versions/Fiji-BDVulcan.app/jars/

cp /Users/tischer/Documents/fccf/target/imflow-$IMFLOW.jar /g/cba/exchange/imflow/jars/ 
cp /Users/tischer/.m2/repository/de/embl/cba/imagej-cluster/$IJ_CLUSTER/imagej-cluster-$IJ_CLUSTER.jar /g/cba/exchange/imflow/jars/
cp /Users/tischer/.m2/repository/de/embl/cba/imagej-utils/$IJ_UTILS/imagej-utils-$IJ_UTILS.jar /g/cba/exchange/imflow/jars/

echo "...done."
