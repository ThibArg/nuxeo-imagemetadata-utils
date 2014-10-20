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
 *     Thibaud Arguillere
 */
package org.nuxeo.imagemetadata;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import org.im4java.core.ETOperation;
import org.im4java.core.ExiftoolCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.storage.StorageBlob;
import org.nuxeo.im4java.StringOutputConsumer;
import org.nuxeo.imagemetadata.ImageMetadataConstants.*;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;

public class ImageMetadataReader {

    protected String filePath = null;

    protected static int exiftoolAvailability = -1;

    protected static String whyExiftoolNotAvailable = "";

    public ImageMetadataReader(Blob inBlob) throws IOException {

        // We try to directly get the full path of the binary, if any,
        // to avoid creating a temporary file (assuming the i/o cost
        // would be > cost of the type casting)
        boolean needTempFile = false;
        String theClass = inBlob.getClass().getSimpleName();
        try {
            if (theClass.equals("StorageBlob")) {
                StorageBlob sb = (StorageBlob) inBlob;
                filePath = ((FileSource) sb.getBinary().getStreamSource()).getFile().getAbsolutePath();
            } else if (theClass.equals("FileBlob")) {
                FileBlob fb = (FileBlob) inBlob;
                filePath = fb.getFile().getAbsolutePath();
            } else if (theClass.equals("StreamingBlob")) {
                StreamingBlob sb = (StreamingBlob) inBlob;
                filePath = ((FileSource) sb.getStreamSource()).getFile().getAbsolutePath();
            }
        } catch (Exception e) {
            needTempFile = true;
        }

        if (needTempFile) {
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

    public static boolean isExiftoolAvailable() {

        if (exiftoolAvailability == -1) {

            try {
                Runtime.getRuntime().exec("exiftool -ver");
                exiftoolAvailability = 1;
            } catch (Exception e) {
                exiftoolAvailability = 0;
                whyExiftoolNotAvailable = e.getMessage();
            }
        }

        return exiftoolAvailability == 1;
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
     * If inTheseKeys is null or its length is 0, we return all properties
     *
     * When a value is returned as null (the key does not exist), it is
     * realigned to the empty string "".
     */
    public HashMap<String, String> getMetadata(String[] inTheseKeys)
            throws InfoException {

        HashMap<String, String> result = new HashMap<String, String>();
        Info imageInfo = new Info(filePath);

        if (inTheseKeys == null || inTheseKeys.length == 0) {

            Enumeration<String> props = imageInfo.getPropertyNames();
            while (props.hasMoreElements()) {
                String propertyName = props.nextElement();
                result.put(propertyName, imageInfo.getProperty(propertyName));
            }

        } else {
            for (String oneProp : inTheseKeys) {
                String value = imageInfo.getProperty(oneProp);
                if (value == null) {
                    value = "";
                }
                result.put(oneProp, value);
            }

            // Handle special case(s)
            // - Re-align resolution to 72x72 for GIF
            String keyResolution = KEYS.RESOLUTION;
            if (result.containsKey(keyResolution)
                    && result.get(keyResolution).isEmpty()) {
                String format = imageInfo.getProperty(KEYS.FORMAT);
                format = format.toLowerCase();
                if (format.indexOf("gif") == 0) {
                    result.put(keyResolution, "72x72");
                }
            }
        }
        return result;
    }

    public String getXMP() throws ClientException {

        if (!ImageMetadataReader.isExiftoolAvailable()) {
            throw new ClientException("ExifTool is not available: "
                    + whyExiftoolNotAvailable);
        }

        try {
            ETOperation op = new ETOperation();
            op.getTags("xmp", "b");
            op.addImage();

            // setup command and execute it (capture output)
            StringOutputConsumer output = new StringOutputConsumer();
            ExiftoolCmd et = new ExiftoolCmd();
            et.setOutputConsumer(output);
            et.run(op, filePath);
            return output.getOutput();

        } catch (IOException e) {
            throw new ClientException(e);
        } catch (InterruptedException e) {
            throw new ClientException(e);
        } catch (IM4JavaException e) {
            throw new ClientException(e);
        }
    }
}
