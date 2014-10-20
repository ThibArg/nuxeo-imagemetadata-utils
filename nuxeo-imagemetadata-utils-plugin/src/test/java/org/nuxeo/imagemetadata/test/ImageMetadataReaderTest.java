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

package org.nuxeo.imagemetadata.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.StringReader;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.imagemetadata.ImageMetadataConstants.*;
import org.nuxeo.imagemetadata.ExtractXMPFromBlobOp;
import org.nuxeo.imagemetadata.ImageMetadataReader;
import org.nuxeo.imagemetadata.SavePictureMeadataInDocument;
import org.nuxeo.imagemetadata.XYResolutionDPI;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.picture.core", "nuxeo-imagemetadata-utils",
        "org.nuxeo.ecm.platform.commandline.executor" })
public class ImageMetadataReaderTest {

    private static final String IMAGE_GIF = "images/a.gif";

    private static final String IMAGE_JPEG = "images/a.jpg";

    private static final String IMAGE_PNG = "images/a.png";

    private static final String IMAGE_TIF = "images/a.tif";

    private static final String NUXEO_LOGO = "images/Nuxeo.png";

    private static final String WITH_XMP = "images/with-xmp.jpg";

    protected File filePNG;

    protected File fileGIF;

    protected File fileTIF;

    protected File fileJPEG;

    protected DocumentModel parentOfTestDocs;

    protected DocumentModel docPNG;

    protected DocumentModel docGIF;

    protected DocumentModel docTIF;

    protected DocumentModel docJPEG;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService service;

    @Before
    public void setUp() {
        // Setup documents if needed, etc.
        filePNG = FileUtils.getResourceFileFromContext(IMAGE_PNG);
        fileGIF = FileUtils.getResourceFileFromContext(IMAGE_GIF);
        fileTIF = FileUtils.getResourceFileFromContext(IMAGE_TIF);
        fileJPEG = FileUtils.getResourceFileFromContext(IMAGE_JPEG);

        // Cleanup repo if needed and create the Picture documents
        // coreSession.removeChildren(coreSession.getRootDocument().getRef());
        parentOfTestDocs = coreSession.createDocumentModel("/",
                "test-pictures", "Folder");
        parentOfTestDocs.setPropertyValue("dc:title", "test-pictures");
        parentOfTestDocs = coreSession.createDocument(parentOfTestDocs);
        parentOfTestDocs = coreSession.saveDocument(parentOfTestDocs);

        docPNG = createPictureDocument(filePNG);
        docGIF = createPictureDocument(fileGIF);
        docTIF = createPictureDocument(fileTIF);
        docJPEG = createPictureDocument(fileJPEG);
        coreSession.save();
    }

    @After
    public void cleanup() {
        coreSession.removeDocument(parentOfTestDocs.getRef());
        coreSession.save();
    }

    protected DocumentModel createPictureDocument(File inFile) {

        DocumentModel pictDoc = coreSession.createDocumentModel(
                parentOfTestDocs.getPathAsString(), inFile.getName(), "Picture");
        pictDoc.setPropertyValue("dc:title", inFile.getName());
        pictDoc.setPropertyValue("file:content", new FileBlob(inFile));
        return coreSession.createDocument(pictDoc);

    }

    protected void checkValues(File inWhichOne, String inWidth,
            String inHeight, String inColorspace, String inResolution,
            String inUnits, int xDPI, int yDPI) throws Exception {

        String theAssertMessage = "getMetadata() for " + inWhichOne.getName();
        ImageMetadataReader imdr = new ImageMetadataReader(
                inWhichOne.getAbsolutePath());

        String[] keysStr = { KEYS.WIDTH, KEYS.HEIGHT, KEYS.COLORSPACE,
                KEYS.RESOLUTION, KEYS.UNITS };
        HashMap<String, String> result = imdr.getMetadata(keysStr);
        assertNotNull(theAssertMessage, result);

        assertEquals(theAssertMessage, inWidth, result.get(KEYS.WIDTH));
        assertEquals(theAssertMessage, inHeight, result.get(KEYS.HEIGHT));
        assertEquals(theAssertMessage, inColorspace,
                result.get(KEYS.COLORSPACE));
        assertEquals(theAssertMessage, inResolution,
                result.get(KEYS.RESOLUTION));
        assertEquals(theAssertMessage, inUnits, result.get(KEYS.UNITS));

        // Resolution needs extra work
        XYResolutionDPI dpi = new XYResolutionDPI(result.get(KEYS.RESOLUTION),
                result.get(KEYS.UNITS));
        assertEquals(theAssertMessage, xDPI, dpi.getX());
        assertEquals(theAssertMessage, yDPI, dpi.getY());
    }

