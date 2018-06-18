import de.embl.cba.drosophila.Transforms;
import de.embl.cba.drosophila.Utils;
import de.embl.cba.drosophila.dapi.DapiRegistration;
import de.embl.cba.drosophila.dapi.DapiRegistrationSettings;
import de.embl.cba.drosophila.shavenbaby.ShavenBabyRegistration;
import de.embl.cba.drosophila.shavenbaby.ShavenBabyRegistrationSettings;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import static de.embl.cba.drosophila.dapi.DapiRegistration.asRealPointList;
import static de.embl.cba.drosophila.viewing.BdvImageViewer.show;

public class ShavenBabyRegistrationTest
{
	public static <T extends RealType<T> & NativeType<T>> void main( String... args)
	{
		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();

		final String path = ShavenBabyRegistrationTest.class.getResource( "/ShavenBaby-Iso4um.zip" ).getFile();

		final ImagePlus imp = IJ.openImage( path );
		double[] calibration = Utils.getCalibration( imp );
		final RandomAccessibleInterval< T > input = ImageJFunctions.wrap( imp );

		ShavenBabyRegistrationSettings settings = new ShavenBabyRegistrationSettings();
		settings.showIntermediateResults = true;

		ShavenBabyRegistration registration = new ShavenBabyRegistration( settings );

		final AffineTransform3D registrationTransform = registration.computeRegistration( input, calibration );

//		show( Transforms.createTransformedView( input, registrationTransform ),
//				"registered input ( " + settings.finalResolutionInMicrometer + " um )",
//				asRealPointList( new Point( 0,0,0 ) ),
//				new double[]{ 1, 1, 1 },
//				false );

	}

}
