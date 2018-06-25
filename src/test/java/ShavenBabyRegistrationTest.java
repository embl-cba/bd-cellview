import de.embl.cba.drosophila.Utils;
import de.embl.cba.drosophila.shavenbaby.ShavenBabyRegistration;
import de.embl.cba.drosophila.shavenbaby.ShavenBabyRegistrationSettings;
import ij.IJ;
import ij.ImagePlus;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import loci.formats.FormatException;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImporterOptions;
import loci.plugins.in.ImportProcess;

import java.io.IOException;

import static de.embl.cba.drosophila.ImageReaders.openWithBioFormats;

public class ShavenBabyRegistrationTest
{
	public static <T extends RealType<T> & NativeType<T>> void main( String... args) throws FormatException, IOException
	{
		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();

		ShavenBabyRegistrationSettings settings = new ShavenBabyRegistrationSettings();
		settings.registrationResolution = 10;
		settings.showIntermediateResults = true;


//		final String path = ShavenBabyRegistrationTest.class.getResource( "/ShavenBaby01.zip" ).getFile();
//		final String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/E3NWT-04-svb-downScaled.tif";

		final String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/E3NWT-02-svb-downscaled.tif";
		final ImagePlus imagePlus = IJ.openImage( path );

//		final String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/E3NWT-02.czi";
//		final ImagePlus imagePlus = openWithBioFormats( path );
//		RandomAccessibleInterval< T > svb = Utils.copyAsArrayImg( Views.hyperSlice( input, Utils.imagePlusChannelDimension, settings.shavenBabyChannelIndex ) )

		final RandomAccessibleInterval< T > svb = ImageJFunctions.wrap( imagePlus );

		ShavenBabyRegistration registration = new ShavenBabyRegistration( settings, imagej );


		final AffineTransform3D registrationTransform = registration.computeRegistration( svb, Utils.getCalibration( imagePlus ) );

//		show( Transforms.createTransformedView( input, registrationTransform ),
//				"registered input ( " + settings.finalResolutionInMicrometer + " um )",
//				asRealPointList( new Point( 0,0,0 ) ),
//				new double[]{ 1, 1, 1 },
//				false );

	}

}
