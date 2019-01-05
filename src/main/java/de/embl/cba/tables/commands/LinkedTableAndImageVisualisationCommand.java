package de.embl.cba.tables.commands;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.RandomAccessibleIntervalSource4D;
import de.embl.cba.bdv.utils.behaviour.BdvSelectionEventHandler;
import de.embl.cba.bdv.utils.converters.argb.SelectableVolatileARGBConverter;
import de.embl.cba.bdv.utils.converters.argb.VolatileARGBConvertedRealSource;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.TableBdvConnector;
import de.embl.cba.tables.TableUtils;
import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.objects.ObjectTablePanel;
import de.embl.cba.tables.objects.ui.ObjectCoordinateColumnsSelectionUI;
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


@Plugin(type = Command.class, menuPath = "Plugins>Measurements>Linked Table And Image Visualisation" )
public class LinkedTableAndImageVisualisationCommand implements Command
{
	@Parameter ( label = "Results table" )
	public File inputTableFile;

	@Parameter ( label = "Object label column index" )
	public Integer objectLabelsColumnIndex = 0;

	@Parameter ( label = "Label mask (single channel, 2D+t or 3D+t)" )
	public File inputLabelMasksFile;

	@Parameter ( label = "Intensities (optional)", required = false )
	public File inputIntensitiesFile;

	public  ObjectTablePanel objectTablePanel;
	private SelectableVolatileARGBConverter labelsConverter;
	private VolatileARGBConvertedRealSource labelsSource;
	private JTable table;
	private Bdv bdv;
	private BdvSelectionEventHandler bdvSelectionEventHandler;

	@Override
	public void run()
	{
		loadTable();

		loadLabels();

		showImagesWithBdv();

		showTablePanel();

		bdvSelectionEventHandler = new BdvSelectionEventHandler(
				bdv,
				labelsSource,
				labelsConverter );

		new TableBdvConnector( objectTablePanel, bdvSelectionEventHandler );

		new ObjectCoordinateColumnsSelectionUI( objectTablePanel );
	}

	public void showTablePanel()
	{
		objectTablePanel = new ObjectTablePanel( table );
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
		labelsConverter = new SelectableVolatileARGBConverter();
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
