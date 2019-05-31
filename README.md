# AppBundler Maven Plugin

This is a Maven plugin for working with Oracle's AppBundler. Specifically the fork at https://github.com/TheInfiniteKind/appbundler

```xml
<plugin>
  <groupId>com.evolvedbinary.appbundler</groupId>
  <artifactId>appbundler-maven-plugin</artifactId>
  <version>1.0-SNAPSHOT</version>
</plugin>
```

## Example

```xml
<plugin>
  <groupId>com.evolvedbinary.appbundler</groupId>
  <artifactId>appbundler-maven-plugin</artifactId>
  <version>1.0-SNAPSHOT</version>
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


