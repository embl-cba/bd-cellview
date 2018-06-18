package de.embl.cba.drosophila.shavenbaby;

import de.embl.cba.drosophila.Algorithms;
import de.embl.cba.drosophila.Utils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.algorithm.morphology.distance.DistanceTransform;

import java.util.Arrays;

import static de.embl.cba.drosophila.Transforms.getDownScalingFactors;
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
		 */

		Utils.correctCalibrationForRefractiveIndexMismatch( calibration, settings.refractiveIndexCorrectionAxialScalingFactor );

		final RandomAccessibleInterval< T > downscaled = Algorithms.createDownscaledArrayImg( input, getDownScalingFactors( calibration, settings.resolutionDuringRegistration ) );

		final double[] calibrationDuringRegistration = getCalibrationDuringRegistration();

		if ( settings.showIntermediateResults ) show( downscaled, "downscaled", null, calibrationDuringRegistration, false );


		/**
		 * Threshold
		 *
		 * TODO: Distance transform seems to only work for pixel values 255 and 0 (s.b.)
		 */

		final RandomAccessibleInterval< UnsignedByteType > binary = Converters.convert(
				downscaled, ( i, o ) -> o.set( i.getRealDouble() > settings.threshold ? 255 : 0 ), new UnsignedByteType() );

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

	private double[] getCalibrationDuringRegistration()
	{
		double[] calibrationDuringRegistration = new double[3];
		Arrays.fill( calibrationDuringRegistration, settings.resolutionDuringRegistration );
		return calibrationDuringRegistration;
	}
}
