# Yaml EntityStore - Development details

## Setup
The project is based on Maven and ship the required API-Gateway libraries as part of the project. 
In order to setup your development environment just clone the project:  
`git clone https://github.com/axwayhackathon/YamlES.git`  
and execute:  
`mvn package`  

To setup the project for Eclipse:  
`mvn eclipse:eclipse`  

## Classes & Package structure
### YamlEntityStore

### YamlEntityStoreProvider

com.vordel.rcp.policystudio_7.7.0.v20200331-1131.jar
system\conf\esproviders.xml
```xml
<?xml version = "1.0" encoding = "UTF-8"?>
<!--
  Configuration file for the Entity Store factory.
-->
<entityStore>
  <!--
    A list of classes which implement the Entity Store API. They must
    be loaded by the EntityStoreFactory before they are available for
    client use.
  -->
  <providers>
    <!--
      Always available providers - absence of these represent a
      serious problem with the install image:
    -->
    <LoadClass name="com.vordel.es.provider.file.FileStore"/>
    <LoadClass name="com.vordel.es.fed.FederatedEntityStore"/>
    <LoadClass name="com.axway.gw.es.yaml.YamlEntityStoreProvider"/>
  </providers>
</entityStore>
```
