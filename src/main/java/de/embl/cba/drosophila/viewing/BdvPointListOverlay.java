package de.embl.cba.drosophila.viewing;

import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.List;


public class BdvPointListOverlay extends BdvOverlay
{

	final List< RealPoint > points;

	public BdvPointListOverlay( List< RealPoint > points )
	{
		super();
		this.points = points;
	}

	@Override
	protected void draw( final Graphics2D g )
	{
		final AffineTransform3D t = new AffineTransform3D();
		getCurrentTransform3D( t );

		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		for ( final RealPoint p : points )
		{
			p.localize( lPos );
			t.apply( lPos, gPos );
			final int size = getSize( gPos[ 2 ] );
			final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
			final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
			g.setColor( getColor( gPos[ 2 ] ) );
			g.fillOval( x, y, size, size );
		}
	}

	private Color getColor( final double depth )
	{
		int alpha = 255 - ( int ) Math.round( Math.abs( depth ) );

		if ( alpha < 64 )
			alpha = 64;

		return new Color( 255, 0, 0, alpha );
	}

	private int getSize( final double depth )
	{
		return ( int ) Math.max( 5, 20 - 0.1 * Math.round( Math.abs( depth ) ) );
	}

}
