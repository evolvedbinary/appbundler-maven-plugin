# AppBundler Maven Plugin

[![Build Status](https://github.com/evolvedbinary/appbundler-maven-plugin/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/evolvedbinary/appbundler-maven-plugin/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.evolvedbinary.appbundler/appbundler-maven-plugin?logo=apachemaven&label=maven+central&color=green)](https://central.sonatype.com/search?namespace=com.evolvedbinary.appbundler)

This is a Maven plugin for working with Oracle's AppBundler. Specifically the fork at https://github.com/evolvedbinary/appbundler

```xml
<plugin>
  <groupId>com.evolvedbinary.appbundler</groupId>
  <artifactId>appbundler-maven-plugin</artifactId>
  <version>3.1.0</version>
</plugin>
```

NOTE: By default, the plugin will use a universal native binary for the AppBundler that should run on both macOS for x86_64 and arm64 processors.
If you only need a platform specific binary, you can override the dependency on `com.evolvedbinary.appbundler:appbundler` with a classifier of either: `<classifier>macos-x86_64-only</classifier>` or `<classifier>macos-arm64-only</classifier>`, for example:

```xml
<plugin>
  <groupId>com.evolvedbinary.appbundler</groupId>
  <artifactId>appbundler-maven-plugin</artifactId>
  <version>3.1.0</version>
  <dependencies>
    <dependency>
      <groupId>com.evolvedbinary.appbundler</groupId>
      <artifactId>appbundler</artifactId>
      <version>1.4.1</version>
      <classifier>macos-x86_64-only</classifier>
    </dependency>
  </dependencies>
</plugin>
```

## Example

```xml
<plugin>
  <groupId>com.evolvedbinary.appbundler</groupId>
  <artifactId>appbundler-maven-plugin</artifactId>
  <version>3.1.1-SNAPSHOT</version>
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


