package de.embl.cba.drosophila;

import de.embl.cba.drosophila.geometry.CentroidsParameters;
import de.embl.cba.drosophila.geometry.CoordinatesAndValues;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.axis.LinearAxis;
import net.imglib2.*;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.drosophila.Constants.*;
import static de.embl.cba.drosophila.viewing.BdvImageViewer.show;
import static java.lang.Math.*;

public class Utils
{
	public static int imagePlusChannelDimension = 2;

	public static < T extends RealType< T > & NativeType< T > >
	CoordinatesAndValues computeAverageIntensitiesAlongAxis(
			RandomAccessibleInterval< T > rai, int axis )
	{
		final CoordinatesAndValues coordinatesAndValues = new CoordinatesAndValues();

		for ( long coordinate = rai.min( axis ); coordinate <= rai.max( axis ); ++coordinate )
		{
			final IntervalView< T > intensitySlice = Views.hyperSlice( rai, axis, coordinate );
			coordinatesAndValues.coordinates.add( (double) coordinate );
			coordinatesAndValues.values.add( computeAverage( intensitySlice ) );
		}

		return coordinatesAndValues;
	}

	public static < T extends RealType< T > & NativeType< T > >
	CoordinatesAndValues computeAverageIntensitiesAlongAxis(
			RandomAccessibleInterval< T > rai, RandomAccessibleInterval< BooleanType > mask, int axis )
	{
		final CoordinatesAndValues coordinatesAndValues = new CoordinatesAndValues();

		for ( long coordinate = rai.min( axis ); coordinate <= rai.max( axis ); ++coordinate )
		{
			final IntervalView< T > intensitySlice = Views.hyperSlice( rai, axis, coordinate );
			final IntervalView< BooleanType > maskSlice = Views.hyperSlice( mask, axis, coordinate );

			coordinatesAndValues.coordinates.add( (double) coordinate );
			coordinatesAndValues.values.add( computeAverage( intensitySlice, maskSlice ) );
		}

		return coordinatesAndValues;
	}


	public static CentroidsParameters computeCentroidsParametersAlongXAxis(
			RandomAccessibleInterval< BooleanType > rai,
			double calibration )
	{

		CentroidsParameters centroidsParameters = new CentroidsParameters();

		final double[] unitVectorInNegativeZDirection = new double[]{ 0, -1 };

		for ( long coordinate = rai.min( X ); coordinate <= rai.max( X ); ++coordinate )
		{

			final double[] centroid = computeCentroidPerpendicularToAxis( rai, X, coordinate );

			if ( centroid != null )
			{
				double centroidNorm = vectorNorm( centroid );

				/**
				 *  centroid[ 0 ] is the y-axis coordinate
				 *  the sign of the y-axis coordinate determines the sign of the angle,
				 *  i.e. the direction of rotation
				 */
				final double angle = Math.signum( centroid[ 0 ] ) * 180 / Math.PI * acos( dotProduct( centroid, unitVectorInNegativeZDirection ) / centroidNorm );

				centroidsParameters.distances.add( centroidNorm * calibration );
				centroidsParameters.angles.add( angle );
				centroidsParameters.axisCoordinates.add( (double) coordinate );
				centroidsParameters.centroids.add( new RealPoint( coordinate * calibration , centroid[ 0 ] * calibration , centroid[ 1 ] * calibration  ) );
			}

		}

		return centroidsParameters;

	}

	public static double vectorNorm( double[] vector )
	{
		double centroidNorm = 0;

		for ( int d = 0; d < vector.length; ++d )
		{
			centroidNorm += vector[ d ] * vector[ d ];
		}

		centroidNorm = Math.sqrt( centroidNorm );

		return centroidNorm;
	}

	public static double dotProduct( double[] vector01, double[] vector02  )
	{
		double dotProduct = 0;

		for ( int d = 0; d < vector01.length; ++d )
		{
			dotProduct += vector01[ d ] * vector02[ d ];
		}

		return dotProduct;
	}

