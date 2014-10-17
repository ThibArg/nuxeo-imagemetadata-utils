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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.storage.StorageBlob;
import org.nuxeo.imagemetadata.ImageMetadataConstants.*;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;

public class ImageMetadataReader {

    // private static final Log log =
    // LogFactory.getLog(ImageMetadataReader.class);

    protected String filePath;

    public ImageMetadataReader(Blob inBlob) throws IOException {

        // We try to directly get the full path of the binary, if any.
        // This blob is not a storage blob (not stored with a document)
        // Maybe it only exists in memory, in a stream,
        // ***Should find an API to extract the path, if any****
        // In the meantime, let's create a temp file. It's duplicating, but,
        // well. No choices
        // at the time this is written

        try {
            StorageBlob sb = (StorageBlob) inBlob;
            filePath = ((FileSource) sb.getBinary().getStreamSource()).getFile().getAbsolutePath();
        } catch (Exception e) {
            File tempFile = File.createTempFile("IMDR-", "");
            inBlob.transferTo(tempFile);
            filePath = tempFile.getAbsolutePath();
            tempFile.deleteOnExit();
            Framework.trackFile(tempFile, this);
        }
    }

    public ImageMetadataReader(String inFullPath) {
        filePath = inFullPath;
    }

    public String getAllMetadata() throws InfoException {
        String result = "";

        Info imageInfo = new Info(filePath);

        Enumeration<String> props = imageInfo.getPropertyNames();
        while (props.hasMoreElements()) {
            String propertyName = props.nextElement();
            result += propertyName + "=" + imageInfo.getProperty(propertyName)
                    + "\n";
        }

        return result;
    }

    /*
     * If inTheseKeys is null or its lenght is 0, then we return all values
     * defined in METADATA_KEYS (not all the values returned. Just the one in
     * METADATA_KEYS).
     *
     * When a value is returned as null (the key does not exist), it is
     * realigned to the empty string "".
     */
    public HashMap<String, String> getMetadata(String[] inTheseKeys)
            throws InfoException {
        Info imageInfo = new Info(filePath);

        HashMap<String, String> result = new HashMap<String, String>();

        if (inTheseKeys == null) {
            for (METADATA_KEYS oneProp : METADATA_KEYS.values()) {
                String value = imageInfo.getProperty(oneProp.toString());
                if (value == null) {
                    value = "";
                }
                result.put(oneProp.toString(), value);
            }
        } else {
            for (String oneProp : inTheseKeys) {
                String value = imageInfo.getProperty(oneProp.toString());
                if (value == null) {
                    value = "";
                }
                result.put(oneProp, value);
            }
        }

        // Handle special case(s)
        // - Re-align resolution to 72x72 for GIF
        String keyResolution = METADATA_KEYS.RESOLUTION.toString();
        if (result.containsKey(keyResolution)
                && result.get(keyResolution).isEmpty()) {
            String format = imageInfo.getProperty(METADATA_KEYS.FORMAT.toString());
            format = format.toLowerCase();
            if (format.indexOf("gif") == 0) {
                result.put(keyResolution, "72x72");
            }
        }

        return result;
    }

    /*
     * If inTheseKeys is null, then we return all values defined in METADATA
     * (not all the values returned. Just the one in METADATA).
     *
     * The values returned are always String. Must be converted when needed. The
     * only conversion the method does is to find "null" values and set them to
     * ""
     */
    public HashMap<METADATA_KEYS, String> getMetadata(
            METADATA_KEYS[] inTheseKeys) throws InfoException {

        Info imageInfo = new Info(filePath);

        // Get the values
        HashMap<METADATA_KEYS, String> result = new HashMap<METADATA_KEYS, String>();
        if (inTheseKeys == null) {
            for (METADATA_KEYS oneProp : METADATA_KEYS.values()) {
                String value = imageInfo.getProperty(oneProp.toString());
                if (value == null) {
                    value = "";
                }
                result.put(oneProp, value);
            }
        } else {
            for (METADATA_KEYS oneProp : inTheseKeys) {
                String value = imageInfo.getProperty(oneProp.toString());
                if (value == null) {
                    value = "";
                }
                result.put(oneProp, value);
            }
        }

        // Handle special case(s)
        // - Re-align resolution to 72x72 for GIF
        if (result.containsKey(METADATA_KEYS.RESOLUTION)
                && result.get(METADATA_KEYS.RESOLUTION).isEmpty()) {
            String format = imageInfo.getProperty(METADATA_KEYS.FORMAT.toString());
            format = format.toLowerCase();
            if (format.indexOf("gif") == 0) {
                result.put(METADATA_KEYS.RESOLUTION, "72x72");
            }
        }

        return result;

    }
}
