/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fest;

import fr.jmmc.oitools.image.FitsImage;
import fr.jmmc.oitools.image.FitsUnit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.fest.swing.annotation.GUITest;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OImagingCreateImageValuesTest extends OImagingFestBase {

    public static File DYNAMIC_FITS_FOLDER = new File("./test/list/");

    public static File SAMPLE_FOLDER = new File(getProjectFolderPath() + "samples/input/");
    public static String SAMPLE_PATH = "2004-FKV1137-L1L2-example.fits";
    public static double SAMPLE_FOV = 945.3800000000001;
    public static double SAMPLE_INC = 0.8516936936936937;
    public static int SAMPLE_PIX = 1110;

    @Test
    @GUITest
    public void m1() {

        // test static sample file
        loadOIFitsFile(SAMPLE_FOLDER, new File(SAMPLE_FOLDER.getAbsolutePath() + "/" + SAMPLE_PATH));
        createImageDefault();
        FovIncPix fovIncPix = getFovIncPixInitImage();
        Assert.assertEquals(SAMPLE_FOV, fovIncPix.fov, 1e-8);
        Assert.assertEquals(SAMPLE_INC, fovIncPix.inc, 1e-8);
        Assert.assertEquals(SAMPLE_PIX, fovIncPix.pix);

        // test dynamic files and put numbers in results.txt
        // these numbers are meant to be checked manually by the user
        try ( FileWriter fileWriter = new FileWriter(DYNAMIC_FITS_FOLDER.getAbsolutePath() + "/results.txt");  BufferedWriter buffWriter = new BufferedWriter(fileWriter)) {

            for (String path : DYNAMIC_FITS_FOLDER.list()) {
                if (path.endsWith(".fits") || path.endsWith(".oifits")) {
                    loadOIFitsFile(DYNAMIC_FITS_FOLDER, new File(path));
                    createImageDefault();
                    FovIncPix fovIncPix2 = getFovIncPixInitImage();
                    writeFovIncPixToFile(fovIncPix2, path, buffWriter);
                }
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static class FovIncPix {

        public double fov;
        public double inc;
        public int pix;
    }

    public FovIncPix getFovIncPixInitImage() {
        FitsImage initImage = getInitImage();
        FovIncPix fovIncPix = new FovIncPix();
        fovIncPix.fov = FitsUnit.ANGLE_RAD.convert(initImage.getArea().width, FitsUnit.ANGLE_MILLI_ARCSEC);
        fovIncPix.inc = FitsUnit.ANGLE_RAD.convert(initImage.getIncCol(), FitsUnit.ANGLE_MILLI_ARCSEC);
        fovIncPix.pix = initImage.getNbCols();
        return fovIncPix;
    }

    public void writeFovIncPixToFile(FovIncPix fovIncPix, String path, BufferedWriter buffWriter) throws IOException {
        buffWriter.write(String.format("fov %8f inc %8f pix %5d file %s",
                fovIncPix.fov, fovIncPix.inc, fovIncPix.pix, path));
        buffWriter.newLine();
    }
}
