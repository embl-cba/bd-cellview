package de.embl.cba.imflow;

import de.embl.cba.tables.FileUtils;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.*;
import ij.plugin.filter.Convolver;
import ij.process.ColorProcessor;
import ij.process.LUT;
import loci.formats.FormatException;
import loci.plugins.BF;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BDVulcanProcessor
{
	public static final String OVERLAY = "Overlay";
	public static final String FORWARD_SCATTER = "ForewardScatter";
	public static final String SIDE_SCATTER = "SideScatter";
	public static final String WHITE = "White";
	public static final String GREEN = "Green";
	public static final String MAGENTA = "Magenta";
	public static final String[] MONTAGE_SEQUENCE = { OVERLAY, WHITE, GREEN, MAGENTA };

	public static final String VIEW_RAW = "Raw";
	public static final String VIEW_PROCESSED_MONTAGE = "Processed Montage";
	public static final String VIEW_PROCESSED_BF_GF_OVERLAY = "Processed BrightField GreenFluo Overlay";
	public static final String VIEW_PROCESSED_OVERLAY = "Processed Overlay";
	public static final String VIEW_PROCESSED_OVERLAY_AND_INDIVIDUAL_CHANNELS = "Processed Overlay And Individual Channels";

	public static final String VIEW_RAW_AND_MONTAGE = "Raw and Processed Montage";
	private static HashMap< String, Integer > colorToSlice = new HashMap<>(  );
	private static HashMap< String, double[] > colorToRange = new HashMap<>(  );

	public static HashMap< String, Integer > getColorToSlice()
	{
		return colorToSlice;
	}

	public static HashMap< String, double[] > getColorToRange()
	{
		return colorToRange;
	}

	public static ImagePlus createProcessedImage(
			String filePath,
			Map< String, double[] > nameToRange,
			Map< String, Integer > nameToSlice,
			int horizontalCropNumPixels,
			String viewingModality )
	{
		ImagePlus imp = tryOpenImage( filePath );

		if ( viewingModality.equals( BDVulcanProcessor.VIEW_RAW ) ) return imp;

		imp = processImage( imp );

		if ( horizontalCropNumPixels > 0 )
		{
			imp.setRoi( new Rectangle( horizontalCropNumPixels, 0, imp.getWidth() - 2 * horizontalCropNumPixels, imp.getHeight() ) );
			imp = imp.crop( "stack" );
		}

		final Map< String, ImagePlus > colorToImp = extractChannels( imp, nameToRange, nameToSlice );

		addOverlayAndConvertImagesToRGB( colorToImp );

		imp = createOutputImp( colorToImp, viewingModality );

		imp.setTitle( new File( filePath ).getName() );

		return imp;
	}

	public static ImagePlus tryOpenImage( String filePath )
	{
		ImagePlus inputImp = null;
		try
		{
			inputImp = openImage( filePath );
		} catch ( FormatException e )
		{
			e.printStackTrace();
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
		return inputImp;
	}

	public static HashMap< String, ImagePlus > extractChannels(
			ImagePlus inputImp,
			Map< String, double[] > colorToRange,
			Map< String, Integer > colorToSlice )
	{
		final HashMap< String, ImagePlus > nameToImagePlus = new HashMap<>();

		for( String color : colorToSlice.keySet() )
		{
			final ImagePlus channel =
					extractChannelFromImagePlus(
							inputImp,
							colorToSlice.get( color ),
							color,
							colorToRange.get( color )[ 0 ],
							colorToRange.get( color )[ 1 ] );

			nameToImagePlus.put( color, channel );
		}

		return nameToImagePlus;
	}

	public static void putImagePlus(
			ImagePlus inputImp,
			HashMap< String, ImagePlus > nameToImp,
			String name,
			Map< String, double[] > nameToRange,
			Map< String, Integer > nameToSlice )
	{
		final ImagePlus channel =
				extractChannelFromImagePlus(
						inputImp,
						nameToSlice.get( name ),
						name,
						nameToRange.get( name )[ 0 ],
						nameToRange.get( name )[ 1 ] );

		nameToImp.put( name, channel );
	}

	public static ImagePlus openImage( String filePath ) throws FormatException, IOException
	{
		final ImagePlus[] imps = BF.openImagePlus( filePath );
		return imps[ 0 ];
	}

	public static ImagePlus processImage( ImagePlus imp )
	{
		int height = imp.getHeight();
		int scaledHeight = (int) ( 0.8 * height );
		imp = Scaler.resize( imp, imp.getWidth(), scaledHeight, imp.getStackSize(), "bilinear" );
		//IJ.run(imp, "Scale...", "x=1.0 y=0.8 z=1.0 interpolation=Bilinear average process create title=scaled");

		final Convolver convolver = new Convolver();
		convolver.setNormalize( true );
		for (int i = 1; i <= imp.getStackSize(); i++)
		{
			convolver.convolve(
					imp.getStack().getProcessor(i),
					new float[]{0, 1.6F, 4, 1.6F, 0}, 5, 1 );
			//IJ.run(inputImp, "Convolve...", "text1=[0 1.6 4 1.6 0\n] normalize stack");
		}

		return imp;
	}

	public static void addOverlayAndConvertImagesToRGB( Map< String, ImagePlus > colorToImp )
	{
		final CompositeImage overlay = createOverlay( colorToImp );
		colorToImp.put( OVERLAY, overlay );

		for ( String color : colorToImp.keySet() )
			colorToImp.put( color, toRGB( colorToImp.get( color ) ) );
	}

	public static ImagePlus toRGB( ImagePlus imp )
	{
		return new ImagePlus( "", imp.getProcessor().convertToColorProcessor() );
	}

	public static ImagePlus createOutputImp( final Map< String, ImagePlus > colorToImp, String viewingModality )
	{
		if ( viewingModality.equals( VIEW_PROCESSED_OVERLAY ) )
		{
			return colorToImp.get( OVERLAY );
		}
		else if ( viewingModality.equals( VIEW_PROCESSED_OVERLAY_AND_INDIVIDUAL_CHANNELS ))
		{
			// make montage
			final ImagePlus firstImp = colorToImp.values().iterator().next();
			final int width = firstImp.getWidth();
			final int height = firstImp.getHeight();

			int montageWidth = colorToImp.size() * width;
			int montageHeight = height;

			final ImagePlus montageImp = new ImagePlus( "montage", new ColorProcessor( montageWidth, montageHeight ) );

			final StackInserter inserter = new StackInserter();

			int i = 0;
			for ( String color : MONTAGE_SEQUENCE )
			{
				if ( colorToImp.keySet().contains( color ) )
					inserter.insert( colorToImp.get( color ), montageImp, (i++) * width, 0 );
			}

			return montageImp;
		}
		else if ( viewingModality.equals( BDVulcanProcessor.VIEW_PROCESSED_MONTAGE ) )
		{
			// NOTE: This viewing modality is currently not used

			// make montage
			final int width = colorToImp.get( WHITE ).getWidth();
			final int height = colorToImp.get( WHITE ).getHeight();

			int montageWidth = 3 * width;
			int montageHeight = 2 * height;

			final ImagePlus montageImp = new ImagePlus( "montage", new ColorProcessor( montageWidth, montageHeight ) );

			final StackInserter inserter = new StackInserter();
			inserter.insert( colorToImp.get( WHITE ), montageImp, 0, 0 );
			inserter.insert( colorToImp.get( GREEN ), montageImp, width, 0 );
			inserter.insert( colorToImp.get( OVERLAY ), montageImp, 2 * width, 0 );
			inserter.insert( colorToImp.get( SIDE_SCATTER ), montageImp, 0, height );
			inserter.insert( colorToImp.get( FORWARD_SCATTER ), montageImp, width, height );

			return montageImp;
		}
		else
		{
			throw new UnsupportedOperationException( "Viewing modality not supported: " + viewingModality );
		}
	}

	public static CompositeImage createOverlay( ImagePlus gray, ImagePlus green )
	{
		final CompositeImage overlay = ( CompositeImage ) RGBStackMerge.mergeChannels( new ImagePlus[]{ gray, green }, true );

		overlay.setC( 1 );
		overlay.setChannelLut( LUT.createLutFromColor( Color.WHITE ) );
		overlay.setDisplayRange( 0, 255 );
		overlay.setC( 2 );
		overlay.setChannelLut( LUT.createLutFromColor( Color.GREEN ) );
		overlay.setDisplayRange( 0, 255 );

		RGBStackConverter.convertToRGB( overlay );

		return overlay;
	}

	public static CompositeImage createOverlay( Map< String, ImagePlus > colorToImp )
	{
		final CompositeImage overlay = ( CompositeImage ) RGBStackMerge.mergeChannels( colorToImp.values().toArray( new ImagePlus[ colorToImp.size() ] ), true );

		int channelIndex = 1;
		for ( String color : colorToImp.keySet() )
		{
			overlay.setC( channelIndex++ );
			overlay.setChannelLut( LUT.createLutFromColor( getColor( color ) ) );
			if ( ! color.equals( WHITE ) )
			{

				final double min = colorToImp.get( color ).getDisplayRangeMin();
				final double max = colorToImp.get( color ).getDisplayRangeMax();
				overlay.setDisplayRange(
						min,
						min + ( colorToImp.size() - 0 ) * ( max - min ) );
			}
		}

		RGBStackConverter.convertToRGB( overlay );

		return overlay;
	}

	public static Color getColor(String name) {
		try {
			return (Color)Color.class.getField(name.toUpperCase()).get(null);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static List< File > readFileNamesFromDirectoryWithLogging( File inputImagesDirectory )
	{
		final long startMillis = System.currentTimeMillis();
		IJ.log( "Fetching file list. Please wait..." );
		final List< File > fileList = FileUtils.getFileList( inputImagesDirectory, ".*.tiff", true );

//		final String[] fileNames = getValidFileNames( inputImagesDirectory );
		IJ.log( "Fetched file list in " + ( System.currentTimeMillis() - startMillis) + " ms; number of files: " + fileList.size() );

		return fileList;
	}

	public class IntensityRanges
	{
		double minBF;
		double maxBF;
		double minGFP;
		double maxGFP;
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

	public static double getFileSizeKiloBytes( File file )
	{
		return file.length() / 1024.0 ;
	}

	public static ImagePlus extractChannelFromImagePlus( ImagePlus imp, int slice, String title, double min, double max )
	{
		Duplicator duplicator = new Duplicator();
		final ImagePlus impBf = duplicator.run( imp, slice, slice );
		impBf.getProcessor().setMinAndMax( min, max );
		return new ImagePlus( title, impBf.getProcessor().convertToByteProcessor() );
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
}
