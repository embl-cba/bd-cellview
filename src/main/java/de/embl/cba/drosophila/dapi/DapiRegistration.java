package de.embl.cba.drosophila.dapi;

import de.embl.cba.drosophila.Algorithms;
import de.embl.cba.drosophila.RefractiveIndexCorrections;
import de.embl.cba.drosophila.Transforms;
import de.embl.cba.drosophila.Utils;
import de.embl.cba.drosophila.geometry.Ellipsoids;
import de.embl.cba.drosophila.geometry.EllipsoidParameters;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import java.util.Arrays;
import java.util.List;

import static de.embl.cba.drosophila.Constants.*;
import static de.embl.cba.drosophila.Utils.copyAsArrayImg;
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

		RefractiveIndexCorrections.correctCalibration( calibration, settings.refractiveIndexCorrectionAxialScalingFactor );

		if ( settings.showIntermediateResults ) show( input, "calibration corrected createTransformedView on raw input data", null, calibration, false );

		AffineTransform3D scalingTransform3D = new AffineTransform3D();
		scalingTransform3D.scale( 0.5 );

		AffineTransform3D scalingToRegistrationResolution = Transforms.getTransformToIsotropicRegistrationResolution( settings.resolutionDuringRegistrationInMicrometer, calibration );

		final RandomAccessibleInterval< T > binnedView = Transforms.createTransformedView( input, scalingToRegistrationResolution );

		final RandomAccessibleInterval< T > binned = copyAsArrayImg( binnedView );

		calibration = getIsotropicCalibration( settings.resolutionDuringRegistrationInMicrometer );

		if ( settings.showIntermediateResults ) show( binned, "binned copyAsArrayImg ( " + settings.resolutionDuringRegistrationInMicrometer + " um )", null, calibration, false );

		RefractiveIndexCorrections.correctIntensity( binned, calibration[ Z ], 0.0D );

		final RandomAccessibleInterval< BitType > binaryImage = Utils.createBinaryImage( binned, settings.threshold );

		if ( settings.showIntermediateResults ) show( binaryImage, "binary", null, calibration, false );

		final EllipsoidParameters ellipsoidParameters = Ellipsoids.computeParametersFromBinaryImage( binaryImage );

		registration.preConcatenate( Ellipsoids.createAlignmentTransform( ellipsoidParameters ) );

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
		final List< RealPoint > realPoints = Utils.asRealPointList( maximum );
		realPoints.add( new RealPoint( new double[]{ 0, 0 } ) );

		if ( settings.showIntermediateResults ) show( blurred, "perpendicular projection - blurred ", realPoints, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		registration.preConcatenate( createRollTransform( Transforms.createTransformedView( binned, registration ), maximum ) );

		if ( settings.showIntermediateResults ) show( Transforms.createTransformedView( binned, registration ), "registered binned dapi", null, calibration, false );

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
		double angleToZAxisInDegrees = angleToZAxis( maximum );
		AffineTransform3D rollTransform = new AffineTransform3D();
		rollTransform.rotate( X, toRadians( angleToZAxisInDegrees ) );

		return rollTransform;
	}

	public static double angleToZAxis( Point maximum )
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


}
