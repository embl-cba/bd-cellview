package de.embl.cba.drosophila.shavenbaby;

import de.embl.cba.registration.algorithm.Algorithms;
import de.embl.cba.registration.geometry.EllipsoidParameterComputer;
import de.embl.cba.registration.geometry.EllipsoidParameters;
import de.embl.cba.registration.utils.Transforms;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import java.util.List;

import static de.embl.cba.registration.bdv.BdvImageViewer.show;
import static de.embl.cba.registration.utils.Constants.*;
import static de.embl.cba.registration.utils.Transforms.createArrayCopy;

public class ShavenBabyRegistration
{


	public < T extends RealType< T > & NativeType< T > >
	AffineTransform3D computeRegistration( RandomAccessibleInterval< T > dapiRaw, double[] calibration  )
	{

		AffineTransform3D registration = new AffineTransform3D();

		if ( settings.showIntermediateResults ) show( dapiRaw, "raw input data", null, calibration, false );

		correctCalibrationForRefractiveIndexMismatch( calibration, settings.refractiveIndexCorrectionAxialScalingFactor );

		if ( settings.showIntermediateResults ) show( dapiRaw, "calibration corrected view on raw input data", null, calibration, false );


		AffineTransform3D scalingTransform3D = new AffineTransform3D();
		scalingTransform3D.scale( 0.5 );

		AffineTransform3D scalingToRegistrationResolution = getScalingTransform( settings.resolutionDuringRegistrationInMicrometer, calibration );

		final RandomAccessibleInterval< T > binnedView = Transforms.view( dapiRaw, scalingToRegistrationResolution );

		final RandomAccessibleInterval< T > binned = createArrayCopy( binnedView );

		calibration = getIsotropicCalibration( settings.resolutionDuringRegistrationInMicrometer );

		if ( settings.showIntermediateResults ) show( binned, "binned copy ( " + settings.resolutionDuringRegistrationInMicrometer + " um )", null, calibration, false );

		correctIntensityAlongZ( binned, calibration[ Z ] );

		final RandomAccessibleInterval< BitType > binaryImage = createBinaryImage( binned, settings.threshold );

		if ( settings.showIntermediateResults ) show( binaryImage, "binary", null, calibration, false );

		final EllipsoidParameters ellipsoidParameters = EllipsoidParameterComputer.compute( binaryImage );

		registration.preConcatenate( createEllipsoidAlignmentTransform( binned, ellipsoidParameters ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "ellipsoid aligned", null, calibration, false );

		registration.preConcatenate( createOrientationTransformation(
				Transforms.view( binned, registration ), X,
				settings.derivativeDeltaInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer,
				settings.showIntermediateResults ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "oriented", null, calibration, false );

		final RandomAccessibleInterval< T > longAxisProjection = createAverageProjection(
				Transforms.view( binned, registration ), X,
				settings.projectionRangeMinDistanceToCenterInMicrometer,
				settings.projectionRangeMaxDistanceToCenterInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer);

		if ( settings.showIntermediateResults ) show( longAxisProjection, "perpendicular projection", null, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		final RandomAccessibleInterval< T > blurred = createBlurredRai(
				longAxisProjection,
				settings.sigmaForBlurringAverageProjectionInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer );

		final Point maximum = Algorithms.findMaximum( blurred, new double[]{ calibration[ Y ], calibration[ Z ] });
		final List< RealPoint > realPoints = asRealPointList( maximum );
		realPoints.add( new RealPoint( new double[]{ 0, 0 } ) );

		if ( settings.showIntermediateResults ) show( blurred, "perpendicular projection - blurred ", realPoints, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		registration.preConcatenate( createRollTransform( Transforms.view( binned, registration ), maximum ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "registered binned dapi", null, calibration, false );

		final AffineTransform3D scalingToFinalResolution = getScalingTransform( settings.finalResolutionInMicrometer / settings.resolutionDuringRegistrationInMicrometer, new double[]{ 1, 1, 1 } );

		registration = scalingToRegistrationResolution.preConcatenate( registration ).preConcatenate( scalingToFinalResolution );

		return registration;

	}
}
