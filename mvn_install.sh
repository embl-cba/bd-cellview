mvn clean package

rm /g/almf/software/Fiji-versions/Fiji-BDVulcan.app/jars/imflow-*
rm /g/almf/software/Fiji-versions/Fiji-BDVulcan.app/jars/imagej-utils-*

rm /g/cba/exchange/imflow/jars/imflow-*
rm /g/cba/exchange/imflow/jars/imagej-utils-*
rm /g/cba/exchange/imflow/jars/imagej-cluster-*

cp /Users/tischer/Documents/fccf/target/imflow-0.2.0.jar /g/almf/software/Fiji-versions/Fiji-BDVulcan.app/jars/ 
cp /Users/tischer/Documents/imagej-utils/target/imagej-utils-0.5.5-SNAPSHOT.jar /g/almf/software/Fiji-versions/Fiji-BDVulcan.app/jars/

cp /Users/tischer/Documents/fccf/target/imflow-0.2.0.jar /g/cba/exchange/imflow/jars/ 
cp /Users/tischer/Documents/imagej-cluster/target/imagej-cluster-0.6.3-SNAPSHPOT.jar /g/cba/exchange/imflow/jars/
cp /Users/tischer/Documents/imagej-utils/target/imagej-utils-0.5.5-SNAPSHOT.jar /g/cba/exchange/imflow/jars/