    @Test
    public void testAllImages() throws Exception {
        checkValues(filePNG, "100", "100", "sRGB", "37.79x37.79",
                "PixelsPerCentimeter", 96, 96);
        checkValues(fileGIF, "328", "331", "sRGB", "72x72", "Undefined", 72, 72);
        checkValues(fileTIF, "438", "640", "sRGB", "72x72", "PixelsPerInch",
                72, 72);
        checkValues(fileJPEG, "1597", "232", "sRGB", "96x96", "PixelsPerInch",
                96, 96);
    }

    @Test
    public void testGetAllMetadata() throws Exception {
        ImageMetadataReader imdr = new ImageMetadataReader(
                filePNG.getAbsolutePath());

        // ==================================================
        // Test with metadata returned as a String
        // ==================================================
        String all = imdr.getAllMetadata();
        assertTrue(all != null);
        assertTrue(!all.isEmpty());

        // Just for an example:
        assertTrue(all.indexOf("Format=PNG (Portable Network Graphics)") > -1);
        assertTrue(all.indexOf("Channel depth:green=8-bit") > -1);

        // ==================================================
        // Test with the result as a hashmap
        // ==================================================
        HashMap<String, String> allInHashMap = imdr.getMetadata(null);
        assertTrue(allInHashMap.containsKey("Format"));
        assertEquals("PNG (Portable Network Graphics)",
                allInHashMap.get("Format"));
        assertTrue(allInHashMap.containsKey("Channel depth:green"));
        assertEquals("8-bit", allInHashMap.get("Channel depth:green"));
    }

    @Test
    public void testXYResolutionDPI() throws Exception {
        XYResolutionDPI xyDPI = new XYResolutionDPI("180x180",
                RESOLUTION_UNITS.PIXELS_PER_INCH);
        assertEquals(180, xyDPI.getX());
        assertEquals(180, xyDPI.getY());

        xyDPI = new XYResolutionDPI("37.89x37.89",
                RESOLUTION_UNITS.PIXELS_PER_CENTIMETER);
        assertEquals(96, xyDPI.getX());
        assertEquals(96, xyDPI.getY());

        xyDPI = new XYResolutionDPI("72x72", RESOLUTION_UNITS.UNDEFINED);
        assertEquals(72, xyDPI.getX());
        assertEquals(72, xyDPI.getY());

        xyDPI = new XYResolutionDPI("", RESOLUTION_UNITS.PIXELS_PER_INCH);
        assertEquals(0, xyDPI.getX());
        assertEquals(0, xyDPI.getY());
    }

    @Test
    public void testSavePictureMetadataInDocument() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        // ========================================
        // TEST WITH DEFAULT VALUES
        // ========================================
        String changeToken = docPNG.getChangeToken();
        ctx.setInput(docPNG);
        OperationChain chain = new OperationChain("testChain");
        // let default value for "xpath", "properties" and "save"
        chain.add(SavePictureMeadataInDocument.ID);
        service.run(ctx, chain);

        // Check the doc was modified
        assertNotSame(changeToken, docPNG.getChangeToken());

        // Check value for this PNG
        assertEquals((long) 100,
                docPNG.getPropertyValue("imd:pixel_xdimension"));
        assertEquals((long) 100,
                docPNG.getPropertyValue("imd:pixel_ydimension"));
        assertEquals("sRGB", docPNG.getPropertyValue("imd:color_space"));
        assertEquals((long) 96, docPNG.getPropertyValue("imd:xresolution"));
        assertEquals((long) 96, docPNG.getPropertyValue("imd:yresolution"));

