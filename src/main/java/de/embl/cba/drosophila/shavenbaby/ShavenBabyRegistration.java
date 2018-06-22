package de.embl.cba.drosophila.shavenbaby;

import de.embl.cba.drosophila.*;
import de.embl.cba.drosophila.geometry.CentroidsParameters;
import de.embl.cba.drosophila.geometry.EllipsoidParameters;
import de.embl.cba.drosophila.geometry.Ellipsoids;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;

import java.util.ArrayList;
import java.util.Arrays;

import static de.embl.cba.drosophila.Constants.X;
import static de.embl.cba.drosophila.Constants.XYZ;
import static de.embl.cba.drosophila.Constants.Z;
import static de.embl.cba.drosophila.Transforms.getScalingFactors;
import static de.embl.cba.drosophila.viewing.BdvImageViewer.show;
import static java.lang.Math.toRadians;


public class ShavenBabyRegistration
{

	final ShavenBabyRegistrationSettings settings;
	final ImageJ ij;

	public ShavenBabyRegistration( ShavenBabyRegistrationSettings settings, ImageJ ij )
	{
		this.settings = settings;
		this.ij = ij;
	}

	public < T extends RealType< T > & NativeType< T > >
	AffineTransform3D computeRegistration( RandomAccessibleInterval< T > input, double[] inputCalibration  )
	{

		AffineTransform3D registration = new AffineTransform3D();

		/**
		 *  Refractive index corrections
		 */

		RefractiveIndexCorrections.correctCalibration( inputCalibration, settings.refractiveIndexSaclingCorrectionFactor );

		final RandomAccessibleInterval< T > intensityCorrected = Utils.copyAsArrayImg( input );

		RefractiveIndexCorrections.correctIntensity( intensityCorrected, inputCalibration[ Z ], settings.intensityOffset, settings.refractiveIndexIntensityCorrectionDecayLength );

		if ( settings.showIntermediateResults ) show( intensityCorrected, "input data", null, inputCalibration, false );

		/**
		 *  Down-sampling to registration resolution
		 */

		final RandomAccessibleInterval< T > downscaled = Algorithms.createDownscaledArrayImg( intensityCorrected, getScalingFactors( inputCalibration, settings.registrationResolution ) );

		double[] registrationCalibration = getRegistrationCalibration();

		if ( settings.showIntermediateResults ) show( downscaled, "downscaled", null, registrationCalibration, false );


		/**
		 * Threshold
		 */

		final RandomAccessibleInterval< BoolType > mask = Converters.convert(
				downscaled, ( i, o ) -> o.set( i.getRealDouble() > settings.thresholdAfterOffsetSubtraction ? true : false ), new BoolType() );

		if ( settings.showIntermediateResults ) show( mask, "mask", null, registrationCalibration, false );


		/**
		 * Watershed
		 */

		// prepare result label image
		final Img< IntType > labelImg = ArrayImgs.ints( Intervals.dimensionsAsLongArray( mask ) );
		final ImgLabeling< Integer, IntType > labeling = new ImgLabeling<>( labelImg );

		ij.op().image().watershed(
				labeling,
				mask,
				false,
				false,
				settings.minimalDistanceOfLocalMinimaForWatershed / settings.registrationResolution,
				mask );


		if ( settings.showIntermediateResults ) show( labelImg, "watershed", null, registrationCalibration, false );

		/**
		 * Get Central Embryo
		 */

		final LabelRegion< Integer > centralObjectRegion = getCentralObjectLabelRegion( labeling );

		final Img< BitType > centralObjectMask = createBitTypeMaskFromLabelRegion( centralObjectRegion, Intervals.dimensionsAsLongArray( downscaled ) );

		if ( settings.showIntermediateResults ) show( centralObjectMask, "central object", null, registrationCalibration, false );

		/**
		 * Compute ellipsoid (probably mainly yaw) alignment
		 */

		final EllipsoidParameters ellipsoidParameters = Ellipsoids.computeParametersFromBinaryImage( centralObjectMask );

		registration.preConcatenate( Ellipsoids.createAlignmentTransform( ellipsoidParameters ) );

		final RandomAccessibleInterval yawAlignedMask = Utils.copyAsArrayImg( Transforms.createTransformedView( centralObjectMask, registration, new NearestNeighborInterpolatorFactory() ) );

		/**
		 * Compute roll alignment
		 */

		final CentroidsParameters centroidsParameters = Utils.computeCentroidsParametersAlongX( yawAlignedMask, settings.registrationResolution );

		if ( settings.showIntermediateResults )  Plots.plot( centroidsParameters.axisCoordinates, centroidsParameters.angles, "x", "angle" );
		if ( settings.showIntermediateResults )  Plots.plot( centroidsParameters.axisCoordinates, centroidsParameters.distances, "x", "distance" );

		if ( settings.showIntermediateResults ) show( yawAlignedMask, "yaw aligned mask", centroidsParameters.centroids, registrationCalibration, false );

		final AffineTransform3D rollTransform = computeRollTransform( centroidsParameters, settings );

		ArrayList< RealPoint > transformedCentroids = createTransformedCentroidPointList( centroidsParameters, rollTransform );

		registration.preConcatenate( rollTransform );

		if ( settings.showIntermediateResults ) show( Transforms.createTransformedView( centralObjectMask, registration, new NearestNeighborInterpolatorFactory() ), "yaw and roll aligned mask", transformedCentroids, registrationCalibration, false );

		/**
		 * Compute final registration transform
		 */

		registration = createFinalTransform( inputCalibration, registration, registrationCalibration );

		if ( settings.showIntermediateResults ) show( Transforms.createTransformedView( intensityCorrected, registration ), "aligned input data ( " + settings.outputResolution + " um )", origin(), registrationCalibration, false );



		return registration;

	}

