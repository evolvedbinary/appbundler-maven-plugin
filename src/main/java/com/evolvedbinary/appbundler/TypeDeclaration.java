/*
 * Much of this code was originally taken from
 * App Bundler class com.oracle.appbundler.TypeDeclaration
 * and some modifications made by Adam Retter.
 * The original copyright and license follows...
 */

/*
 * Copyright 2015, Quality First Software GmbH and/or its affiliates. All rights reserved.
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
 */
package com.evolvedbinary.appbundler;

import com.oracle.appbundler.IconContainer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.evolvedbinary.appbundler.BundleDocument.getListFromCommaSeparatedString;

/**
 * Class representing an UTExportedTypeDeclaration or UTImportedTypeDeclaration in Info.plist
 */
public class TypeDeclaration implements IconContainer {

    @Parameter
    private boolean imported = false;

    @Parameter
    private String identifier = null;

    @Parameter
    private String referenceUrl = null;

    @Parameter
    private String description = null;

    @Parameter
    private File icon = null;

    @Parameter
    private List<String> conformsTo = null;

    @Parameter
    private List<String> osTypes = null;

    @Parameter
    private List<String> mimeTypes = null;

    @Parameter
    private List<String> extensions = null;

    public TypeDeclaration() {
        this.conformsTo = Arrays.asList(new String[]{"public.data"});
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(final boolean imported) {
        this.imported = imported;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public void setReferenceUrl(final String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getIcon() {
        return icon.getAbsolutePath();
    }

    public void setIcon(final File icon) {
        this.icon = icon;
    }

    @Override
    public File getIconFile() {
        if (icon == null) {
            return null;
        }

        if (!icon.exists() || icon.isDirectory()) {
            return null;
        }

        return icon;
    }

    @Override
    public boolean hasIcon() {
        return icon != null;
    }

    public List<String> getConformsTo() {
        return conformsTo;
    }

    public void setConformsTo(final String conformsToAsString) throws MojoExecutionException {
        this.conformsTo = getListFromCommaSeparatedString(conformsToAsString, "Conforms To");
    }

    public List<String> getOsTypes() {
        return osTypes;
    }

    public void setOsTypes(final String osTypesAsString) throws MojoExecutionException {
        this.osTypes = getListFromCommaSeparatedString(osTypesAsString, "OS Types");
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(final String mimeTypesAsString) throws MojoExecutionException {
        this.mimeTypes = getListFromCommaSeparatedString(mimeTypesAsString, "Mime Types", true);
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(final String extensionsAsString) throws MojoExecutionException {
        this.extensions = getListFromCommaSeparatedString(extensionsAsString, "Extensions", true);
    }

    @Override
    public String toString() {
        return "" + imported;
    }
}
