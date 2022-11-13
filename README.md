# AppBundler Maven Plugin

[![Build Status](https://github.com/evolvedbinary/appbundler-maven-plugin/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/evolvedbinary/appbundler-maven-plugin/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.evolvedbinary.appbundler/appbundler-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.evolvedbinary.appbundler/appbundler-maven-plugin)

This is a Maven plugin for working with Oracle's AppBundler. Specifically the fork at https://github.com/evolvedbinary/appbundler

```xml
<plugin>
  <groupId>com.evolvedbinary.appbundler</groupId>
  <artifactId>appbundler-maven-plugin</artifactId>
  <version>1.1.0</version>
</plugin>
```

## Example

```xml
<plugin>
  <groupId>com.evolvedbinary.appbundler</groupId>
  <artifactId>appbundler-maven-plugin</artifactId>
  <version>1.1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <id>bundle-my-mac-app</id>
      <phase>package</phase>
      <goals>
        <goal>bundle</goal>
      </goals>
      <configuration>
        <displayName>Your-App-Name</displayName>
        <identifier>com.your.app.Main</identifier>
        <mainClassName>com.your.app.Main</mainClassName>
        <jvmRequired>1.8</jvmRequired>
        <version>${project.version}</version>
        <shortVersion>${project.version}</shortVersion>
        <icon>icon.icns</icon>
        <copyright>${project.inceptionYear} Your Name</copyright>
        <applicationCategory>public.app-category.developer-tools</applicationCategory>
        <additionalResourcePaths>
          <fileSet>
            <directory>${project.basedir}/src/main/config</directory>
            <outputDirectory>etc</outputDirectory>
          </fileSet>
        </additionalResourcePaths>
        <options>
          <option>-Xms128m</option>
          <option>-Dfile.encoding=UTF-8</option>
          <option>-Xdock:name=Your-App-Name</option>
        </options>
        <explicitClassPath>false</explicitClassPath>
        <isDebug>true</isDebug>
      </configuration>
    </execution>
  </executions>
</plugin>
```


