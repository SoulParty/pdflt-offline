#!/usr/bin/env bash
mvn install:install-file -DgroupId=iaik.pkcs.pkcs11 -DartifactId=jce -Dversion=1.0 -Dpackaging=jar -Dfile=iaik_jce_full.jar
mvn install:install-file -DgroupId=iaik.pkcs.pkcs11 -DartifactId=provider -Dversion=1.0 -Dpackaging=jar -Dfile=iaikPkcs11Provider.jar
mvn install:install-file -DgroupId=iaik.pkcs.pkcs11 -DartifactId=wrapper -Dversion=1.0 -Dpackaging=jar -Dfile=iaikPkcs11Wrapper.jar
mvn install:install-file -DgroupId=at.gv.egiz -DartifactId=smcc -Dversion=1.3.15-SNAPSHOT -Dpackaging=jar -Dfile=smcc-1.3.15-SNAPSHOT.jar