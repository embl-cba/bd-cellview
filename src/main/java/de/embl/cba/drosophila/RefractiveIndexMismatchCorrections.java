package de.embl.cba.drosophila;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import static de.embl.cba.drosophila.Constants.Z;
import static java.lang.Math.exp;

public abstract class RefractiveIndexMismatchCorrections
{


	public static double getIntensityCorrectionFactorAlongZ( long z, double zScalingToMicrometer, double intensityDecayLengthInMicrometer )
	{

		/*
		f( 10 ) = 93; f( 83 ) = 30;
		f( z1 ) = v1; f( z2 ) = v2;
		f( z ) = A * exp( -z / d );

		=> d = ( z2 - z1 ) / ln( v1 / v2 );

		> log ( 93 / 30 )
		[1] 1.1314

		=> d = 	73 / 1.1314 = 64.52172;

		=> correction = 1 / exp( -z / d );

		at z = 10 we want value = 93 => z0  = 10;

		=> correction = 1 / exp( - ( z - 10 ) / d );
		 */

		double generalIntensityScaling = 0.3; // TODO: what to use here?

		double offsetInMicrometer = 10.0D; // TODO: might differ between samples?

		double zInMicrometer = z * zScalingToMicrometer - offsetInMicrometer;

		double correctionFactor = generalIntensityScaling / exp( - zInMicrometer / intensityDecayLengthInMicrometer );

		return correctionFactor;
	}

	public static < T extends RealType< T > & NativeType< T > >
	void correctIntensity( RandomAccessibleInterval< T > rai, double zCalibration, double backgroundIntensity, double intensityDecayLength )
	{
		for ( long z = rai.min( Z ); z <= rai.max( Z ); ++z )
		{
			RandomAccessibleInterval< T > slice = Views.hyperSlice( rai, Z, z );

			double intensityCorrectionFactor = getIntensityCorrectionFactorAlongZ( z, zCalibration, intensityDecayLength );

			Views.iterable( slice ).forEach( t ->
					{
						if ( ( t.getRealDouble() - backgroundIntensity ) < 0 )
						{
							t.setReal( 0 );
						}
						else
						{
							t.setReal( t.getRealDouble() - backgroundIntensity );
							t.mul( intensityCorrectionFactor );
						}

					}
			);

		}

	}

	public static void correctCalibration( double[] calibration, double correctionFactor )
	{
		calibration[ Z ] *= correctionFactor;
	}
}