        // ========================================
        // ASK FOR ALL PROPERTIES
        // ========================================
        changeToken = docPNG.getChangeToken();
        ctx.setInput(docPNG);
        chain = new OperationChain("testChain");
        // Let xpath and save the default values
        Properties props = new Properties();
        props.put("dc:description", "all");
        chain.add(SavePictureMeadataInDocument.ID).set("properties", props);
        service.run(ctx, chain);

        // Check the doc was modified
        assertNotSame(changeToken, docPNG.getChangeToken());

        // Check value for this PNG
        String all = (String) docPNG.getPropertyValue("dc:description");
        assertTrue(all != null && !all.isEmpty());
        // Possibly, check some values are available
        assertTrue(all.indexOf("Page geometry") > -1);
        assertTrue(all.indexOf("Units") > -1);

        // ========================================
        // NO SAVE MUST, WELL, NOT SAVE
        // ========================================
        changeToken = docPNG.getChangeToken();
        ctx.setInput(docPNG);
        chain = new OperationChain("testChain");
        // Let xpath and properties the default values
        chain.add(SavePictureMeadataInDocument.ID).set("save", false);
        service.run(ctx, chain);

        assertEquals(changeToken, docPNG.getChangeToken());

    }

    @Test
    public void testWhenDocIsNotSaved() throws Exception {
        File nuxeoFile = FileUtils.getResourceFileFromContext(NUXEO_LOGO);
        DocumentModel aPictDoc = coreSession.createDocumentModel(
                parentOfTestDocs.getPathAsString(), filePNG.getName(),
                "Picture");
        aPictDoc.setPropertyValue("dc:title", filePNG.getName());
        aPictDoc.setPropertyValue("file:content", new FileBlob(nuxeoFile));
        // We don't coreSession.createDocument(aPictDoc);
        // because we don't want the blob to be stored in the BinaryStore
        // (so it's not a StorageBlob)

        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        String changeToken = aPictDoc.getChangeToken();
        ctx.setInput(aPictDoc);
        OperationChain chain = new OperationChain("testChain");
        // Here too, we don't save
        chain.add(SavePictureMeadataInDocument.ID).set("save", false);
        service.run(ctx, chain);

        assertEquals(changeToken, aPictDoc.getChangeToken());
        assertEquals((long) 201,
                aPictDoc.getPropertyValue("imd:pixel_xdimension"));
    }

    @Test
    public void testXMP() throws Exception {

        ImageMetadataReader imdr;
        String xmp;

        // ==============================
        // Test on png file with no xmp
        // ==============================
        imdr = new ImageMetadataReader(filePNG.getAbsolutePath());
        xmp = imdr.getXMP();
        assertTrue(xmp.isEmpty());

        // ==============================
        // Test on file with xmp
        // ==============================
        File withXmpFile = FileUtils.getResourceFileFromContext(WITH_XMP);
        imdr = new ImageMetadataReader(withXmpFile.getAbsolutePath());
        xmp = imdr.getXMP();
        assertFalse(xmp.isEmpty());

        // Check it is a valid, well formed XML
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmp));
        Document doc = dBuilder.parse(is);
        assertEquals("x:xmpmeta", doc.getDocumentElement().getNodeName());

        // ==============================
        // Test the operation
        // ==============================
        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        // Using the file with xmp
        FileBlob fb = new FileBlob(withXmpFile);
        ctx.setInput(fb);
        chain = new OperationChain("testChain");
        chain.add(ExtractXMPFromBlobOp.ID).set("varName", "xmp");
        service.run(ctx, chain);

        // Check our "xmp" Context Variable is filled
        xmp = (String) ctx.get("xmp");
        assertFalse(xmp.isEmpty());

        // Check it is a valid, well formed XML
        // (we re-used the variables declared previously)
        dbFactory = DocumentBuilderFactory.newInstance();
        dBuilder = dbFactory.newDocumentBuilder();
        is = new InputSource(new StringReader(xmp));
        doc = dBuilder.parse(is);
        assertEquals("x:xmpmeta", doc.getDocumentElement().getNodeName());

    }
}
