/*
 * Much of this code was originally taken from Oracle's
 * App Bundler class com.oracle.appbundler.AppBundlerTask
 * and some modifications made by Adam Retter.
 * The original copyright and license follows...
 */

/*
 * Copyright 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.evolvedbinary.appbundler;

import com.oracle.appbundler.IconContainer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.FlatRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.sonatype.aether.util.layout.RepositoryLayout;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "bundle", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AppBundlerMojo extends AbstractMojo {

    private static final String APPBUNDLER_PACKAGE_PATH = "com/oracle/appbundler";
    private static final String APP_ROOT_PREFIX = "$APP_ROOT";

    private static final String PLIST_DTD = "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">";
    private static final String PLIST_TAG = "plist";
    private static final String PLIST_VERSION_ATTRIBUTE = "version";
    private static final String DICT_TAG = "dict";
    private static final String KEY_TAG = "key";
    private static final String ARRAY_TAG = "array";
    private static final String STRING_TAG = "string";

    private static final String EXECUTABLE_NAME = "JavaAppLauncher";
    private static final String DEFAULT_ICON_NAME = "GenericApp.icns";
    private static final String OS_TYPE_CODE = "APPL";

    @Parameter(defaultValue = "${project.runtimeArtifacts}", readonly = true)
    private List<Artifact> artifacts;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject mavenProject;

    /**
     *  Output folder for generated bundle
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}")
    private File outputDirectory;


    /* General bundle properties */

    @Parameter(required = true, defaultValue = "${project.artifactId}-${project.version}")
    private String name;

    @Parameter(required = true)
    private String displayName;

    @Parameter(required = true)
    private String identifier;

    @Parameter
    private File icon;

    @Parameter(defaultValue = EXECUTABLE_NAME)
    private String executableName;

    @Parameter(defaultValue = "${project.version}")
    private String shortVersion = "1.0";

    @Parameter(defaultValue = "${project.version}")
    private String version;

    @Parameter(defaultValue = "????")
    private String signature;

    @Parameter
    private String copyright;

    @Parameter
    private String privileged;

    @Parameter
    private String workingDirectory;

    @Parameter
    private String minimumSystemVersion;

    @Parameter
    private String jvmRequired = null;

    @Parameter(defaultValue = "false")
    private boolean jrePreferred;

    @Parameter(defaultValue = "false")
    private boolean jdkPreferred;

    @Parameter
    private String applicationCategory;

    @Parameter(defaultValue = "true")
    private boolean highResolutionCapable;

    @Parameter(defaultValue = "true")
    private boolean supportsAutomaticGraphicsSwitching;

    @Parameter(defaultValue = "false")
    private boolean hideDockIcon;

    @Parameter(defaultValue = "false")
    private boolean isDebug;

    @Parameter(defaultValue = "false")
    private boolean ignorePSN;

    /* JVM info properties */
    @Parameter
    private String mainClassName;

    @Parameter
    private String jnlpLauncherName;

    @Parameter
    private String jarLauncherName;

    //TODO(AR) needs to be configurable by parameters
    private Runtime runtime;

    @Parameter
    private List<Option> options = new ArrayList<>();

    @Parameter
    private List<String> arguments = new ArrayList<>();

    @Parameter
    private List<String> architectures = new ArrayList<>();

    @Parameter
    private List<String> registeredProtocols = new ArrayList<>();

    @Parameter
    private List<BundleDocument> bundleDocuments = new ArrayList<>();

    @Parameter
    private ArrayList<TypeDeclaration> exportedTypeDeclarations = new ArrayList<>();

    @Parameter
    private ArrayList<TypeDeclaration> importedTypeDeclarations = new ArrayList<>();

    @Parameter
    private List<PlistEntry> plistEntries = new ArrayList<>();

    @Parameter
    private Map<String, String> environments = new HashMap<>();

    /**
     * The set of Programs that bin files will be generated for.
     */
    @Parameter
    private List<FileSet> libraryPaths;


    /**
     * Additional paths for resources
     * that should be bundled.
     */
    @Parameter
    private List<FileSet> additionalResourcePaths;

    /**
     * Determines whether a classpath should be set explicitly in the
     * generated Info.plist file. If {@code false} the JavaAppLauncher
     * native application will implicitly add all Jar files found
     * in the java folder of the .app package to the classpath.
     */
    @Parameter(defaultValue = "false")
    private boolean explicitClassPath;

    /**
     * The following can be used to use all project dependencies instead of the default behavior which represents
     * <code>runtime</code> dependencies only.
     */
    @Parameter(defaultValue = "false")
    private boolean useAllProjectDependencies;


    public void execute() throws MojoExecutionException {
       validateParameters();

        // Create the app bundle
        try {
            getLog().info("Creating app bundle: " + name);

            // Create directory structure
            final Path rootDirectory = outputDirectory.toPath().resolve(name + ".app");
            Files.createDirectories(rootDirectory);

            final Path contentsDirectory = rootDirectory.resolve("Contents");
            Files.createDirectories(contentsDirectory);

            final Path macOSDirectory = contentsDirectory.resolve("MacOS");
            Files.createDirectories(macOSDirectory);

            final Path javaDirectory = contentsDirectory.resolve("Java");
            Files.createDirectories(javaDirectory);

            final Path plugInsDirectory = contentsDirectory.resolve("PlugIns");
            Files.createDirectories(plugInsDirectory);

            final Path resourcesDirectory = contentsDirectory.resolve("Resources");
            Files.createDirectories(resourcesDirectory);

            // Generate Info.plist
            final Path infoPlistFile = contentsDirectory.resolve("Info.plist");
            writeInfoPlist(infoPlistFile, javaDirectory);

            // Generate PkgInfo
            final Path pkgInfoFile = contentsDirectory.resolve("PkgInfo");
            writePkgInfo(pkgInfoFile);

            // Copy executable to MacOS folder
            final Path executableFile = macOSDirectory.resolve(executableName);
            try (final InputStream is = getClass().getClassLoader().getResourceAsStream(APPBUNDLER_PACKAGE_PATH + "/" + EXECUTABLE_NAME)) {
                Files.copy(is, executableFile, StandardCopyOption.REPLACE_EXISTING);
            }
            executableFile.toFile().setExecutable(true, false);

            // Copy localized resources to Resources folder
            copyResources(resourcesDirectory);

            // Copy additional resources to Resources folder
            copyAdditionalResources(resourcesDirectory);

            //TODO(AR) not yet supported
            // Copy runtime to PlugIns folder
//            copyRuntime(plugInsDirectory);

            // Copy class path entries to Java folder
            copyClassPathEntries(javaDirectory);

            // Copy library path entries to MacOS folder
            copyLibraryPathEntries(macOSDirectory);

            // Copy app icon to Resources folder
            copyIcon(resourcesDirectory);

            // Copy app document icons to Resources folder
            copyDocumentIcons(bundleDocuments, resourcesDirectory);
            copyDocumentIcons(exportedTypeDeclarations, resourcesDirectory);
            copyDocumentIcons(importedTypeDeclarations, resourcesDirectory);

            getLog().info("Finished bundling app " + rootDirectory.toAbsolutePath().toString());

        } catch (final IOException e) {
            throw new MojoExecutionException("Could not build app bundle: " + e.getMessage(), e);
        }
    }

    private void validateParameters() throws MojoExecutionException {
        // Validate required properties
        if (outputDirectory == null) {
            throw new MojoExecutionException("outputDirectory is required.");
        }

        if (!outputDirectory.exists()) {
            throw new MojoExecutionException("outputDirectory does not exist.");
        }

        if (!outputDirectory.isDirectory()) {
            throw new MojoExecutionException("Invalid outputDirectory.");
        }

        if (name == null) {
            throw new MojoExecutionException("name is required.");
        }

        if (displayName == null) {
            throw new MojoExecutionException("displayName is required.");
        }

        if (identifier == null) {
            throw new MojoExecutionException("identifier is required.");
        }

        if (icon != null) {
            if (icon.isDirectory()) {
                throw new MojoExecutionException("Invalid icon. Found directory, expected file!");
            }

            if (!icon.exists() && !Files.exists(mavenProject.getBasedir().toPath().resolve("src").resolve("appbundler").resolve(icon.getName()))) {
                throw new MojoExecutionException("icon does not exist.");
            }
        }

        if (shortVersion == null) {
            throw new MojoExecutionException("shortVersion is required.");
        }

        if (signature == null) {
            throw new IllegalStateException("signature is required.");
        }

        if (signature.length() != 4) {
            throw new MojoExecutionException("invalidSignature. Expected 4 characters, found: " + signature.length() + "!");
        }

        if (copyright == null) {
            throw new MojoExecutionException("copyright is required.");
        }

        if (jnlpLauncherName == null && mainClassName == null) {
            throw new MojoExecutionException("mainClassName or jnlpLauncherName is required.");
        }
    }

