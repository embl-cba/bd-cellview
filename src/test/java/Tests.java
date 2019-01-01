import bdv.util.Bdv;
import bdv.util.RandomAccessibleIntervalSource;
import de.embl.cba.bdv.utils.argbconversion.SelectableRealVolatileARGBConverter;
import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.objects.ObjectTablePanel;
import de.embl.cba.tables.TableUtils;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public abstract class Tests
{

	public static final String OBJECT_GROUPING_TRIGGER = "ctrl G";

	public static JTable loadObjectTableFor2D16BitLabelMask() throws IOException
	{
		final File tableFile = new File( ExampleInteractiveAttributeColoring.class.getResource( "2d-16bit-labelMask-Morphometry.csv" ).getFile() );

		return TableUtils.loadTable( tableFile, "," );
	}

	public static RandomAccessibleIntervalSource load2D16BitLabelMask()
	{
		final ImagePlus imagePlus = IJ.openImage( ExampleInteractiveAttributeColoring.class.getResource( "2d-16bit-labelMask.tif" ).getFile() );

		RandomAccessibleInterval< RealType > wrap = ImageJFunctions.wrapReal( imagePlus );

		// needs to be at least 3D
		wrap = Views.addDimension( wrap, 0, 0);

		return new RandomAccessibleIntervalSource( wrap, Util.getTypeFromInterval( wrap ), imagePlus.getTitle() );
	}

	public static void createInteractiveTablePanel( JTable jTable, Bdv bdv, SelectableRealVolatileARGBConverter selectableConverter )
	{
		final ObjectTablePanel objectTablePanel = new ObjectTablePanel( jTable, bdv, selectableConverter );

		objectTablePanel.showPanel();

		objectTablePanel.setCoordinateColumnIndex( ObjectCoordinate.Label, 0 );
	}
}
