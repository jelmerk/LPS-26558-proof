LPS-26558 proof of concept exploit
==================================

Crudely crafted proof of concept for the vulnerability described in [LPS-26558](http://issues.liferay.com/browse/LPS-26558)
it targets the default liferay ce 6.1 distributable.
To try it out

1) download liferay ce 6.1 from  http://www.liferay.com/downloads/liferay-portal/available-releases,unzip it,
start it  and configure it with default settings.

2) Download and configure [maven 3](http://maven.apache.org) then from the commandline run :

mvn clean install

in the target folder this will create a file called liferay-memcached-exploit-1.0-zip-with-jars.zip

3) unzip liferay-memcached-exploit-1.0-zip-with-jars.zip somewhere and run the exploit with

java -jar liferay-memcached-exploit-1.0.jar


Now follow the instructions in the console. It should grant you administrative access to the liferay instance