//    private void copyRuntime(final Path plugInsDirectory) throws IOException {
//        if (runtime != null) {
//            runtime.copyTo(plugInsDirectory);
//        }
//    }

    private void copyClassPathEntries(final Path javaDirectory) throws IOException {
        if (useAllProjectDependencies) {
            final Set dependencyArtifacts = mavenProject.getDependencyArtifacts();
            artifacts = new ArrayList<>();
            for (final Iterator it = dependencyArtifacts.iterator(); it.hasNext(); ) {
                final Artifact artifact = (Artifact) it.next();
                artifacts.add(artifact);
            }
        }

        final ArtifactRepositoryLayout flatRepositoryLayout = new FlatRepositoryLayout();
        for (final Artifact artifact : artifacts) {
            final Path source = artifact.getFile().toPath();
            final Path dest = javaDirectory.resolve(flatRepositoryLayout.pathOf(artifact));
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);

            getLog().info("Copied dependency " + source.toAbsolutePath().toString() + " to " + dest.toAbsolutePath().toString());
        }
    }

    private List<String> plistClassPath(final Path javaDirectory) {
        if (useAllProjectDependencies) {
            final Set dependencyArtifacts = mavenProject.getDependencyArtifacts();
            artifacts = new ArrayList<>();
            for (final Iterator it = dependencyArtifacts.iterator(); it.hasNext(); ) {
                final Artifact artifact = (Artifact) it.next();
                artifacts.add(artifact);
            }
        }

        final ArtifactRepositoryLayout flatRepositoryLayout = new FlatRepositoryLayout();
        final List<String> classPaths = new ArrayList<>(artifacts.size());
        for (final Artifact artifact : artifacts) {
            final String classPath = javaDirectory.getFileName().resolve(flatRepositoryLayout.pathOf(artifact)).toString();
            classPaths.add(APP_ROOT_PREFIX + "/Contents/" + classPath);
        }
        return classPaths;
    }

    private void copyLibraryPathEntries(final Path macOSDirectory) throws IOException {
        if (libraryPaths == null) {
            return;
        }

        final FileSetManager fileSetManager = new FileSetManager();
        for (final FileSet libraryPath : libraryPaths) {
            copyFileSet(fileSetManager, libraryPath, macOSDirectory);
        }
    }

    private void copyIcon(final Path resourcesDirectory) throws IOException {
        if (icon == null) {
            try (final InputStream is = getClass().getClassLoader().getResourceAsStream(APPBUNDLER_PACKAGE_PATH + "/" + DEFAULT_ICON_NAME)) {
                Files.copy(is, resourcesDirectory.resolve(DEFAULT_ICON_NAME), StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            final Path src;
            final Path dest = resourcesDirectory.resolve(icon.getName());

            if (icon.exists()) {
                src = icon.toPath();
            } else {
                src = mavenProject.getBasedir().toPath().resolve("src").resolve("appbundler").resolve(icon.getName());
                if (!Files.exists(src)) {
                   throw new FileNotFoundException(icon.toString());
                }
            }

            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            getLog().info("Copied icon " + src.toAbsolutePath().toString() + " to " + dest.toAbsolutePath().toString());
        }
    }

    public void copyDocumentIcons(final List<? extends IconContainer> iconContainers,
            final Path resourcesDirectory) throws IOException {
        for (final IconContainer iconContainer : iconContainers) {
            if (iconContainer.hasIcon()) {
                final Path ifile = iconContainer.getIconFile().toPath();
                if (ifile != null) {
                    copyDocumentIcon(ifile,resourcesDirectory);
                }
            }
        }
    }

    private void copyDocumentIcon(final Path ifile, final Path resourcesDirectory) throws IOException {
        if (ifile == null) {
            return;
        } else {
            final Path dest = resourcesDirectory.resolve(ifile.getFileName());
            Files.copy(ifile, dest, StandardCopyOption.REPLACE_EXISTING);
            getLog().info("Copied document icon " + ifile.toAbsolutePath().toString() + " to " + dest.toAbsolutePath().toString());
        }
    }

    private void copyResources(final Path resourcesDirectory) throws IOException {
        // Unzip res.zip into resources directory
        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(APPBUNDLER_PACKAGE_PATH + "/" + "res.zip");
             final ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                final Path file = resourcesDirectory.resolve(zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(file);
                } else {
                    final Path parent = file.getParent();
                    if (!Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zipInputStream, file, StandardCopyOption.REPLACE_EXISTING);
                    getLog().info("Copied resource " + zipEntry.getName() + " to " + file.toAbsolutePath().toString());
                }

                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    private void copyAdditionalResources(final Path resourcesDirectory) throws IOException {
        if (additionalResourcePaths == null) {
            return;
        }

        final FileSetManager fileSetManager = new FileSetManager();

        for (final FileSet additionalResourcePath : additionalResourcePaths) {
            copyFileSet(fileSetManager, additionalResourcePath, resourcesDirectory);
        }
    }

    private void copyFileSet(final FileSetManager fileSetManager, final FileSet fileSet, final Path outputBaseDir) throws IOException {
        final String[] includes = fileSetManager.getIncludedFiles(fileSet);
        for (final String include : includes) {
            final Path source = Paths.get(fileSet.getDirectory()).resolve(include);
            final Path destDir;
            if (fileSet.getOutputDirectory() == null) {
                destDir = outputBaseDir.resolve(source.getParent().getFileName().toString());
            } else if (fileSet.getOutputDirectory().isEmpty() || fileSet.getOutputDirectory().equals(".")) {
                destDir = outputBaseDir;
            } else {
                destDir = outputBaseDir.resolve(fileSet.getOutputDirectory());
            }
            if (!Files.exists(destDir)) {
                Files.createDirectories(destDir);
            }
            final Path dest = destDir.resolve(source.getFileName());
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);

            getLog().info("Copied " + source.toAbsolutePath().toString() + " to " + dest.toAbsolutePath().toString());
        }
    }

    private void writeInfoPlist(final Path file, final Path javaDirectory) throws IOException {
        try (final Writer out = Files.newBufferedWriter(file, UTF_8)) {
            final XMLOutputFactory output = XMLOutputFactory.newInstance();

            XMLStreamWriter xout = null;
            try {
                xout = output.createXMLStreamWriter(out);

                // Write XML declaration
                xout.writeStartDocument();
                xout.writeCharacters("\n");

                // Write plist DTD declaration
                xout.writeDTD(PLIST_DTD);
                xout.writeCharacters("\n");

                // Begin root element
                xout.writeStartElement(PLIST_TAG);
                xout.writeAttribute(PLIST_VERSION_ATTRIBUTE, "1.0");
                xout.writeCharacters("\n");

                // Begin root dictionary
                xout.writeStartElement(DICT_TAG);
                xout.writeCharacters("\n");

                // Write bundle properties
                writeProperty(xout, "CFBundleDevelopmentRegion", "English");
                writeProperty(xout, "CFBundleExecutable", executableName);
                writeProperty(xout, "CFBundleIconFile", (icon == null) ? DEFAULT_ICON_NAME : icon.getName());
                writeProperty(xout, "CFBundleIdentifier", identifier);
                writeProperty(xout, "CFBundleDisplayName", displayName);
                writeProperty(xout, "CFBundleInfoDictionaryVersion", "6.0");
                writeProperty(xout, "CFBundleName", name);
                writeProperty(xout, "CFBundlePackageType", OS_TYPE_CODE);
                writeProperty(xout, "CFBundleShortVersionString", shortVersion);
                writeProperty(xout, "CFBundleVersion", version);
                writeProperty(xout, "CFBundleSignature", signature);
                writeProperty(xout, "NSHumanReadableCopyright", copyright);
                writeProperty(xout, "LSMinimumSystemVersion", minimumSystemVersion);
                writeProperty(xout, "LSApplicationCategoryType", applicationCategory);
                writeProperty(xout, "LSUIElement", hideDockIcon);
                writeProperty(xout, "NSHighResolutionCapable", highResolutionCapable);
                writeProperty(xout, "NSSupportsAutomaticGraphicsSwitching",
                        supportsAutomaticGraphicsSwitching);
                writeProperty(xout, "IgnorePSN", ignorePSN);

                if (registeredProtocols.size() > 0) {
                    writeKey(xout, "CFBundleURLTypes");
                    xout.writeStartElement(ARRAY_TAG);
                    xout.writeCharacters("\n");
                    xout.writeStartElement(DICT_TAG);
                    xout.writeCharacters("\n");

                    writeProperty(xout, "CFBundleURLName", identifier);
                    writeStringArray(xout, "CFBundleURLSchemes", registeredProtocols);

                    xout.writeEndElement();
                    xout.writeCharacters("\n");
                    xout.writeEndElement();
                    xout.writeCharacters("\n");
                }

                //TODO(AR) not yet supported!
                // Write runtime
//                if (runtime != null) {
//                    writeProperty(xout, "JVMRuntime", runtime.getDir().getParentFile().getParentFile().getName());
//                }

                if (jvmRequired != null) {
                    writeProperty(xout, "JVMVersion", jvmRequired);
                }

                writeProperty(xout, "JVMRunPrivileged", privileged);

                writeProperty(xout, "JREPreferred", jrePreferred);
                writeProperty(xout, "JDKPreferred", jdkPreferred);

                writeProperty(xout, "WorkingDirectory", workingDirectory);

                // Write jnlp launcher name - only if set
                writeProperty(xout, "JVMJNLPLauncher", jnlpLauncherName);

                // Write main class name - only if set. There should only one be set
                writeProperty(xout, "JVMMainClassName", mainClassName);

                // Write classpaths in plist, if specified
                if (explicitClassPath) {
                    writeStringArray(xout, "JVMClassPath", plistClassPath(javaDirectory));
                }

                // Write whether launcher be verbose with debug msgs
                writeProperty(xout, "JVMDebug", isDebug);

                // Write jar launcher name
                writeProperty(xout, "JVMJARLauncher", jarLauncherName);

                // Write CFBundleDocument entries
                writeKey(xout, "CFBundleDocumentTypes");
                writeBundleDocuments(xout, bundleDocuments);

                // Write Type Declarations
                if (!exportedTypeDeclarations.isEmpty()) {
                    writeKey(xout, "UTExportedTypeDeclarations");
                    writeTypeDeclarations(xout, exportedTypeDeclarations);
                }
                if (!importedTypeDeclarations.isEmpty()) {
                    writeKey(xout, "UTImportedTypeDeclarations");
                    writeTypeDeclarations(xout, importedTypeDeclarations);
                }

                // Write architectures
                writeStringArray(xout, "LSArchitecturePriority", architectures);

                // Write Environment
                writeKey(xout, "LSEnvironment");
                xout.writeStartElement(DICT_TAG);
                xout.writeCharacters("\n");
                writeKey(xout, "LC_CTYPE");
                writeString(xout, "UTF-8");

                for (final Map.Entry<String, String> environment : environments.entrySet()) {
                    writeProperty(xout, environment.getKey(), environment.getValue());
                }

                xout.writeEndElement();
                xout.writeCharacters("\n");

                // Write options
                writeKey(xout, "JVMOptions");

                xout.writeStartElement(ARRAY_TAG);
                xout.writeCharacters("\n");

                for (final Option option : options) {
                    if (option.getName() == null) {
                        writeString(xout, option.getValue());
                    }
                }

                xout.writeEndElement();
                xout.writeCharacters("\n");

                // Write default options
                writeKey(xout, "JVMDefaultOptions");

                xout.writeStartElement(DICT_TAG);
                xout.writeCharacters("\n");

                for (final Option option : options) {
                    if (option.getName() != null) {
                        writeProperty(xout, option.getName(), option.getValue());
                    }
                }

                xout.writeEndElement();
                xout.writeCharacters("\n");

                // Write arguments
                writeStringArray(xout, "JVMArguments", arguments);

                // Write arbitrary key-value pairs
                for (final PlistEntry plistEntry : plistEntries) {
                    writeKey(xout, plistEntry.getKey());
                    writeValue(xout, plistEntry.getType(), plistEntry.getValue());
                }

                // End root dictionary
                xout.writeEndElement();
                xout.writeCharacters("\n");

                // End root element
                xout.writeEndElement();
                xout.writeCharacters("\n");

                // Close document
                xout.writeEndDocument();
                xout.writeCharacters("\n");

            } finally {
                xout.close();
            }

            out.flush();
        } catch (XMLStreamException exception) {
            throw new IOException(exception);
        }

        getLog().info("Wrote Info.plist: " + file.toAbsolutePath().toString());
    }

    private void writeKey(final XMLStreamWriter xout, final String key) throws XMLStreamException {
        xout.writeStartElement(KEY_TAG);
        xout.writeCharacters(key);
        xout.writeEndElement();
        xout.writeCharacters("\n");
    }

    private void writeValue(final XMLStreamWriter xout, String type, final String value)
            throws XMLStreamException {
        if (type == null) {
            type = STRING_TAG;
        }
        if ("boolean".equals(type)) {
            writeBoolean(xout, "true".equals(value));
        } else {
            xout.writeStartElement(type);
            xout.writeCharacters(value);
            xout.writeEndElement();
            xout.writeCharacters("\n");
        }
    }

    private void writeString(XMLStreamWriter xout, String value) throws XMLStreamException {
        xout.writeStartElement(STRING_TAG);
        xout.writeCharacters(value);
        xout.writeEndElement();
        xout.writeCharacters("\n");
    }

    private void writeBoolean(final XMLStreamWriter xout, final boolean value) throws XMLStreamException {
        xout.writeEmptyElement(value ? "true" : "false");
        xout.writeCharacters("\n");
    }

    private void writeProperty(final XMLStreamWriter xout, final String key, final Boolean value)
            throws XMLStreamException {
        if (value != null && value) {
            writeKey(xout, key);
            writeBoolean(xout, true);
        }
    }

    private void writeProperty(final XMLStreamWriter xout, final String key, final Object value)
            throws XMLStreamException {
        if (value != null) {
            writeKey(xout, key);
            writeString(xout, value.toString());
        }
    }

    public void writeStringArray(final XMLStreamWriter xout, final String key,
            final Iterable<String> values) throws XMLStreamException {
        if (values != null) {
            writeKey(xout, key);
            xout.writeStartElement(ARRAY_TAG);
            xout.writeCharacters("\n");
            for(String singleValue : values) {
                writeString(xout, singleValue);
            }
            xout.writeEndElement();
            xout.writeCharacters("\n");
        }
    }

    public void writeBundleDocuments(
            final XMLStreamWriter xout, final List<BundleDocument> bundleDocuments) throws XMLStreamException {

        xout.writeStartElement(ARRAY_TAG);
        xout.writeCharacters("\n");

        for (final BundleDocument bundleDocument: bundleDocuments) {
            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");

            final List<String> contentTypes = bundleDocument.getContentTypes();
            if (contentTypes != null) {
                writeStringArray(xout, "LSItemContentTypes", contentTypes);
            } else {
                writeStringArray(xout, "CFBundleTypeExtensions", bundleDocument.getExtensions());
                writeProperty(xout, "LSTypeIsPackage", bundleDocument.isPackage());
            }
            writeStringArray(xout, "NSExportableTypes", bundleDocument.getExportableTypes());

            final File ifile = bundleDocument.getIconFile();
            writeProperty(xout, "CFBundleTypeIconFile", ifile != null ?
                    ifile.getName() : bundleDocument.getIcon());

            writeProperty(xout, "CFBundleTypeName", bundleDocument.getName());
            writeProperty(xout, "CFBundleTypeRole", bundleDocument.getRole());
            writeProperty(xout, "LSHandlerRank", bundleDocument.getHandlerRank());

            xout.writeEndElement();
            xout.writeCharacters("\n");
        }

        xout.writeEndElement();
        xout.writeCharacters("\n");
    }

    public void writeTypeDeclarations(final XMLStreamWriter xout,
            final ArrayList<TypeDeclaration> typeDeclarations) throws XMLStreamException {
        xout.writeStartElement(ARRAY_TAG);
        xout.writeCharacters("\n");
        for (final TypeDeclaration typeDeclaration: typeDeclarations) {

            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");

            writeProperty(xout, "UTTypeIdentifier", typeDeclaration.getIdentifier());
            writeProperty(xout, "UTTypeReferenceURL", typeDeclaration.getReferenceUrl());
            writeProperty(xout, "UTTypeDescription", typeDeclaration.getDescription());

            final File ifile = typeDeclaration.getIconFile();
            writeProperty(xout, "UTTypeIconFile", ifile != null ?
                    ifile.getName() : typeDeclaration.getIcon());

            writeStringArray(xout, "UTTypeConformsTo", typeDeclaration.getConformsTo());

            writeKey(xout, "UTTypeTagSpecification");

            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");

            writeStringArray(xout, "com.apple.ostype", typeDeclaration.getOsTypes());
            writeStringArray(xout, "public.filename-extension", typeDeclaration.getExtensions());
            writeStringArray(xout, "public.mime-type", typeDeclaration.getMimeTypes());

            xout.writeEndElement();
            xout.writeCharacters("\n");

            xout.writeEndElement();
            xout.writeCharacters("\n");
        }

        xout.writeEndElement();
        xout.writeCharacters("\n");
    }

    private void writePkgInfo(final Path file) throws IOException {
        try (final Writer out = Files.newBufferedWriter(file, UTF_8)) {
            out.write(OS_TYPE_CODE + signature);
            out.flush();
        }

        getLog().info("Wrote PkgInfo: " + file.toAbsolutePath().toString());
    }
}