	private static double[] computeCentroidPerpendicularToAxis( RandomAccessibleInterval< BooleanType > rai, int axis, long coordinate )
	{
		final IntervalView< BooleanType > slice = Views.hyperSlice( rai, axis, coordinate );

		int numHyperSliceDimensions = rai.numDimensions() - 1;

		final double[] centroid = new double[ numHyperSliceDimensions ];

		int numPoints = 0;

		final Cursor< BooleanType > cursor = slice.cursor();

		while( cursor.hasNext() )
		{
			if( cursor.next().get() )
			{
				for ( int d = 0; d < numHyperSliceDimensions; ++d )
				{
					centroid[ d ] += cursor.getLongPosition( d );
					numPoints++;
				}
			}
		}

		if ( numPoints > 0 )
		{
			for ( int d = 0; d < numHyperSliceDimensions; ++d )
			{
				centroid[ d ] /= 1.0D * numPoints;
			}
			return centroid;
		}
		else
		{
			return null;
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createBlurredRai( RandomAccessibleInterval< T > rai, double sigma, double scaling )
	{
		ImgFactory< T > imgFactory = new ArrayImgFactory( rai.randomAccess().get()  );

		RandomAccessibleInterval< T > blurred = imgFactory.create( Intervals.dimensionsAsLongArray( rai ) );

		blurred = Views.translate( blurred, Intervals.minAsLongArray( rai ) );

		Gauss3.gauss( sigma / scaling, Views.extendBorder( rai ), blurred ) ;

		return blurred;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createGaussFilteredArrayImg( RandomAccessibleInterval< T > rai, double[] sigmas )
	{
		ImgFactory< T > imgFactory = new ArrayImgFactory( rai.randomAccess().get()  );

		RandomAccessibleInterval< T > blurred = imgFactory.create( Intervals.dimensionsAsLongArray( rai ) );

		blurred = Views.translate( blurred, Intervals.minAsLongArray( rai ) );

		Gauss3.gauss( sigmas, Views.extendBorder( rai ), blurred ) ;

		return blurred;
	}



	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createAverageProjection(
			RandomAccessibleInterval< T > rai, int d, double min, double max, double scaling )
	{
		Projection< T > projection = new Projection< T >(  rai, d,  new FinalInterval( new long[]{ (long) ( min / scaling) },  new long[]{ (long) ( max / scaling ) } ) );
		return projection.average();
	}

	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< BitType > createBinaryImage(
			RandomAccessibleInterval< T > input, double doubleThreshold )
	{
		final ArrayImg< BitType, LongArray > binaryImage = ArrayImgs.bits( Intervals.dimensionsAsLongArray( input ) );

		T threshold = input.randomAccess().get().copy();
		threshold.setReal( doubleThreshold );

		final BitType one = new BitType( true );
		final BitType zero = new BitType( false );

		LoopBuilder.setImages( input, binaryImage ).forEachPixel( ( i, b ) ->
				{
					b.set( i.compareTo( threshold ) > 0 ?  one : zero );
				}
		);

		return binaryImage;

	}

	public static < T extends RealType< T > & NativeType< T > >
	AffineTransform3D createOrientationTransformation(
			RandomAccessibleInterval< T > rai, int longAxisDimension, double derivativeDelta, double scaling,
			boolean showPlots )
	{

		final CoordinatesAndValues coordinatesAndValues = computeAverageIntensitiesAlongAxis( rai, longAxisDimension );

		ArrayList< Double > absoluteDerivatives = computeAbsoluteDerivatives( coordinatesAndValues.values, (int) (derivativeDelta / scaling ));

		double maxLoc = computeMaxLoc( coordinatesAndValues.coordinates, absoluteDerivatives );

		System.out.println( "maxLoc = " + maxLoc );

		if ( showPlots )
		{
			Plots.plot( coordinatesAndValues.coordinates, coordinatesAndValues.values, "x", "intensity" );
			Plots.plot( coordinatesAndValues.coordinates, absoluteDerivatives, "x", "abs( derivative )" );
		}

		if ( maxLoc > 0 )
		{
			AffineTransform3D affineTransform3D = new AffineTransform3D();
			affineTransform3D.rotate( Z, toRadians( 180.0D ) );

			return affineTransform3D;
		}
		else
		{
			return new AffineTransform3D();
		}

	}

	public static ArrayList< Double > computeAbsoluteDerivatives( ArrayList< Double > values, int di )
	{
		final ArrayList< Double > derivatives = new ArrayList<>();

		for ( int i = di / 2 + 1; i < values.size() - di / 2 - 1; ++i )
		{
			derivatives.add( abs( values.get( i + di / 2 ) - values.get( i - di / 2 ) ) );
		}

		return derivatives;
	}

	public static double computeMaxLoc( ArrayList< Double > coordinates, ArrayList< Double > values )
	{
		double max = Double.MIN_VALUE;
		double maxLoc = coordinates.get( 0 );

		for ( int i = 0; i < values.size(); ++i )
		{
			if ( values.get( i ) > max )
			{
				max = values.get( i );
				maxLoc = coordinates.get( i );
			}
		}

		return maxLoc;
	}

	public static double[] getCalibration( Dataset dataset )
	{
		double[] calibration = new double[ 3 ];

		for ( int d : XYZ )
		{
			calibration[ d ] = ( ( LinearAxis ) dataset.getImgPlus().axis( d ) ).scale();
		}

		return calibration;
	}

	public static double[] getCalibration( ImagePlus imp )
	{
		double[] calibration = new double[ 3 ];

		calibration[ X ] = imp.getCalibration().pixelWidth;
		calibration[ Y ] = imp.getCalibration().pixelHeight;
		calibration[ Z ] = imp.getCalibration().pixelDepth;

		return calibration;
	}

	public static void correctCalibrationForSubSampling( double[] calibration, int subSampling )
	{
		for ( int d : XYZ )
		{
			calibration[ d ] *= subSampling;
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	double computeAverage( final RandomAccessibleInterval< T > rai )
	{
		final Cursor< T > cursor = Views.iterable( rai ).cursor();

		double average = 0;

		while ( cursor.hasNext() )
		{
			average += cursor.next().getRealDouble();
		}

		average /= Views.iterable( rai ).size();

		return average;
	}




	public static < T extends RealType< T > & NativeType< T > >
	double computeAverage( final RandomAccessibleInterval< T > rai, final RandomAccessibleInterval< BooleanType > mask )
	{
		final Cursor< BooleanType > cursor = Views.iterable( mask ).cursor();
		final RandomAccess< T > randomAccess = rai.randomAccess();

		randomAccess.setPosition( cursor );

		double average = 0;
		long n = 0;

		while ( cursor.hasNext() )
		{
			if ( cursor.next().get() )
			{
				randomAccess.setPosition( cursor );
				average += randomAccess.get().getRealDouble();
				++n;
			}
		}

		average /= n;

		return average;
	}


	public static < T extends RealType< T > & NativeType< T > >
	List< RealPoint > computeMaximumLocation( RandomAccessibleInterval< T > blurred, int sigmaForBlurringAverageProjection )
	{
		Shape shape = new HyperSphereShape( sigmaForBlurringAverageProjection );

		List< RealPoint > points = Algorithms.findLocalMaximumValues( blurred, shape );

		return points;
	}

	public static List< RealPoint > asRealPointList( Point maximum )
	{
		List< RealPoint > realPoints = new ArrayList<>();
		final double[] doubles = new double[ maximum.numDimensions() ];
		maximum.localize( doubles );
		realPoints.add( new RealPoint( doubles) );

		return realPoints;
	}

	public static < T extends NumericType< T > & NativeType< T > >
	RandomAccessibleInterval< T > copyAsArrayImg( RandomAccessibleInterval< T > rai )
	{

		RandomAccessibleInterval< T > copy = new ArrayImgFactory( rai.randomAccess().get() ).create( rai );
		copy = Transforms.adjustOrigin( rai, copy );

		final Cursor< T > out = Views.iterable( copy ).localizingCursor();
		final RandomAccess< T > in = rai.randomAccess();

		while( out.hasNext() )
		{
			out.fwd();
			in.setPosition( out );
			out.get().set( in.get() );
		}

		return copy;
	}


	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > createSeeds( RandomAccessibleInterval< T > rai, Shape shape, double threshold )
	{

		RandomAccessibleInterval< BitType > maxima = ArrayImgs.bits( Intervals.dimensionsAsLongArray( rai ) );
		maxima = Transforms.adjustOrigin( rai, maxima );

		RandomAccessible< Neighborhood< T > > neighborhoods = shape.neighborhoodsRandomAccessible( Views.extendBorder( rai ) );
		RandomAccessibleInterval< Neighborhood< T > > neighborhoodsInterval = Views.interval( neighborhoods, rai );

		final Cursor< Neighborhood< T > > neighborhoodCursor = Views.iterable( neighborhoodsInterval ).cursor();
		final RandomAccess< T > intensitiesRandomAccess = rai.randomAccess();
		final RandomAccess< BitType > maximaRandomAccess = maxima.randomAccess();

		while ( neighborhoodCursor.hasNext() )
		{
			final Neighborhood< T > neighborhood = neighborhoodCursor.next();

			intensitiesRandomAccess.setPosition( neighborhood );
			T centerValue = intensitiesRandomAccess.get();

			if ( centerValue.getRealDouble() > threshold )
			{
				maximaRandomAccess.setPosition( neighborhood );
				maximaRandomAccess.get().set( true );
			}
			else if ( isCenterLargestOrEqual( centerValue, neighborhood ) )
			{
				if ( centerValue.getRealDouble() > 0 )
				{
					// local maximum and larger than zero
					maximaRandomAccess.setPosition( neighborhood );
					maximaRandomAccess.get().set( true );
				}
			}

		}

		return maxima;
	}


	public static < T extends IntegerType >
	ImgLabeling< Integer, IntType > createLabelImg( RandomAccessibleInterval< T > rai )
	{
		final Img< IntType > labelImg = ArrayImgs.ints( Intervals.dimensionsAsLongArray( rai ) );
		final ImgLabeling< Integer, IntType > labeling = new ImgLabeling<>( labelImg );

		ConnectedComponents.labelAllConnectedComponents( Views.extendBorder( rai ), labelImg, ConnectedComponents.StructuringElement.EIGHT_CONNECTED );

		return labeling;
	}

	private static < T extends RealType< T > & NativeType< T > >
	boolean isCenterLargestOrEqual( T center, Neighborhood< T > neighborhood )
	{
		for( T neighbor : neighborhood )
		{
			if( neighbor.compareTo( center ) > 0 )
			{
				return false;
			}
		}
		return true;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > invertedView( RandomAccessibleInterval< T > input )
	{
		final double maximum = Algorithms.findMaximumValue( input );

		final RandomAccessibleInterval< T > inverted = Converters.convert( input, ( i, o ) -> {
			o.setReal( ( int ) ( maximum - i.getRealDouble() ) );
		},  Views.iterable( input ).firstElement() );

		return inverted;
	}



	public static RandomAccessibleInterval< IntType >  asIntImg( ImgLabeling< Integer, IntType > labeling )
	{
		final RandomAccessibleInterval< IntType > intImg =
				Converters.convert( ( RandomAccessibleInterval< LabelingType< Integer > > ) labeling,
						( i, o ) -> {
							o.set( i.getIndex().getInteger() );
						}, new IntType() );

		return intImg;
	}


}
