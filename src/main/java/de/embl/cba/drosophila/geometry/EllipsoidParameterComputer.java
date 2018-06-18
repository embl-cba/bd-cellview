package de.embl.cba.drosophila.geometry;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.Views;

import static de.embl.cba.drosophila.Constants.*;
import static java.lang.Math.*;

public class EllipsoidParameterComputer
{

	public static EllipsoidParameters compute( RandomAccessibleInterval< BitType > binaryImg )
	{

		double[] sums = new double[ 3 ];
		double[] sumSquares = new double[ 6 ];

		final long numPixels = computeSumsAndSumSquares( binaryImg, sums, sumSquares );

		final double[] center = computeCenter( sums, numPixels );

		final double[] moments = computeMoments( sumSquares, center, numPixels );

		final Matrix momentsMatrix = getMomentsMatrix( moments );

		EllipsoidParameters ellipsoidParameters = new EllipsoidParameters();

		ellipsoidParameters.center = center;

		SingularValueDecomposition svd = new SingularValueDecomposition( momentsMatrix );

		ellipsoidParameters.radii = computeRadii( svd.getS() );

		ellipsoidParameters.anglesInDegrees = computeAngles( svd.getU() );

		return ellipsoidParameters;

	}

	public static double[] computeMoments( double[] sumSquares, double[] center, long numPixels )
	{
		double[] moments = new double[ 6 ];

		for ( int d : XYZ )
		{
			moments[ d ] = sumSquares[ d ] / numPixels - center[ d ] * center[ d ];
		}

		moments[ XY ] = sumSquares[ XY ] / numPixels - center[ X ] * center[ Y ];
		moments[ XZ ] = sumSquares[ XZ ] / numPixels - center[ X ] * center[ Z ];
		moments[ YZ ] = sumSquares[ YZ ] / numPixels - center[ Y ] * center[ Z ];

		return moments;
	}

	public static long computeSumsAndSumSquares( RandomAccessibleInterval< BitType > binaryImg, double[] sums, double[] sumSquares )
	{

		Cursor< BitType > cursor = Views.iterable( binaryImg ).localizingCursor();

		long[] xyzPosition = new long[ 3 ];

		BitType one = new BitType( true );

		long numPixels = 0;

		while ( cursor.hasNext() )
		{
			if ( cursor.next().valueEquals( one ) )
			{
				numPixels++;

				cursor.localize( xyzPosition );

				for ( int d : XYZ )
				{
					sums[ d ] += xyzPosition[ d ];
				}

				for ( int d : XYZ )
				{
					sumSquares[ d ] += ( xyzPosition[ d ] * xyzPosition[ d ] );
				}

				sumSquares[ XY ] += xyzPosition[ X ] * xyzPosition[ Y ];
				sumSquares[ YZ ] += xyzPosition[ Y ] * xyzPosition[ Z ];
				sumSquares[ XZ ] += xyzPosition[ X ] * xyzPosition[ Z ];

			}
		}

		return numPixels;
	}


	public static double[] computeCenter( double[] sum, long n )
	{
		double[] center = new double[ 3 ];

		for ( int i = 0; i < 3; ++i )
		{
			center[ i ] = sum[ i ] / n;
		}

		return center;
	}


	public static Matrix getMomentsMatrix( double[] moments )
	{
		// computeRegistration computeRegistration parameters for each region
		Matrix matrix = new Matrix( 3, 3 );

		for ( int d : XYZ )
		{
			matrix.set( d, d, moments[ d ] );
		}

		matrix.set( 0, 1, moments[ XY ] );
		matrix.set( 0, 2, moments[ XZ ] );

		matrix.set( 1, 0, moments[ XY ] );
		matrix.set( 1, 2, moments[ YZ ] );

		matrix.set( 2, 0, moments[ XZ ] );
		matrix.set( 2, 1, moments[ YZ ] );

		return matrix;
	}


	private static double[] computeRadii( Matrix sdvS )
	{

		double[] radii = new double[ 3 ];

		for ( int d : XYZ )
		{
			radii[ d ] = sqrt( 5 ) * sqrt( sdvS.get( d, d ) );
		}

		return radii;
	}

	private static double[] computeAngles( Matrix sdvU )
	{

		double[] angles = new double[ 3 ];

		// extract |cos(theta)|
		double tmp = hypot(sdvU.get(1, 1), sdvU.get(2, 1));
		double phi, theta, psi;

		// avoid dividing by 0
		if (tmp > 16 * Double.MIN_VALUE)
		{
			// normal case: theta <> 0
			psi     = atan2( sdvU.get(2, 1), sdvU.get(2, 2));
			theta   = atan2(-sdvU.get(2, 0), tmp);
			phi     = atan2( sdvU.get(1, 0), sdvU.get(0, 0));
		}
		else
		{
			// theta is around 0
			psi     = atan2(-sdvU.get(1, 2), sdvU.get(1,1));
			theta   = atan2(-sdvU.get(2, 0), tmp);
			phi     = 0;
		}

		angles[ EllipsoidParameters.PHI ] = toDegrees( phi );
		angles[ EllipsoidParameters.THETA ] = toDegrees( theta );
		angles[ EllipsoidParameters.PSI ] = toDegrees( psi );

		return angles;
	}






}
