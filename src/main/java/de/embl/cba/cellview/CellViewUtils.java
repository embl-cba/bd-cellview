package de.embl.cba.cellview;

import ij.IJ;
import ij.ImagePlus;
import loci.formats.FormatException;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

public class CellViewUtils
{
	public static String getLocalDateAndHourAndMinute()
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

	public static boolean checkFileSize( String filePath, double minimumFileSizeKiloBytes, double maximumFileSizeKiloBytes )
	{
		final File file = new File( filePath );

		if ( ! file.exists() )
		{
			throw new UnsupportedOperationException( "File does not exist: " + file );
		}

		final double fileSizeKiloBytes = getFileSizeKiloBytes( file );

		if ( fileSizeKiloBytes < minimumFileSizeKiloBytes )
		{
			IJ.log( "Skipped too small file: " + file.getName() + "; size [kB]: " + fileSizeKiloBytes);
			return false;
		}
		else if ( fileSizeKiloBytes > maximumFileSizeKiloBytes )
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
		ImagePlus inputImp = null;
		try
		{
			inputImp = CellViewImageProcessor.openImage( filePath );
		} catch ( FormatException e )
		{
			e.printStackTrace();
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
		return inputImp;
	}
}
