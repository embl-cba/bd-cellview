import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class HyperSlicingTest
{

	public static void main( String... args )
	{

		RandomAccessibleInterval< BitType > bits = ArrayImgs.bits( new long[]{ 5, 5, 5 } );
		bits = Views.translate( bits, new long[]{-1,-1,-1});

		final RandomAccess< BitType > randomAccess = bits.randomAccess();
		randomAccess.setPosition( new long[]{ 3, -1, 3} );
		randomAccess.get().set( true );

		final IntervalView< BitType > hyperSlice = Views.hyperSlice( bits, 0, 3 );

		final Cursor< BitType > cursor = hyperSlice.cursor();

		while( cursor.hasNext() )
		{
			if ( cursor.next().get() )
			{
				System.out.println( " y : " + cursor.getLongPosition( 0 ) );
				System.out.println( " z : " + cursor.getLongPosition( 1 ) );
			}
		}


		BdvFunctions.show( bits, "" );

		BdvFunctions.show( hyperSlice, "hyperSlice", BdvOptions.options().is2D() );

	}
}
