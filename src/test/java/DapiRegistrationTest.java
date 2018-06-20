import de.embl.cba.drosophila.Transforms;
import de.embl.cba.drosophila.Utils;
import de.embl.cba.drosophila.dapi.DapiRegistration;
import de.embl.cba.drosophila.dapi.DapiRegistrationSettings;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import static de.embl.cba.drosophila.Utils.asRealPointList;
import static de.embl.cba.drosophila.viewing.BdvImageViewer.show;

public class DapiRegistrationTest
{
	public static <T extends RealType<T> & NativeType<T>> void main( String... args)
	{
		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();

		final String path = DapiRegistrationTest.class.getResource( "/DapiWithGut-Iso2um.zip" ).getFile();

		final ImagePlus imp = IJ.openImage( path );
		double[] calibration = Utils.getCalibration( imp );
		final RandomAccessibleInterval< T > dapi = ImageJFunctions.wrap( imp );

		DapiRegistrationSettings settings = new DapiRegistrationSettings();
		settings.showIntermediateResults = true;

		DapiRegistration registration = new DapiRegistration( settings );

		final AffineTransform3D registrationTransform = registration.computeRegistration( dapi, calibration );

		show( Transforms.createTransformedView( dapi, registrationTransform ),
				"registered input ( " + settings.finalResolutionInMicrometer + " um )",
				asRealPointList( new Point( 0,0,0 ) ),
				new double[]{ 1, 1, 1 },
				false );

	}

}
