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

import java.io.IOException;
import java.util.HashMap;

import org.im4java.core.InfoException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.imagemetadata.ImageMetadataConstants.*;
import org.nuxeo.imagemetadata.ImageMetadataReader.WHICH_TOOL;

/**
 * Extract the metadata from the picture stored in the <code>xpath</code> field.
 * <p>
 * <code>properties</code> (optional) contains a list of
 * <code>xpath=Metadata Key</code> where Metadata Key is the exact name (case
 * sensitive) of a property to retrieve. For example:
 * <p>
 * <code>dc:format=Format</code>
 * <p>
 * If you use ExifTool, remember the tabs are not the same as in ImageMagick.
 * Also, even if ExifTool is not case sensitive for the tags, this operation is,
 * because it handles key=value
 * <p>
 * If <code>properties</code> is not used, the operation extracts
 * <code>width</code>, <code>height</code>, <code>resolution</code> and
 * <code>colorspace</code> from the picture file, and save the values in the
 * <code>image_metadata</code> schema (the DPI is realigned if needed.)There is
 * a special property: If you pass <code>schemaprefix:field=all</code>, then all
 * the properties are returned (the field must be a String field)
 * <p>
 * <i>IMPORTANT</i>: When <code>properties</code>, is empty it is not possible
 * to use ExifTool.
 *
 */
@Operation(id = SavePictureMeadataInDocument.ID, category = Constants.CAT_DOCUMENT, label = "Save Picture Metadata in Document", description = "Extract the metadata from the picture stored in the <code>xpath</code> field. <code>properties</code> (optional) contains a list of <code>xpath=Metadata Key</code> where Metadata Key is the exact name (case sensitive) of a property to retrieve. For example: <code>dc:format=Format</code>If <code>properties</code> is not used, the operation extracts <code>width</code>, <code>height</code>, <code>resolution</code> and <code>colorspace</code> from the picture file, and save the values in the <code>image_metadata</code> schema (the DPI is realigned if needed.)There is a special property: If you pass <code>schemaprefix:field=all</code>, then all the properties are returned (the field must be a String field)")
public class SavePictureMeadataInDocument {

    public static final String ID = "ExtractMetadataInDocument";

    // private static final Log log =
    // LogFactory.getLog(ExtractMetadataInDocument.class);

    @Context
    protected CoreSession session;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    // The map has the xpath as key and the metadata property as value:
    // dc:description=Colorspace
    // dc:format =Format
    // dc:nature=Units
    // . . .
    @Param(name = "properties", required = false)
    protected Properties properties;

    @Param(name = "tool", required = false, widget = Constants.W_OPTION, values = {
            "ImageMagick", "GraphicsMagick", "ExifTool" })
    String tool = "ImageMagick";

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws ClientException,
            IOException, InfoException {
        // We do nothing if we don't have the correct kind of document.
        // We could return an error, but we are more generic here,
        // avoiding an hassle to the caller.
        // If properties parameter is not used, we check the document has the
        // picture_metadata schema
        boolean hasProperties = properties != null && properties.size() > 0;
        if (inDoc.isImmutable()
                || (!hasProperties && !inDoc.hasSchema("image_metadata"))) {
            return inDoc;
        }

        ImageMetadataReader.WHICH_TOOL toolToUse = null;
        switch (tool.toLowerCase()) {
        case "graphicsmagick":
            toolToUse = WHICH_TOOL.GRAPHICSMAGICK;
            break;

        case "exiftool":
            toolToUse = WHICH_TOOL.EXIFTOOL;
            break;

        default:
            toolToUse = WHICH_TOOL.IMAGEMAGICK;
            break;
        }

        // Get the blob
        // We also give up silently if there is no binary
        Blob theBlob = null;
        try {
            theBlob = (Blob) inDoc.getPropertyValue(xpath);
        } catch (PropertyException e) {
            return inDoc;
        }
        if (theBlob == null) {
            return inDoc;
        }

        // If we have a key-value map, use it.
        // Else, we just get width, height, resolution and color space and
        // store the values in the image_metadata fields
        ImageMetadataReader imdr = new ImageMetadataReader(theBlob);
        HashMap<String, String> result = null;
        if (hasProperties) {
            String xpathForAll = "";

            // The names of the metadata properties are stored as values in the
            // map
            String[] keysStr = new String[properties.size()];
            int idx = 0;
            for (String inXPath : properties.keySet()) {
                keysStr[idx] = properties.get(inXPath);
                if (keysStr[idx].toLowerCase().equals("all")) {
                    xpathForAll = inXPath;
                }

                idx += 1;
            }
            result = imdr.getMetadata(keysStr, toolToUse);
            for (String inXPath : properties.keySet()) {
                String value = result.get(properties.get(inXPath));

                String theType = utils_getBasePropertyType(inDoc.getProperty(inXPath));
                if(theType.equals("int") || theType.equals("long")) {
                    if(value.isEmpty()) {
                        value = "0";
                    }
                    long v = Math.round(Double.valueOf(value));
                    value = "" + v;
                } else if(theType.equals("float")) {
                    if(value.isEmpty()) {
                        value = "0.0";
                    }
                    float v = Float.valueOf(value);
                    value = "" + v;
                } else if(theType.equals("double")) {
                    if(value.isEmpty()) {
                        value = "0.0";
                    }
                    double v = Double.valueOf(value);
                    value = "" + v;
                }
                inDoc.setPropertyValue(inXPath, value);
            }

            if (!xpathForAll.isEmpty()) {
                inDoc.setPropertyValue(xpathForAll, imdr.getAllMetadata());
            }

        } else {

            String[] keysStr = { KEYS.WIDTH, KEYS.HEIGHT, KEYS.COLORSPACE,
                    KEYS.RESOLUTION, KEYS.UNITS };
            result = imdr.getMetadata(keysStr);

            // Store the values in the schema
            inDoc.setPropertyValue("imd:pixel_xdimension",
                    result.get(KEYS.WIDTH));
            inDoc.setPropertyValue("imd:pixel_ydimension",
                    result.get(KEYS.HEIGHT));
            inDoc.setPropertyValue("imd:color_space",
                    result.get(KEYS.COLORSPACE));

            // Resolution needs extra work
            XYResolutionDPI dpi = new XYResolutionDPI(
                    result.get(KEYS.RESOLUTION), result.get(KEYS.UNITS));
            inDoc.setPropertyValue("imd:xresolution", dpi.getX());
            inDoc.setPropertyValue("imd:yresolution", dpi.getY());
        }

        // Save the document
        if (save) {
            session.saveDocument(inDoc);
        }

        return inDoc;
    }

    protected String utils_getBasePropertyType(Property inProp) {

        Type t = inProp.getType();
        String theType;

        do {
            theType = t.getName();
            t = t.getSuperType();
        } while(t != null);

        return theType;
    }
}
