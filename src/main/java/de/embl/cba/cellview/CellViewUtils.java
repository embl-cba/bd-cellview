package de.embl.cba.cellview;

import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import loci.formats.FormatException;
import loci.plugins.BF;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class CellViewUtils
{
	public static String localDateAndHourAndMinute()
	{
		final LocalDate localDate = LocalDate.now();
		final LocalTime localTime = LocalTime.now();
		String localDateAndTime = localDate + "-" + localTime.getHour() + "-" + localTime.getMinute();
		return localDateAndTime;
	}

	public static Color getColor( String name ) {
		try {
			name = name.replace( "*", "" ); // "*" is used to indicate to include in merge
			return (Color)Color.class.getField(name.trim().toUpperCase()).get(null);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new RuntimeException( "Could not convert " + name + " to a Color." );
		}
	}

	public static boolean checkFileSize( String filePath, String fileSizeRangeCSV )
	{
		final double[] fileSizeMinMax = Arrays.stream( fileSizeRangeCSV.split( "," ) ).mapToDouble( x -> Double.parseDouble( x.trim() ) ).toArray();

		final File file = new File( filePath );

		if ( ! file.exists() )
		{
			throw new UnsupportedOperationException( "File does not exist: " + file );
		}

		final double fileSizeKiloBytes = getFileSizeKiloBytes( file );

		if ( fileSizeKiloBytes < fileSizeMinMax[ 0 ] )
		{
			IJ.log( "Skipped too small file: " + file.getName() + "; size [kB]: " + fileSizeKiloBytes);
			return false;
		}
		else if ( fileSizeKiloBytes > fileSizeMinMax[ 1 ] )
		{
			IJ.log( "Skipped too large file: " + file.getName() + "; size [kB]: " + fileSizeKiloBytes);
			return false;
		}
		else
		{
			return true;
		}
	}

	public static String[] getValidFileNames( File inputDirectory )
	{
		return inputDirectory.list( new FilenameFilter()
		{
			@Override
			public boolean accept( File dir, String name )
			{
				if ( ! name.contains( ".tif" ) ) return false;
					return true;
			}
		} );
	}

	public static double getFileSizeKiloBytes( File file )
	{
		return file.length() / 1024.0 ;
	}

	public static ImagePlus tryOpenImage( String filePath )
	{
		try
		{
			return openImage( filePath );
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	public static void glimpseTable( JTable jTable )
	{
		IJ.log( "# Table Info"  );
		IJ.log( "Number of rows: " + jTable.getRowCount() );
		final List< String > columnNames = Tables.getColumnNames( jTable );
		int numRows = Math.min( jTable.getRowCount(), 5 );
		for ( String columnName : columnNames )
		{
			final int columnIndex = jTable.getColumnModel().getColumnIndex( columnName );

			String firstRows = "";
			for ( int rowIndex = 0; rowIndex < numRows; rowIndex++ )
			{
				firstRows += jTable.getValueAt( rowIndex, columnIndex );
				firstRows += ", ";
			}
			firstRows += "...";

			IJ.log( columnName + ": " + firstRows );
		}
	}

	public static File selectTableDialog( File[] tableFiles )
	{
		final GenericDialog gd = new GenericDialog( "Please select table for image preview" );
		final String[] tablePaths = Arrays.stream( tableFiles ).map( x -> x.toString() ).toArray( String[]::new );
		gd.addChoice( "Table", tablePaths, tablePaths[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		File selectedTableFile = new File( gd.getNextChoice() );
		IJ.log( "Selected Table: " + selectedTableFile );
		return selectedTableFile;
	}

	public static String selectGateDialog( String currentSelectedGate, Set< String > gates )
	{
		final GenericDialog gd = new GenericDialog( "Please select gate for image preview" );
		final String[] choices = gates.stream().toArray( String[]::new );

		gd.addChoice( "Gate", choices, currentSelectedGate );
		gd.showDialog();

		if ( gd.wasCanceled() )
			return currentSelectedGate;

		String selectedGate = gd.getNextChoice();

		return selectedGate;
	}

	public static String selectGateColumnDialog( JTable jTable )
	{
		final List< String > columnNames = Tables.getColumnNames( jTable );
		columnNames.add( "There is no gate column" );
		final GenericDialog gd = new GenericDialog( "Please select gate column" );
		final String[] items = columnNames.stream().toArray( String[]::new );
		gd.addChoice( "Gate colum", items, items[ 0 ] );
		gd.showDialog();
		final String selectedColumn = gd.getNextChoice();

		if ( selectedColumn.equals( "There is no gate column" ) )
			return null;
		else
			return selectedColumn;
	}

	public static ImagePlus openImage( String filePath ) throws FormatException, IOException
	{
		final ImagePlus[] imps = BF.openImagePlus( filePath );
		return imps[ 0 ];
	}
}
