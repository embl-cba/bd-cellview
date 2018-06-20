package de.embl.cba.drosophila.dapi;

import de.embl.cba.drosophila.Algorithms;
import de.embl.cba.drosophila.Transforms;
import de.embl.cba.drosophila.Utils;
import de.embl.cba.drosophila.geometry.EllipsoidParameterComputer;
import de.embl.cba.drosophila.geometry.Ellipsoid3dParameters;
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
import java.util.Arrays;
import java.util.List;

import static de.embl.cba.drosophila.Constants.*;
import static de.embl.cba.drosophila.Transforms.copyAsArrayImg;
import static de.embl.cba.drosophila.viewing.BdvImageViewer.show;
import static de.embl.cba.drosophila.viewing.BdvImageViewer.show;
import static java.lang.Math.*;

public class DapiRegistration
{
	final DapiRegistrationSettings settings;

	public DapiRegistration( DapiRegistrationSettings settings )
	{
		this.settings = settings;
	}

	public < T extends RealType< T > & NativeType< T > >
	AffineTransform3D computeRegistration( RandomAccessibleInterval< T > input, double[] calibration  )
	{

		AffineTransform3D registration = new AffineTransform3D();

		if ( settings.showIntermediateResults ) show( input, "dapi input data", null, calibration, false );

		Utils.correctCalibrationForRefractiveIndexMismatch( calibration, settings.refractiveIndexCorrectionAxialScalingFactor );

		if ( settings.showIntermediateResults ) show( input, "calibration corrected createTransformedView on raw input data", null, calibration, false );

		AffineTransform3D scalingTransform3D = new AffineTransform3D();
		scalingTransform3D.scale( 0.5 );

		AffineTransform3D scalingToRegistrationResolution = Transforms.getTransformToIsotropicRegistrationResolution( settings.resolutionDuringRegistrationInMicrometer, calibration );

		final RandomAccessibleInterval< T > binnedView = Transforms.createTransformedView( input, scalingToRegistrationResolution );

		final RandomAccessibleInterval< T > binned = copyAsArrayImg( binnedView );

		calibration = getIsotropicCalibration( settings.resolutionDuringRegistrationInMicrometer );

		if ( settings.showIntermediateResults ) show( binned, "binned copyAsArrayImg ( " + settings.resolutionDuringRegistrationInMicrometer + " um )", null, calibration, false );

		Utils.correctIntensityAlongZ( binned, calibration[ Z ] );

		final RandomAccessibleInterval< BitType > binaryImage = Utils.createBinaryImage( binned, settings.threshold );

		if ( settings.showIntermediateResults ) show( binaryImage, "binary", null, calibration, false );

		final Ellipsoid3dParameters ellipsoid3dParameters = EllipsoidParameterComputer.compute( binaryImage );

		registration.preConcatenate( Utils.createEllipsoidAlignmentTransform( binned, ellipsoid3dParameters ) );

		if ( settings.showIntermediateResults ) show( Transforms.createTransformedView( binned, registration ), "ellipsoid aligned", null, calibration, false );

		registration.preConcatenate( Utils.createOrientationTransformation(
				Transforms.createTransformedView( binned, registration ), X,
				settings.derivativeDeltaInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer,
				settings.showIntermediateResults ) );

		if ( settings.showIntermediateResults ) show( Transforms.createTransformedView( binned, registration ), "oriented", null, calibration, false );

		final RandomAccessibleInterval< T > longAxisProjection = Utils.createAverageProjection(
				Transforms.createTransformedView( binned, registration ), X,
				settings.projectionRangeMinDistanceToCenterInMicrometer,
				settings.projectionRangeMaxDistanceToCenterInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer);

		if ( settings.showIntermediateResults ) show( longAxisProjection, "perpendicular projection", null, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		final RandomAccessibleInterval< T > blurred = Utils.createBlurredRai(
				longAxisProjection,
				settings.sigmaForBlurringAverageProjectionInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer );

		final Point maximum = Algorithms.findMaximumLocation( blurred, new double[]{ calibration[ Y ], calibration[ Z ] });
		final List< RealPoint > realPoints = asRealPointList( maximum );
		realPoints.add( new RealPoint( new double[]{ 0, 0 } ) );

		if ( settings.showIntermediateResults ) show( blurred, "perpendicular projection - blurred ", realPoints, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		registration.preConcatenate( createRollTransform( Transforms.createTransformedView( binned, registration ), maximum ) );

		if ( settings.showIntermediateResults ) show( Transforms.createTransformedView( binned, registration ), "registered binned dapi", null, calibration, false );

		// Down-sampling: Saalfeld:
		// fiji plugins examples down-sample (bean-shell).

		final AffineTransform3D scalingToFinalResolution = Transforms.getTransformToIsotropicRegistrationResolution( settings.finalResolutionInMicrometer / settings.resolutionDuringRegistrationInMicrometer, new double[]{ 1, 1, 1 } );

		registration = scalingToRegistrationResolution.preConcatenate( registration ).preConcatenate( scalingToFinalResolution );

		return registration;

	}

	public static double[] getIsotropicCalibration( double value )
	{
		double[] calibration = new double[ 3 ];

		Arrays.fill( calibration, value );

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

		double[] calibration = Utils.getCalibration( dataset );

		final RandomAccessibleInterval< T > dapiRaw = (RandomAccessibleInterval< T > ) dataset.getImgPlus().copy();
		
		DapiRegistrationSettings settings = new DapiRegistrationSettings();

		settings.showIntermediateResults = true;

		DapiRegistration registration = new DapiRegistration( settings );

		final AffineTransform3D registrationTransform = registration.computeRegistration( dapiRaw, calibration );

		show( Transforms.createTransformedView( dapiRaw, registrationTransform ), "registered input ( " + settings.finalResolutionInMicrometer + " um )", asRealPointList( new Point( 0,0,0 ) ), new double[]{ 1, 1, 1 }, false );

	}



}
