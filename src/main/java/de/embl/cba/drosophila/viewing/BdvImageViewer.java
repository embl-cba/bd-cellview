package de.embl.cba.drosophila.viewing;

import bdv.util.*;
import net.imagej.ImgPlus;
import net.imagej.axis.LinearAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Arrays;
import java.util.List;

public class BdvImageViewer
{
	public static < T extends RealType< T > & NativeType< T > >
	void show( RandomAccessibleInterval< T > rai )
	{
		show( rai, "", null, null, false );
	}


	public static < T extends RealType< T > & NativeType< T > >
	void show( RandomAccessibleInterval rai, String title, List< RealPoint > points, double[] calibration, boolean resetViewTransform )
	{
		final Bdv bdv;

		if ( calibration == null )
		{
			calibration = new double[ rai.numDimensions() ];
			Arrays.fill( calibration, 1.0 );
		}

		if ( rai.numDimensions() ==  2 )
		{
			bdv = BdvFunctions.show( rai, title, BdvOptions.options().is2D().sourceTransform( calibration ) );
		}
		else
		{
			bdv = BdvFunctions.show( rai, title, BdvOptions.options().sourceTransform( calibration ) );
		}

		if ( points != null )
		{
			BdvOverlay bdvOverlay = new BdvPointListOverlay( points );
			BdvFunctions.showOverlay( bdvOverlay, "overlay", BdvOptions.options().addTo( bdv ) );
		}

		if ( resetViewTransform )
		{
			resetViewTransform( bdv );
		}


	}

	private static void resetViewTransform( Bdv bdv )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		affineTransform3D.scale( 2.5D );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affineTransform3D );

	}

	private static BdvStackSource show3DImgPlusInBdv( ImgPlus imgPlus )
	{

		BdvStackSource bdvStackSource = BdvFunctions.show(
				imgPlus,
				imgPlus.getName(),
				Bdv.options().sourceTransform(
						((LinearAxis )imgPlus.axis( 0 )).scale(),
						((LinearAxis)imgPlus.axis( 1 )).scale(),
						((LinearAxis)imgPlus.axis( 2 )).scale() )
		);

		return bdvStackSource;
	}
}
