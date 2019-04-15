/*
 * Much of this code was originally taken from
 * App Bundler class com.oracle.appbundler.BundleDocument
 * and some modifications made by Adam Retter.
 * The original copyright and license follows...
 */

/*
 * Copyright 2012, The Infinite Kind and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  The Infinite Kind designates this
 * particular file as subject to the "Classpath" exception as provided
 * by The Infinite Kind in the LICENSE file that accompanied this code.
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
import java.util.ArrayList;
import java.util.List;

/**
 * Represent a CFBundleDocument.
 */
public class BundleDocument implements IconContainer {

    @Parameter
    private String name = null;

    @Parameter
    private String role = "Editor";

    @Parameter
    private File icon = null;

    @Parameter
    private String handlerRank = null;

    @Parameter
    private List<String> extensions;

    @Parameter
    private List<String> contentTypes;

    @Parameter
    private List<String> exportableTypes;

    @Parameter
    private boolean isPackage = false;

    private String capitalizeFirst(final String string) {
        final char[] stringArray = string.toCharArray();
        stringArray[0] = Character.toUpperCase(stringArray[0]);
        return new String(stringArray);
    }

    public void setExtensions(final String extensionsString) throws MojoExecutionException {
        extensions = getListFromCommaSeparatedString(extensionsString, "Extensions", true);
    }

    public void setContentTypes(final String contentTypesString) throws MojoExecutionException {
        contentTypes = getListFromCommaSeparatedString(contentTypesString, "Content Types");
    }

    public void setExportableTypes(final String exportableTypesString) throws MojoExecutionException {
        exportableTypes = getListFromCommaSeparatedString(exportableTypesString, "Exportable Types");
    }

    public static List<String> getListFromCommaSeparatedString(final String listAsString,
                                                               final String attributeName) throws MojoExecutionException {
        return getListFromCommaSeparatedString(listAsString, attributeName, false);
    }

    public static List<String> getListFromCommaSeparatedString(final String listAsString,
                                                               final String attributeName, final boolean lowercase) throws MojoExecutionException {
        if(listAsString == null) {
            throw new MojoExecutionException(attributeName + " can't be null");
        }

        final String[] splittedListAsString = listAsString.split(",");
        final List<String> stringList = new ArrayList<>();

        for (final String extension : splittedListAsString) {
            String cleanExtension = extension.trim();
            if (lowercase) {
                cleanExtension = cleanExtension.toLowerCase();
            }
            if (cleanExtension.length() > 0) {
                stringList.add(cleanExtension);
            }
        }

        if (stringList.size() == 0) {
            throw new MojoExecutionException(attributeName + " list must not be empty");
        }
        return stringList;
    }

    public void setIcon(final File icon) {
        this.icon = icon;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setRole(final String role) {
        this.role = capitalizeFirst(role);
    }

    public void setHandlerRank(final String handlerRank) {
        this.handlerRank = capitalizeFirst(handlerRank);
    }

    public void setIsPackage(final String isPackageString) {
        if(isPackageString.trim().equalsIgnoreCase("true")) {
            this.isPackage = true;
        } else {
            this.isPackage = false;
        }
    }

    @Override
    public String getIcon() {
        return icon.getAbsolutePath();
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getHandlerRank() {
        return handlerRank;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public List<String> getExportableTypes() {
        return exportableTypes;
    }

    @Override
    public File getIconFile() {
        if (icon == null) {
            return null;
        }

        if (! icon.exists() || icon.isDirectory()) {
            return null;
        }

        return icon;
    }

    @Override
    public boolean hasIcon() {
        return icon != null;
    }

    public boolean isPackage() {
        return isPackage;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(getName());
        s.append(" ").append(getRole())
                .append(" ").append(getIcon())
                .append(" ").append(getHandlerRank())
                .append(" ");
        if (contentTypes != null) {
            for(String contentType : contentTypes) {
                s.append(contentType).append(" ");
            }
        }
        if (extensions != null) {
            for(String extension : extensions) {
                s.append(extension).append(" ");
            }
        }
        if (exportableTypes != null) {
            for(String exportableType : exportableTypes) {
                s.append(exportableType).append(" ");
            }
        }

        return s.toString();
    }
}
