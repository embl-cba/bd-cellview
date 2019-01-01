package de.embl.cba.tables.commands;

import bdv.util.*;
import de.embl.cba.bdv.utils.argbconversion.SelectableRealVolatileARGBConverter;
import de.embl.cba.bdv.utils.argbconversion.VolatileARGBConvertedRealSource;
import de.embl.cba.bdv.utils.behaviour.BehaviourSelectionEventHandler;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.TableUtils;
import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.objects.ObjectTablePanel;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.io.File;
import java.io.IOException;


@Plugin(type = Command.class, menuPath = "Plugins>Measurements>Object Measurements Review" )
public class ObjectMeasurementsReviewCommand implements Command
{
	@Parameter ( label = "Results table" )
	public File inputTableFile;

	@Parameter ( label = "Object label column index" )
	public Integer objectLabelsColumnIndex = 0;

	@Parameter ( label = "Label mask (single channel, 2D+t or 3D+t)" )
	public File inputLabelMasksFile;

	@Parameter ( label = "Intensities (optional)", required = false )
	public File inputIntensitiesFile;

	private SelectableRealVolatileARGBConverter labelsConverter;
	private VolatileARGBConvertedRealSource labelsSource;
	private JTable table;
	private Bdv bdv;
	private ObjectTablePanel objectTablePanel;

	@Override
	public void run()
	{
		loadTable();

		loadLabels();

		showImagesWithBdv();

		showTablePanel();

		final BehaviourSelectionEventHandler behaviourSelectionEventHandler = new BehaviourSelectionEventHandler( bdv, labelsSource, labelsConverter );


	}

	public void showTablePanel()
	{
		objectTablePanel = new ObjectTablePanel( table, bdv, labelsConverter );
		objectTablePanel.showPanel();
		objectTablePanel.setCoordinateColumn( ObjectCoordinate.Label, table.getColumnName( objectLabelsColumnIndex ) );
	}

	public void showImagesWithBdv()
	{
		bdv = BdvFunctions.show( labelsSource, BdvOptions.options().is2D() ).getBdvHandle();
	}

	public void loadTable()
	{
		table = loadTable( inputTableFile );
	}

	public void loadLabels()
	{
		final RandomAccessibleIntervalSource4D labels = loadImage( inputLabelMasksFile );
		labelsConverter = new SelectableRealVolatileARGBConverter();
		labelsSource = new VolatileARGBConvertedRealSource( labels, labelsConverter );
	}

	public JTable loadTable( File file )
	{
		try
		{
			return TableUtils.loadTable( file, null );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	public static RandomAccessibleIntervalSource4D loadImage( File file )
	{
		final ImagePlus imagePlus = IJ.openImage( file.toString() );

		if ( imagePlus.getNChannels() > 1 )
		{
			Logger.error( "Only single channel images are supported.");
			return null;
		}

		RandomAccessibleInterval< RealType > wrap = ImageJFunctions.wrapReal( imagePlus );

		if ( imagePlus.getNFrames() == 1 )
		{
			// needs to be a movie
			wrap = Views.addDimension( wrap, 0, 0 );
		}

		if ( imagePlus.getNSlices() == 1 )
		{
			// needs to be 3D
			wrap = Views.addDimension( wrap, 0, 0 );
			wrap = Views.permute( wrap, 2, 3 );
		}

		return new RandomAccessibleIntervalSource4D( wrap, Util.getTypeFromInterval( wrap ), imagePlus.getTitle() );
	}

}
