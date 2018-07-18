package de.embl.cba.drosophila;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.Arrays;

public class Projection< T extends RealType< T > & NativeType< T > >
{

    private int[] inputAxesExcludingProjectionAxis;
    private int projectionDimension;
    private RandomAccessibleInterval< T > output;
    private int numOutputDimensions;
    private RandomAccess< T > inputAccess;
    private RandomAccessibleInterval< T > input;
    private FinalInterval projectionInterval;
    private long[] outputDimensions;


    public Projection( RandomAccessibleInterval< T > input, int projectionDimension )
    {
        init( input, projectionDimension, fullProjectionInterval( input ) );
    }


    public Projection( RandomAccessibleInterval< T > input, int projectionDimension, long min, long max )
    {
        projectionInterval = new FinalInterval( new long[]{ min }, new long[]{ max }  );

        init( input, projectionDimension, projectionInterval );
    }

    public Projection( RandomAccessibleInterval< T > input, int projectionDimension, FinalInterval projectionInterval )
    {
        init( input, projectionDimension, projectionInterval );
    }

    private void init( RandomAccessibleInterval< T > input, int projectionDimension, FinalInterval projectionInterval )
    {
        this.numOutputDimensions = input.numDimensions() - 1;

        this.input = input;
        this.inputAccess = input.randomAccess( );

        this.projectionDimension = projectionDimension;
        this.projectionInterval = projectionInterval;

        configureInputAxesExcludingProjectionAxis();
        initializeOutputArrayImg( );
    }

    private FinalInterval fullProjectionInterval( RandomAccessibleInterval< T > input)
    {
        long[] minMax = new long[]{ input.min( projectionDimension ), input.max( projectionDimension ) };
        return Intervals.createMinMax( minMax );
    }

    public RandomAccessibleInterval< T > average( )
    {
        final Cursor< T > outputCursor = Views.iterable( output ).localizingCursor();

        while ( outputCursor.hasNext() )
        {
            outputCursor.fwd( );
            setInputAccess( outputCursor );
            setAverageProjection( outputCursor );
        }

        return output;
    }

    public RandomAccessibleInterval< T > median( )
    {
        final Cursor< T > outputCursor = Views.iterable( output ).localizingCursor();

        while ( outputCursor.hasNext() )
        {
            outputCursor.fwd();
            setInputAccess( outputCursor );
            setMedianProjection( outputCursor );
        }

        return output;
    }

    public RandomAccessibleInterval< T > sum( )
    {
        final Cursor< T > outputCursor = Views.iterable( output ).localizingCursor();

        while ( outputCursor.hasNext() )
        {
            outputCursor.fwd();
            setInputAccess( outputCursor );
            setSumProjection( outputCursor );
        }

        return output;
    }

    public RandomAccessibleInterval< T > maximum( )
    {
        final Cursor< T > outputCursor = Views.iterable( output ).localizingCursor();

        while ( outputCursor.hasNext() )
        {
            outputCursor.fwd();
            setInputAccess( outputCursor );
            setMaximumProjection( outputCursor );
        }

        return output;
    }

    public RandomAccessibleInterval< T > minimum( )
    {
        final Cursor< T > outputCursor = Views.iterable( output ).localizingCursor();

        while ( outputCursor.hasNext() )
        {
            outputCursor.fwd();
            setInputAccess( outputCursor );
            setMinimumProjection( outputCursor );
        }

        return output;
    }

    private void initializeOutputArrayImg()
    {
        setOutputDimensions();
        final ImgFactory< T > factory = new ArrayImgFactory< >( input.randomAccess().get().createVariable() );
        output = factory.create( outputDimensions );
        output = Views.translate( output,  outputOffset() );
    }

    private long[] outputOffset()
    {
        long[] offset = new long[ numOutputDimensions ];
        for ( int d = 0; d < numOutputDimensions; ++d )
        {
            offset[ d ] = input.min( inputAxesExcludingProjectionAxis[ d ] );
        }
        return offset;
    }

    private void setOutputDimensions( )
    {
        outputDimensions = new long[ numOutputDimensions ];

        for ( int d = 0; d < numOutputDimensions; ++d )
        {
            outputDimensions[ d ] = input.dimension( inputAxesExcludingProjectionAxis[ d ] );
        }
    }

    private void setSumProjection( Cursor< T > outputCursor )
    {
        outputCursor.get().setZero();

        for ( long d = projectionInterval.min(0); d <= projectionInterval.max(0 ); ++d )
        {
            inputAccess.setPosition( d, projectionDimension );
            outputCursor.get().add( inputAccess.get() );
        }
    }


    private void setAverageProjection( Cursor< T > outputCursor )
    {
        double average = 0;
        long count = 0;

        for ( long position = projectionInterval.min(0 ); position <= projectionInterval.max(0 ); ++position )
        {
            inputAccess.setPosition( position, projectionDimension );
            average += inputAccess.get().getRealDouble();
            count++;
        }

        average /= count;

        outputCursor.get().setReal( average );

    }

    private void setMedianProjection( Cursor< T > outputCursor )
    {
        double[] values = new double[ (int) projectionInterval.dimension( 0 ) ];
        int min = (int) projectionInterval.min(0);
        int max = (int) projectionInterval.max(0);

        for ( int i = min; i <= max; ++i )
        {
            inputAccess.setPosition( i, projectionDimension );
            values[ i - min ] = inputAccess.get().getRealDouble();
        }

        outputCursor.get().setReal( getMedian( values ) );
    }

    private void setMinimumProjection( Cursor< T > outputCursor )
    {
        double minValue = Double.MAX_VALUE;
        double value;

        for ( int i = (int) projectionInterval.min(0); i <= (int) projectionInterval.max(0 ); ++i )
        {
            inputAccess.setPosition( i, projectionDimension );
            value = inputAccess.get().getRealDouble();
            minValue = value < minValue ? value : minValue;
        }

        outputCursor.get().setReal( minValue );
    }

    private void setMaximumProjection( Cursor< T > outputCursor )
    {
        double maxValue = Double.MIN_VALUE;
        double value;

        for ( int i = (int) projectionInterval.min(0); i <= (int) projectionInterval.max(0 ); ++i )
        {
            inputAccess.setPosition( i, projectionDimension );
            value = inputAccess.get().getRealDouble();
            maxValue = value > maxValue ? value : maxValue;
        }

        outputCursor.get().setReal( maxValue );
    }


    public double getMedian( double[] values) {

        if ( values.length == 1 )
        {
            return values[ 0 ];
        }
        else
        {
            Arrays.sort( values );
            int middle = values.length / 2;
            middle = middle % 2 == 0 ? middle - 1 : middle;
            return values[ middle ];
        }
    }

    private void setInputAccess( Cursor< T > cursorOutput )
    {
        for ( int d = 0; d < numOutputDimensions; ++d )
        {
            inputAccess.setPosition( cursorOutput.getLongPosition( d ) , inputAxesExcludingProjectionAxis[ d ] );
        }
    }

    private void configureInputAxesExcludingProjectionAxis()
    {
        inputAxesExcludingProjectionAxis = new int[ numOutputDimensions ];

        for ( int i = 0; i < numOutputDimensions; i++ )
        {
            if ( i >= projectionDimension )
            {
                inputAxesExcludingProjectionAxis[ i ] = i + 1;
            }
            else
            {
                inputAxesExcludingProjectionAxis[ i ] = i;
            }
        }
    }
}
