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

import java.util.HashMap;

import org.im4java.core.InfoException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.imagemetadata.ImageMetadataConstants.*;

/**
 *  The operation gets the binary of the document (main file, stored in file:content) and
 *  extract the metadata to store it in the "imagemetadata" schema, which is a schema
 *  defined by nuxeo.
 *  You could store the data in another schema (maybe a custom schema with only these
 *  info for example), we use this one because it is convenient and already here.
 */
@Operation(id = SavePictureMeadataInDocument.ID, category = Constants.CAT_DOCUMENT, label = "Save Picture Metadata in Document", description = "Extract widht height, resolution and colorspace from the picture file, and save the values in the <code>image_metadata</code> schema")
public class SavePictureMeadataInDocument {

    public static final String ID = "ExtractMetadataInDocument";

    //private static final Log log = LogFactory.getLog(ExtractMetadataInDocument.class);

    @Context
    protected CoreSession session;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws PropertyException,
            ClientException, InfoException {
        // We do nothing if we don't have the correct kind of document.
        // We could return an error, but we are more generic here,
        // avoiding an hassle to the caller (checking the facet and
        // calling us only if the document is ok)
        // (could log something, maybe)
        if (inDoc.isImmutable() || !inDoc.hasSchema("image_metadata") || ! inDoc.hasSchema("file")) {
            return inDoc;
        }

        // Get the main blob
        Blob theBlob = (Blob) inDoc.getPropertyValue("file:content");
        if (theBlob == null) {
            BlobHolder bh = inDoc.getAdapter(BlobHolder.class);
            if (bh != null) {
                theBlob = bh.getBlob();
            }
        }
        // We also give up silently if there is no binary
        if (theBlob == null) {
            return inDoc;
        }

        // Now, get the metadata. Only some fields
        ImageMetadataReader imdr = new ImageMetadataReader(theBlob);
        METADATA_KEYS[] keys = { METADATA_KEYS.WIDTH, METADATA_KEYS.HEIGHT,
                            METADATA_KEYS.COLORSPACE, METADATA_KEYS.RESOLUTION,
                            METADATA_KEYS.UNITS };
        HashMap<METADATA_KEYS, String> result = imdr.getMetadata(keys);

        // Store the values in the schema
        inDoc.setPropertyValue("imd:pixel_xdimension",
                                result.get(METADATA_KEYS.WIDTH));
        inDoc.setPropertyValue("imd:pixel_ydimension",
                                result.get(METADATA_KEYS.HEIGHT));
        inDoc.setPropertyValue("imd:color_space",
                                result.get(METADATA_KEYS.COLORSPACE));

        // Resolution needs extra work
        XYResolutionDPI dpi = new XYResolutionDPI(
                                result.get(METADATA_KEYS.RESOLUTION),
                                result.get(METADATA_KEYS.UNITS));
        inDoc.setPropertyValue("imd:xresolution", dpi.getX());
        inDoc.setPropertyValue("imd:yresolution", dpi.getY());

        // Save the document
        session.saveDocument(inDoc);

        return inDoc;
    }

}
