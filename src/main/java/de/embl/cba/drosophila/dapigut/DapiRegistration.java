package de.embl.cba.drosophila.dapigut;

import de.embl.cba.drosophila.geometry.EllipsoidParameterComputer;
import de.embl.cba.drosophila.geometry.EllipsoidParameters;
import de.embl.cba.registration.algorithm.Algorithms;
import de.embl.cba.registration.utils.Transforms;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.registration.bdv.BdvImageViewer.show;
import static de.embl.cba.registration.utils.Constants.*;
import static de.embl.cba.registration.utils.Transforms.createArrayCopy;
import static java.lang.Math.*;

public class DapiRegistration
{
	final DapiRegistrationSettings settings;

	public DapiRegistration( DapiRegistrationSettings settings )
	{
		this.settings = settings;
	}

	public < T extends RealType< T > & NativeType< T > >
	AffineTransform3D computeRegistration( RandomAccessibleInterval< T > dapiRaw, double[] calibration  )
	{

		AffineTransform3D registration = new AffineTransform3D();

		if ( settings.showIntermediateResults ) show( dapiRaw, "raw input data", null, calibration, false );

		DrosophilaRegistrationUtils.correctCalibrationForRefractiveIndexMismatch( calibration, settings.refractiveIndexCorrectionAxialScalingFactor );

		if ( settings.showIntermediateResults ) show( dapiRaw, "calibration corrected view on raw input data", null, calibration, false );

		AffineTransform3D scalingTransform3D = new AffineTransform3D();
		scalingTransform3D.scale( 0.5 );

		AffineTransform3D scalingToRegistrationResolution = getScalingTransform( settings.resolutionDuringRegistrationInMicrometer, calibration );

		final RandomAccessibleInterval< T > binnedView = Transforms.view( dapiRaw, scalingToRegistrationResolution );

		final RandomAccessibleInterval< T > binned = createArrayCopy( binnedView );

		calibration = getIsotropicCalibration( settings.resolutionDuringRegistrationInMicrometer );

		if ( settings.showIntermediateResults ) show( binned, "binned copy ( " + settings.resolutionDuringRegistrationInMicrometer + " um )", null, calibration, false );

		DrosophilaRegistrationUtils.correctIntensityAlongZ( binned, calibration[ Z ] );

		final RandomAccessibleInterval< BitType > binaryImage = DrosophilaRegistrationUtils.createBinaryImage( binned, settings.threshold );

		if ( settings.showIntermediateResults ) show( binaryImage, "binary", null, calibration, false );

		final EllipsoidParameters ellipsoidParameters = EllipsoidParameterComputer.compute( binaryImage );

		registration.preConcatenate( DrosophilaRegistrationUtils.createEllipsoidAlignmentTransform( binned, ellipsoidParameters ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "ellipsoid aligned", null, calibration, false );

		registration.preConcatenate( DrosophilaRegistrationUtils.createOrientationTransformation(
				Transforms.view( binned, registration ), X,
				settings.derivativeDeltaInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer,
				settings.showIntermediateResults ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "oriented", null, calibration, false );

		final RandomAccessibleInterval< T > longAxisProjection = DrosophilaRegistrationUtils.createAverageProjection(
				Transforms.view( binned, registration ), X,
				settings.projectionRangeMinDistanceToCenterInMicrometer,
				settings.projectionRangeMaxDistanceToCenterInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer);

		if ( settings.showIntermediateResults ) show( longAxisProjection, "perpendicular projection", null, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		final RandomAccessibleInterval< T > blurred = DrosophilaRegistrationUtils.createBlurredRai(
				longAxisProjection,
				settings.sigmaForBlurringAverageProjectionInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer );

		final Point maximum = Algorithms.findMaximum( blurred, new double[]{ calibration[ Y ], calibration[ Z ] });
		final List< RealPoint > realPoints = asRealPointList( maximum );
		realPoints.add( new RealPoint( new double[]{ 0, 0 } ) );

		if ( settings.showIntermediateResults ) show( blurred, "perpendicular projection - blurred ", realPoints, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		registration.preConcatenate( createRollTransform( Transforms.view( binned, registration ), maximum ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "registered binned dapi", null, calibration, false );

		// Down-sampling: Saalfeld:
		// fiji plugins examples down-sample (bean-shell).

		final AffineTransform3D scalingToFinalResolution = getScalingTransform( settings.finalResolutionInMicrometer / settings.resolutionDuringRegistrationInMicrometer, new double[]{ 1, 1, 1 } );

		registration = scalingToRegistrationResolution.preConcatenate( registration ).preConcatenate( scalingToFinalResolution );

		return registration;

	}

	public static AffineTransform3D getScalingTransform( double binning, double[] calibration )
	{
		double[] downScaling = new double[ 3 ];

		for ( int d : XYZ )
		{
			downScaling[ d ] = calibration[ d ] / binning;
		}

		final AffineTransform3D scalingTransform = createScalingTransform( downScaling );

		return scalingTransform;
	}

	public static AffineTransform3D createScalingTransform( double[] calibration )
	{
		AffineTransform3D scaling = new AffineTransform3D();

		for ( int d : XYZ )
		{
			scaling.set( calibration[ d ], d, d );
		}

		return scaling;
	}

	public static double[] getIsotropicCalibration( double value )
	{
		double[] calibration = new double[ 3 ];
		for ( int d : XYZ )
		{
			calibration[ d ] = value;
		}
		return calibration;
	}

	public static < T extends RealType< T > & NativeType< T > >
	AffineTransform3D createRollTransform( RandomAccessibleInterval< T > rai, Point maximum )
	{
		double angleToZAxisInDegrees = getAngleToZAxis( maximum );
		AffineTransform3D rollTransform = new AffineTransform3D();
		rollTransform.rotate( X, toRadians( angleToZAxisInDegrees ) );

		return rollTransform;
	}

	public static double getAngleToZAxis( Point maximum )
	{
		final double angleToYAxis;

		if ( maximum.getIntPosition( Y ) == 0 )
		{
			angleToYAxis = 90;
		}
		else
		{
			angleToYAxis = toDegrees( atan( maximum.getDoublePosition( X ) / maximum.getDoublePosition( Y ) ) );
		}

		return angleToYAxis;
	}

	public static List< RealPoint > asRealPointList( Point maximum )
	{
		List< RealPoint > realPoints = new ArrayList<>();
		final double[] doubles = new double[ maximum.numDimensions() ];
		maximum.localize( doubles );
		realPoints.add( new RealPoint( doubles) );

		return realPoints;
	}


	public static < T extends RealType< T > & NativeType< T > >
	List< RealPoint > computeMaximumLocation( RandomAccessibleInterval< T > blurred, int sigmaForBlurringAverageProjection )
	{
		Shape shape = new HyperSphereShape( sigmaForBlurringAverageProjection );

		List< RealPoint > points = Algorithms.findMaxima( blurred, shape );

		return points;
	}


	public static < T extends RealType< T > & NativeType< T > >
	void main( String... args ) throws IOException
	{
		//		String path = "/Users/tischer/Documents/fiji-plugin-imageRegistration/src/test/resources/crocker-7-2-scale0.25-rot_z_60.zip";
		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 7-2 rotated.tif";
//		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 6-3 Dapi iso1um.tif";
//		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 4-3 Dapi iso1um.tif";

		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();

		DatasetIOService datasetIOService = imagej.scifio().datasetIO();

		Dataset dataset = datasetIOService.open( path );

		double[] calibration = DrosophilaRegistrationUtils.getCalibration( dataset );

		final RandomAccessibleInterval< T > dapiRaw = (RandomAccessibleInterval< T > ) dataset.getImgPlus().copy();


		DapiRegistrationSettings settings = new DapiRegistrationSettings();

		settings.showIntermediateResults = true;

		DapiRegistration registration = new DapiRegistration( settings );

		final AffineTransform3D registrationTransform = registration.computeRegistration( dapiRaw, calibration );

		show( Transforms.view( dapiRaw, registrationTransform ), "registered input ( " + settings.finalResolutionInMicrometer + " um )", asRealPointList( new Point( 0,0,0 ) ), new double[]{ 1, 1, 1 }, false );

	}



}
