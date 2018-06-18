package de.embl.cba.drosophila.dapigut;

import bdv.util.*;
import de.embl.cba.registration.utils.ImagePlusUtils;
import de.embl.cba.registration.utils.Transforms;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.app.StatusService;
import org.scijava.command.InteractiveCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;

import java.util.ArrayList;


@Plugin(type = InteractiveCommand.class, menuPath = "Plugins>Registration>EMBL>Drosophila Dapi" )
public class DapiRegistrationCommand<T extends RealType<T> & NativeType< T > > extends InteractiveCommand
{
	//
	// Services
	//

	@Parameter
	public UIService uiService;

	@Parameter
	public DatasetService datasetService;

	@Parameter
	public LogService logService;

//	@Parameter
//	public ThreadService threadService;

	@Parameter
	public OpService opService;

	@Parameter
	public StatusService statusService;

	//
	// Images
	//

	@Parameter
	public ImagePlus imagePlus;

	//
	// Settings
	//

	@Parameter
	public int dapiChannelIndexOneBased = 2;

	DapiRegistrationSettings settings = new DapiRegistrationSettings();

	@Parameter
	public boolean showIntermediateResults = settings.showIntermediateResults;

	@Parameter
	public double resolutionDuringRegistrationInMicrometer = settings.resolutionDuringRegistrationInMicrometer;

	@Parameter
	public double finalResolutionInMicrometer = settings.finalResolutionInMicrometer;

	@Parameter
	public double threshold = settings.threshold;

	@Parameter
	public double refractiveIndexCorrectionAxialScalingFactor = settings.refractiveIndexCorrectionAxialScalingFactor;

	@Parameter
	public int derivativeDeltaInMicrometer = settings.derivativeDeltaInMicrometer;

	@Parameter
	public double projectionRangeMinDistanceToCenterInMicrometer = settings.projectionRangeMinDistanceToCenterInMicrometer;

	@Parameter
	public double projectionRangeMaxDistanceToCenterInMicrometer = settings.projectionRangeMaxDistanceToCenterInMicrometer;

	@Parameter
	public double sigmaForBlurringAverageProjectionInMicrometer = settings.sigmaForBlurringAverageProjectionInMicrometer;

	//
	// Buttons
	//

	@Parameter(label = "Run", callback = "execute")
	private Button runButton;

	//
	// Constants
	//

	private int imagePlusChannelDimension = 2;


	public void run()
	{
		System.out.println( "Test" );
	}

//	public void init()
//	{
//
//	}

	public void execute()
	{

		settings.resolutionDuringRegistrationInMicrometer = resolutionDuringRegistrationInMicrometer;
		settings.refractiveIndexCorrectionAxialScalingFactor = refractiveIndexCorrectionAxialScalingFactor;
		settings.finalResolutionInMicrometer = finalResolutionInMicrometer;
		settings.showIntermediateResults = showIntermediateResults;
		settings.threshold = threshold;
		settings.derivativeDeltaInMicrometer = derivativeDeltaInMicrometer;
		settings.projectionRangeMaxDistanceToCenterInMicrometer = projectionRangeMaxDistanceToCenterInMicrometer;
		settings.projectionRangeMinDistanceToCenterInMicrometer = projectionRangeMinDistanceToCenterInMicrometer;
		settings.sigmaForBlurringAverageProjectionInMicrometer = sigmaForBlurringAverageProjectionInMicrometer;

		RandomAccessibleInterval< T > allChannels = ImageJFunctions.wrap( imagePlus );

		RandomAccessibleInterval< T > dapiChannel = Views.hyperSlice( allChannels, imagePlusChannelDimension, dapiChannelIndexOneBased - 1 );

		DapiRegistration dapiRegistration = new DapiRegistration( settings );

		final AffineTransform3D registrationTransform = dapiRegistration.computeRegistration( dapiChannel, ImagePlusUtils.getCalibration( imagePlus ) );

		ArrayList< RandomAccessibleInterval< T > > transformedChannels = new ArrayList<>(  );

		for ( int c = 0; c < allChannels.dimension( imagePlusChannelDimension ); ++c )
		{
			final RandomAccessibleInterval< T > channel = Views.hyperSlice( allChannels, imagePlusChannelDimension, c );
			transformedChannels.add( Transforms.view( channel, registrationTransform ) );
		}

		RandomAccessibleInterval< T > stack = Views.stack( transformedChannels );
//		transformedChannel = ViewsUtils.insertDimension( transformedChannel, 2 );
//		final ImagePlus channelImp = ImageJFunctions.wrap( transformedChannel,"channel " + ( c + 1 ) );
//		IJ.run( channelImp, "Properties...", "unit=micrometer pixel_width="+finalResolutionInMicrometer+" pixel_height="+finalResolutionInMicrometer+" voxel_depth="+finalResolutionInMicrometer);
//		channelImp.show();

		Bdv bdv = BdvFunctions.show( stack, "registered", BdvOptions.options().axisOrder( AxisOrder.XYZC ) );
		final ArrayList< RealPoint > points = new ArrayList<>();
		points.add( new RealPoint( new double[]{0,0,0} ));
		BdvFunctions.showPoints( points, "origin", BdvOptions.options().addTo( bdv ) );
		ImageJFunctions.show( Views.permute( stack, 2, 3 ) );

	}

	public static void main(final String... args) throws Exception
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 7-1 rotated.tif";

		// Load and show data
		ImagePlus imp = IJ.openImage( path );
		imp.show();

		// invoke the plugin
		ij.command().run( DapiRegistrationCommand.class, true );
	}

}
