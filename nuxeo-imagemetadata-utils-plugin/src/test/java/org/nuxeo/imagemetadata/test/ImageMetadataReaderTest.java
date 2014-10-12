/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.imagemetadata.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.imagemetadata.ImageMetadataConstants.RESOLUTION_UNITS;
import org.nuxeo.imagemetadata.ImageMetadataReader;
import org.nuxeo.imagemetadata.ImageMetadataConstants.METADATA_KEYS;
import org.nuxeo.imagemetadata.XYResolutionDPI;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class })
@Deploy({ "nuxeo-imagemetadata-utils" })
public class ImageMetadataReaderTest {

    private static final String IMAGE_GIF = "images/a.gif";

    private static final String IMAGE_JPEG = "images/a.jpg";

    private static final String IMAGE_PNG = "images/a.png";

    private static final String IMAGE_TIF = "images/a.tif";

    private static final METADATA_KEYS[] KEYS = { METADATA_KEYS.WIDTH,
            METADATA_KEYS.HEIGHT, METADATA_KEYS.COLORSPACE,
            METADATA_KEYS.RESOLUTION, METADATA_KEYS.UNITS };

    @Inject
    CoreSession coreSession;

    @Before
    public void setUp() {
        // Setup documents if needed, etc.
    }

    private void checkValues(String inWhichOne, String inWidth, String inHeight,
            String inColorspace, String inResolution, String inUnits, int xDPI,
            int yDPI) throws Exception {

        File theFile = FileUtils.getResourceFileFromContext(inWhichOne);
        ImageMetadataReader imdr = new ImageMetadataReader(theFile.getAbsolutePath());

        HashMap<METADATA_KEYS, String> result = imdr.getMetadata(KEYS);
        assertNotNull(inWhichOne, result);

        assertEquals(inWhichOne, inWidth, result.get(METADATA_KEYS.WIDTH));
        assertEquals(inWhichOne, inHeight, result.get(METADATA_KEYS.HEIGHT));
        assertEquals(inWhichOne, inColorspace, result.get(METADATA_KEYS.COLORSPACE));
        assertEquals(inWhichOne, inResolution, result.get(METADATA_KEYS.RESOLUTION));
        assertEquals(inWhichOne, inUnits, result.get(METADATA_KEYS.UNITS));

        // Resolution needs extra work
        XYResolutionDPI dpi = new XYResolutionDPI(
                result.get(METADATA_KEYS.RESOLUTION),
                result.get(METADATA_KEYS.UNITS));
        assertEquals(inWhichOne, xDPI, dpi.getX());
        assertEquals(inWhichOne, yDPI, dpi.getY());
    }

    @Test
    public void testAllImages() throws Exception {
        checkValues(IMAGE_PNG, "100", "100", "sRGB", "37.79x37.79", "PixelsPerCentimeter", 96, 96);
        checkValues(IMAGE_GIF, "328", "331", "sRGB", "72x72", "Undefined", 72, 72);
        checkValues(IMAGE_TIF, "438", "640", "sRGB", "72x72", "PixelsPerInch", 72, 72);
        checkValues(IMAGE_JPEG, "1597", "232", "sRGB", "96x96", "PixelsPerInch", 96, 96);
    }

    @Test
    public void testGetAllMetadata() throws Exception {
        File theFile = FileUtils.getResourceFileFromContext(IMAGE_PNG);
        ImageMetadataReader imdr = new ImageMetadataReader(theFile.getAbsolutePath());
        String all = imdr.getAllMetadata();
        assertTrue(all != null);
        assertTrue(!all.isEmpty());

        // Just for an example:
        assertTrue(all.indexOf("Format=PNG") > -1);
    }

    @Test
    public void testXYResolutionDPI() throws Exception {
        XYResolutionDPI xyDPI = new XYResolutionDPI("180x180", RESOLUTION_UNITS.PIXELS_PER_INCH.toString());
        assertEquals(180, xyDPI.getX());
        assertEquals(180, xyDPI.getY());

        xyDPI = new XYResolutionDPI("37.89x37.89", RESOLUTION_UNITS.PIXELS_PER_CENTIMETER.toString());
        assertEquals(96, xyDPI.getX());
        assertEquals(96, xyDPI.getY());

        xyDPI = new XYResolutionDPI("72x72", RESOLUTION_UNITS.UNDEFINED.toString());
        assertEquals(72, xyDPI.getX());
        assertEquals(72, xyDPI.getY());

        xyDPI = new XYResolutionDPI("", RESOLUTION_UNITS.PIXELS_PER_INCH.toString());
        assertEquals(0, xyDPI.getX());
        assertEquals(0, xyDPI.getY());
    }
}
