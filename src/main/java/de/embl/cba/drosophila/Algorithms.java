package de.embl.cba.drosophila;

import net.imglib2.*;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.drosophila.Transforms.createTransformedInterval;

public class Algorithms
{

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createDownscaledArrayImg( RandomAccessibleInterval< T > input, double[] scalingFactors )
	{
		assert scalingFactors.length == input.numDimensions();

		/*
		 * Blur image
		 */

		RandomAccessibleInterval< T > blurred = createOptimallyBlurredArrayImg( input, scalingFactors );

		/*
		 * Sample values from blurred image
		 */

		final RandomAccessibleInterval< T > downscaled = createSubsampledArrayImg( blurred, scalingFactors );

		return downscaled;
	}

	private static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createSubsampledArrayImg( RandomAccessibleInterval< T > input, double[] scalingFactors )
	{
		// TODO: is there a simple way to do this?

		Scale scale = new Scale( scalingFactors );
		RealRandomAccessible< T > rra = Views.interpolate( Views.extendBorder( input ), new NearestNeighborInterpolatorFactory<>() );
		rra = RealViews.transform( rra, scale );
		final RandomAccessible< T > raster = Views.raster( rra );
		final RandomAccessibleInterval< T > output = Views.interval( raster, createTransformedInterval( input, scale ) );

		return Transforms.copyAsArrayImg( output  );
	}

	private static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T > createOptimallyBlurredArrayImg( RandomAccessibleInterval< T > input, double[] scalingFactors )
	{
		final long[] inputDimensions = Intervals.dimensionsAsLongArray( input );
		final double[] sigmas = new double[ inputDimensions.length ];

		for ( int d = 0; d < inputDimensions.length; ++d )
		{
			sigmas[ d ] = 0.5 / scalingFactors[ d ]; // From Saalfeld
		}

		ImgFactory< T > imgFactory = new ArrayImgFactory( input.randomAccess().get()  );
		RandomAccessibleInterval< T > blurred = Views.translate( imgFactory.create( inputDimensions ), Intervals.minAsLongArray( input )  ) ;

		Gauss3.gauss( sigmas, Views.extendBorder( input ), blurred ) ;

		return blurred;
	}


	public static < T extends RealType< T > & NativeType< T > >
	Point findMaximum( RandomAccessibleInterval< T > rai, double[] calibration )
	{
		Cursor< T > cursor = Views.iterable( rai ).localizingCursor();

		double maxValue = Double.MIN_VALUE;

		long[] maxLoc = new long[ cursor.numDimensions() ];
		cursor.localize( maxLoc );

		while ( cursor.hasNext() )
		{
			final double value = cursor.next().getRealDouble();
			if ( value > maxValue )
			{
				maxValue = value;
				cursor.localize( maxLoc );
			}
		}


		for ( int d = 0; d < rai.numDimensions(); ++d )
		{
			maxLoc[ d ] *= calibration[ d ];
		}
		
		Point point = new Point( maxLoc );

		return point;
	}


	public static < T extends RealType< T > & NativeType< T > >
	boolean isCenterLargest( T center, Neighborhood< T > neighborhood )
	{
		boolean centerIsLargest = true;

		for( T neighbor : neighborhood ) {
			if( neighbor.compareTo( center ) > 0 )
			{
				centerIsLargest = false;
				break;
			}
		}

		return centerIsLargest;
	}

	public static < T extends RealType< T > & NativeType< T > >
	List< RealPoint > findMaxima( RandomAccessibleInterval< T > rai, Shape shape )
	{
		List< RealPoint > points = new ArrayList<>();

		RandomAccessible<Neighborhood<T>> neighborhoods = shape.neighborhoodsRandomAccessible( Views.extendBorder( rai ) );
		RandomAccessibleInterval<Neighborhood<T>> neighborhoodsInterval = Views.interval( neighborhoods, rai );

		LoopBuilder.setImages( neighborhoodsInterval, rai ).forEachPixel(
				(neighborhood, center) -> {
					if( isCenterLargest( center, neighborhood ) )
					{
						points.add( new RealPoint( neighborhood ) );
					}
				}
		);

		return points;
	}



}