	public ArrayList< RealPoint > createTransformedCentroidPointList( CentroidsParameters centroidsParameters, AffineTransform3D rollTransform )
	{
		final ArrayList< RealPoint > transformedRealPoints = new ArrayList<>();

		for ( RealPoint realPoint : centroidsParameters.centroids )
		{
			final RealPoint transformedRealPoint = new RealPoint( 0, 0, 0 );
			rollTransform.apply( realPoint, transformedRealPoint );
			transformedRealPoints.add( transformedRealPoint );
		}
		return transformedRealPoints;
	}

	public static AffineTransform3D computeRollTransform( CentroidsParameters centroidsParameters, ShavenBabyRegistrationSettings settings )
	{
		final double rollAngle = computeRollAngle( centroidsParameters, settings.minDistanceToAxis );

		AffineTransform3D rollTransform = new AffineTransform3D();

		rollTransform.rotate( X, - toRadians( rollAngle ) );

		return rollTransform;
	}

	public static double computeRollAngle( CentroidsParameters centroidsParameters, double minDistanceToAxis )
	{
		final int n = centroidsParameters.axisCoordinates.size();

		double averageAngle = 0;
		int numAngles = 0;

		for ( int i = 0; i < n; ++i )
		{
			if ( centroidsParameters.distances.get( i ) > minDistanceToAxis )
			{
				averageAngle += centroidsParameters.angles.get( i );
				++numAngles;
			}
		}

		averageAngle /= numAngles;

		averageAngle = - averageAngle;

		return averageAngle;
	}

	public static ArrayList< RealPoint > origin()
	{
		final ArrayList< RealPoint > origin = new ArrayList<>();
		origin.add( new RealPoint( new double[]{ 0, 0, 0 } ) );
		return origin;
	}

	private AffineTransform3D createFinalTransform( double[] inputCalibration, AffineTransform3D registration, double[] registrationCalibration )
	{
		final AffineTransform3D transform =
				Transforms.getScalingTransform( inputCalibration, settings.registrationResolution )
				.preConcatenate( registration )
				.preConcatenate( Transforms.getScalingTransform( registrationCalibration, settings.outputResolution ) );

		return transform;
	}

