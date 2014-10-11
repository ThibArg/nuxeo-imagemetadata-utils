/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */
package org.nuxeo.imagemetadata;

public class ImageMetadataConstants {

    /*
     * The value of each item here must match the parameter of getProperty()
     * method of the im4java Info class expects. Which actually, is anything
     * contained in it. When a property does not exist, the returned value is
     * null ("Resolution" for a gif for example)
     */
    public static enum METADATA_KEYS {
        WIDTH("Width"), HEIGHT("Height"), RESOLUTION("Resolution"), COLORSPACE(
                "Colorspace"), UNITS("Units"), FORMAT("Format");

        private String stringValue = "";

        METADATA_KEYS(String inValue) {
            stringValue = inValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public boolean equals(String inStr) {
            return inStr == null ? false : inStr.equals(stringValue);
        }
    }

    public static enum RESOLUTION_UNITS {
        PIXELS_PER_CENTIMETER("PixelsPerCentimeter"), PIXELS_PER_INCH(
                "PixelsPerInch"), UNDEFINED("Undefined");

        private String stringValue = "";

        RESOLUTION_UNITS(String inValue) {
            stringValue = inValue;
        }

        public boolean equals(String inStr) {
            return inStr == null ? false : inStr.equals(stringValue);
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }
}
