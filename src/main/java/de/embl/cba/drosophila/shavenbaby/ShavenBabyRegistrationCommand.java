package de.embl.cba.drosophila.dapi;

import bdv.util.*;
import de.embl.cba.drosophila.Transforms;
import de.embl.cba.drosophila.Utils;
import de.embl.cba.drosophila.shavenbaby.ShavenBabyRegistration;
import de.embl.cba.drosophila.shavenbaby.ShavenBabyRegistrationSettings;
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
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.util.ArrayList;


@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>Drosophila Svb" )
public class ShavenBabyRegistrationCommand<T extends RealType<T> & NativeType< T > > implements Command
{
	@Parameter
	public UIService uiService;

	@Parameter
	public DatasetService datasetService;

	@Parameter
	public LogService logService;

	@Parameter
	public OpService opService;

	@Parameter
	public StatusService statusService;

	@Parameter( required = false )
	public ImagePlus imagePlus;

	ShavenBabyRegistrationSettings settings = new ShavenBabyRegistrationSettings();

	@Parameter
	public int shavenBabyChannelIndexOneBased = settings.shavenBabyChannelIndexOneBased;

	@Parameter
	public boolean showIntermediateResults = settings.showIntermediateResults;

	@Parameter
	public double registrationResolution = settings.registrationResolution;

	@Parameter
	public double outputResolution = settings.outputResolution;

	@Parameter
	public double thresholdAfterBackgroundSubtraction = settings.thresholdAfterBackgroundSubtraction;

	@Parameter
	public double refractiveIndexScalingCorrectionFactor = settings.refractiveIndexScalingCorrectionFactor;

	@Parameter
	public double refractiveIndexIntensityCorrectionDecayLength = settings.refractiveIndexIntensityCorrectionDecayLength;

	@Parameter
	public double minDistanceToAxisForRollAngleComputation = settings.minDistanceToAxisForRollAngleComputation;

	@Parameter
	public double watershedSeedsDistanceThreshold = settings.watershedSeedsDistanceThreshold;
	
	public void run()
	{

		setSettingsFromUI();

		final ShavenBabyRegistration registration = new ShavenBabyRegistration( settings, opService );

		if ( imagePlus != null )
		{
			RandomAccessibleInterval< T > transformed = createTransformedImage( imagePlus, registration );
			showWithBdv( transformed );
			ImageJFunctions.show( Views.permute( transformed, 2, 3 ) );
		}
		else
		{
			final File directory = uiService.chooseFile( null, FileWidget.DIRECTORY_STYLE );
			String[] files = directory.list();

			// for each name in the path array
			for( String file : files )
			{
				if ( file.endsWith( ".lsm" ) )
				{
					System.out.println( file );
				}
			}
		}


	}

	public void showWithBdv( RandomAccessibleInterval< T > transformed )
	{
		Bdv bdv = BdvFunctions.show( transformed, "registered", BdvOptions.options().axisOrder( AxisOrder.XYZC ) );
		final ArrayList< RealPoint > points = new ArrayList<>();
		points.add( new RealPoint( new double[]{0,0,0} ));
		BdvFunctions.showPoints( points, "origin", BdvOptions.options().addTo( bdv ) );
	}

	public RandomAccessibleInterval< T > createTransformedImage( ImagePlus imagePlus, ShavenBabyRegistration registration )
	{
		RandomAccessibleInterval< T > allChannels = ImageJFunctions.wrap( imagePlus );
		RandomAccessibleInterval< T > svb = Views.hyperSlice( allChannels, Utils.imagePlusChannelDimension,  shavenBabyChannelIndexOneBased - 1 );
		final AffineTransform3D registrationTransform = registration.computeRegistration( svb, Utils.getCalibration( imagePlus ) );
		return Transforms.transformMultipleChannels( allChannels, registrationTransform );
	}

	public void setSettingsFromUI()
	{
		settings.showIntermediateResults = showIntermediateResults;
		settings.registrationResolution = registrationResolution;
		settings.outputResolution = outputResolution;
		settings.thresholdAfterBackgroundSubtraction = thresholdAfterBackgroundSubtraction;
		settings.refractiveIndexScalingCorrectionFactor = refractiveIndexScalingCorrectionFactor;
		settings.refractiveIndexIntensityCorrectionDecayLength = refractiveIndexIntensityCorrectionDecayLength;
		settings.minDistanceToAxisForRollAngleComputation = minDistanceToAxisForRollAngleComputation;
		settings.watershedSeedsDistanceThreshold = watershedSeedsDistanceThreshold;
	}


}