	private Img< BitType > createBitTypeMaskFromLabelRegion( LabelRegion< Integer > centralObjectRegion, long[] dimensions )
	{
		final Img< BitType > centralObjectImg = ArrayImgs.bits( dimensions );

		final Cursor< Void > regionCursor = centralObjectRegion.cursor();
		final net.imglib2.RandomAccess< BitType > access = centralObjectImg.randomAccess();
		while ( regionCursor.hasNext() )
		{
			regionCursor.fwd();
			access.setPosition( regionCursor );
			access.get().set( true );
		}
		return centralObjectImg;
	}

	private Img< UnsignedByteType > createUnsignedByteTypeMaskFromLabelRegion( LabelRegion< Integer > centralObjectRegion, long[] dimensions )
	{
		final Img< UnsignedByteType > centralObjectImg = ArrayImgs.unsignedBytes( dimensions );

		final Cursor< Void > regionCursor = centralObjectRegion.cursor();
		final net.imglib2.RandomAccess< UnsignedByteType > access = centralObjectImg.randomAccess();
		while ( regionCursor.hasNext() )
		{
			regionCursor.fwd();
			access.setPosition( regionCursor );
			access.get().set( 255 );
		}
		return centralObjectImg;
	}


	private LabelRegion< Integer > getCentralObjectLabelRegion( ImgLabeling< Integer, IntType > labeling )
	{
		int centralLabel = getCentralLabel( labeling );

		final LabelRegions< Integer > labelRegions = new LabelRegions<>( labeling );
		return labelRegions.getLabelRegion( centralLabel );
	}

	private static int getCentralLabel( ImgLabeling< Integer, IntType > labeling )
	{
		final net.imglib2.RandomAccess< LabelingType< Integer > > labelingRandomAccess = labeling.randomAccess();
		for ( int d : XYZ ) labelingRandomAccess.setPosition( labeling.dimension( d ) / 2, d );
		int centralIndex = labelingRandomAccess.get().getIndex().getInteger();
		return labeling.getMapping().labelsAtIndex( centralIndex ).iterator().next();
	}

	private double[] getRegistrationCalibration()
	{
		double[] registrationCalibration = new double[ 3 ];
		Arrays.fill( registrationCalibration, settings.registrationResolution );
		return registrationCalibration;
	}

	//
	// Useful code snippets
	//

	/**
	 *  Distance transform

	 Hi Christian

	 yes, it seems that you were doing the right thing (as confirmed by your
	 visual inspection of the result). One thing to note: You should
	 probably use a DoubleType image with 1e20 and 0 values, to make sure
	 that f(q) is larger than any possible distance in your image. If you
	 choose 255, your distance is effectively bounded at 255. This can be an
	 issue for big images with sparse foreground objects. With squared
	 Euclidian distance, 255 is already reached if a background pixels is
	 further than 15 pixels from a foreground pixel! If you use
	 Converters.convert to generate your image, the memory consumption
	 remains the same.


	 Phil

	 final RandomAccessibleInterval< UnsignedByteType > binary = Converters.convert(
	 downscaled, ( i, o ) -> o.set( i.getRealDouble() > settings.thresholdAfterOffsetSubtraction ? 255 : 0 ), new UnsignedByteType() );

	 if ( settings.showIntermediateResults ) show( binary, "binary", null, calibration, false );


	 final RandomAccessibleInterval< DoubleType > distance = ArrayImgs.doubles( Intervals.dimensionsAsLongArray( binary ) );

	 DistanceTransform.transform( binary, distance, DistanceTransform.DISTANCE_TYPE.EUCLIDIAN );


	 final double maxDistance = Algorithms.findMaximumValue( distance );

	 final RandomAccessibleInterval< IntType > invertedDistance = Converters.convert( distance, ( i, o ) -> {
	 o.set( ( int ) ( maxDistance - i.get() ) );
	 }, new IntType() );

	 if ( settings.showIntermediateResults ) show( invertedDistance, "distance", null, calibration, false );

	 */


	/**
	 * Convert ImgLabelling to Rai

	 final RandomAccessibleInterval< IntType > labelMask =
	 Converters.convert( ( RandomAccessibleInterval< LabelingType< Integer > > ) watershedImgLabeling,
	 ( i, o ) -> {
	 o.set( i.getIndex().getInteger() );
	 }, new IntType() );

	 */




}
