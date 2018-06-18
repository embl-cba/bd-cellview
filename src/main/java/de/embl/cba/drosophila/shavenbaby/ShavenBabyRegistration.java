package de.embl.cba.drosophila.shavenbaby;

import de.embl.cba.drosophila.Transforms;
import de.embl.cba.drosophila.Utils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.algorithm.morphology.distance.DistanceTransform;

import static de.embl.cba.drosophila.Transforms.copyAsArrayImg;
import static de.embl.cba.drosophila.viewing.BdvImageViewer.show;


public class ShavenBabyRegistration
{

	final ShavenBabyRegistrationSettings settings;

	public ShavenBabyRegistration( ShavenBabyRegistrationSettings settings )
	{
		this.settings = settings;
	}

	public < T extends RealType< T > & NativeType< T > >
	AffineTransform3D computeRegistration( RandomAccessibleInterval< T > input, double[] calibration  )
	{

		if ( settings.showIntermediateResults ) show( input, "input data", null, calibration, false );

		AffineTransform3D registration = new AffineTransform3D();

		/**
		 *  Axial calibration correction and scaling to isotropic (down-sampled) resolution
		 *
		 *  TODO: replace below code by an averaging down-sampling, using Gaussian blurring
		 *
		 */

		Utils.correctCalibrationForRefractiveIndexMismatch( calibration, settings.refractiveIndexCorrectionAxialScalingFactor );

		AffineTransform3D scalingToIsotropicRegistrationResolution = Transforms.getTransformToIsotropicRegistrationResolution( settings.resolutionDuringRegistrationInMicrometer, calibration );

		final RandomAccessibleInterval< T > scaledView = Transforms.createTransformedView( input, scalingToIsotropicRegistrationResolution );

		final RandomAccessibleInterval< T > scaled = copyAsArrayImg( scaledView );


		/**
		 * Threshold
		 */

		final RandomAccessibleInterval< UnsignedByteType > binary = Converters.convert(
				scaled, ( i, o ) -> o.set( i.getRealDouble() > settings.threshold ? 255 : 0 ), new UnsignedByteType() );

		if ( settings.showIntermediateResults ) show( binary, "binary", null, calibration, false );



		/**
		 *  Distance transform
		 *
		 *  TODO: what kind of input image does the distance transform expect? for BoolType and UnsignedByteType with 1 and 0 I don't get anything sensible...
		 */

		final RandomAccessibleInterval< DoubleType > distance = ArrayImgs.doubles( Intervals.dimensionsAsLongArray( binary ) );

		DistanceTransform.transform( binary, distance, DistanceTransform.DISTANCE_TYPE.EUCLIDIAN );

		if ( settings.showIntermediateResults ) show( distance, "distance", null, calibration, false );

		return registration;

	}
}
