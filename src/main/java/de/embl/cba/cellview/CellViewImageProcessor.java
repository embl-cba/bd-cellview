package de.embl.cba.cellview;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.plugin.*;
import ij.plugin.filter.Convolver;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import loci.formats.FormatException;
import loci.plugins.BF;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CellViewImageProcessor
{
	public static final String MERGE = "Merge";

	// Viewing modalities
	public static final String RAW = "Raw";
	public static final String PROCESSED_MERGE = "Processed merge";
	public static final String PROCESSED_MERGE_AND_INDIVIDUAL_CHANNELS = "Processed merge and individual channels";

	public ImagePlus run(
			String filePath,
			ArrayList< CellViewChannel > inputChannels,
			int horizontalCropNumPixels,
			String viewingModality,
			boolean showOnlyMergeInColor )
	{
		// create a copy because the list will be modified
		final ArrayList< CellViewChannel > channels = new ArrayList<>( inputChannels );

		ImagePlus imp = CellViewUtils.tryOpenImage( filePath );

		if ( viewingModality.equals( CellViewImageProcessor.RAW ) ) return imp;

		imp = processImage( imp );

		if ( horizontalCropNumPixels > 0 )
		{
			imp.setRoi( new Rectangle( horizontalCropNumPixels, 0, imp.getWidth() - 2 * horizontalCropNumPixels, imp.getHeight() ) );
			imp = imp.crop( "stack" );
		}

		extractChannelsAsImagePlus( imp, channels );

		addMergeAndConvertImagesToRGB( channels, showOnlyMergeInColor );

		imp = createOutputImp( channels, viewingModality );

		imp.setTitle( new File( filePath ).getName() );

		return imp;
	}

	private static void extractChannelsAsImagePlus(
			ImagePlus inputImp,
			ArrayList< CellViewChannel > channels )
	{
		for ( CellViewChannel channel : channels )
		{
			final ImagePlus imagePlus =
					extractChannelFromImagePlus(
							inputImp,
							channel.sliceIndex,
							channel.color,
							channel.contrastLimits[ 0 ],
							channel.contrastLimits[ 1 ] );

			final Color color = CellViewUtils.getColor( channel.color );
			imagePlus.setLut( LUT.createLutFromColor( color ) );
			channel.imagePlus = imagePlus;
		}
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
					// normalised: 0.0 0.24 0.55 0.24 0.0
		}

		return imp;
	}

	private static void addMergeAndConvertImagesToRGB( ArrayList< CellViewChannel > channels, boolean showOnlyMergeInColor )
	{
		channels.add( createMergedImage( channels ) );

		for ( CellViewChannel channel : channels )
		{
			if ( channel.color == MERGE ) continue;

			channel.imagePlus = toRGB( channel.imagePlus, showOnlyMergeInColor );
		}
	}

	public static ImagePlus toRGB( ImagePlus imp, boolean applyGrayLUT )
	{
		final ImageProcessor processor = imp.getProcessor();

		if ( applyGrayLUT )
			processor.setLut( LUT.createLutFromColor( Color.WHITE ) );


		final ImagePlus rgb = new ImagePlus( "", processor.convertToColorProcessor() );

		return rgb;
	}

	public static ImagePlus createOutputImp( final ArrayList< CellViewChannel > channels, String viewingModality )
	{
		if ( viewingModality.equals( PROCESSED_MERGE ) )
		{
			for ( CellViewChannel channel : channels )
			{
				if ( channel.color.equals( MERGE ) )
					return channel.imagePlus;
			}

			throw new RuntimeException( "MERGE channel not found!" + viewingModality );
		}
		else if ( viewingModality.equals( PROCESSED_MERGE_AND_INDIVIDUAL_CHANNELS ))
		{
			// make montage
			final int width = channels.get( 0 ).imagePlus.getWidth();
			final int height = channels.get( 0 ).imagePlus.getHeight();
			int montageWidth = channels.size() * width;
			int montageHeight = height;

			final ImagePlus montageImp = new ImagePlus( "montage", new ColorProcessor( montageWidth, montageHeight ) );
			final StackInserter inserter = new StackInserter();

			// put overlay first
			for ( CellViewChannel channel : channels )
			{
				if ( channel.color.equals( MERGE ) )
				{
					inserter.insert( channel.imagePlus, montageImp, 0, 0 );
					break;
				}
			}

			// then put the individual images
			for ( int c = 0; c < channels.size(); c++ )
			{
				if ( channels.get( c ).color.equals( MERGE ) ) continue;
				inserter.insert( channels.get( c ).imagePlus, montageImp, (c + 1) * width, 0 );
			}

			return montageImp;
		}
		else
		{
			throw new UnsupportedOperationException( "Viewing modality not supported: " + viewingModality );
		}
	}

	public static CellViewChannel createMergedImage( ArrayList< CellViewChannel > channels )
	{
		final ArrayList< CellViewChannel > mergeChannels = ( ArrayList ) channels.stream().filter( c -> c.color.contains( "*" ) ).collect( Collectors.toList() );

		final ImagePlus[] imagePluses = mergeChannels.stream().map( c -> c.imagePlus ).toArray( ImagePlus[]::new );

		final CompositeImage merge = ( CompositeImage ) RGBStackMerge.mergeChannels( imagePluses, true );

		for ( int c = 0; c < imagePluses.length; c++ )
		{
			merge.setC( c + 1 );
			final CellViewChannel channel = mergeChannels.get( c );
			final Color color = CellViewUtils.getColor( channel.color );
			merge.setChannelLut( LUT.createLutFromColor( color ) );

			if ( ! color.equals( Color.WHITE ) )
			{
				// dim the level of the colored channels, such that they do not just look white when added
				final double min = imagePluses[ c ].getDisplayRangeMin();
				final double max = imagePluses[ c ].getDisplayRangeMax();
				merge.setDisplayRange(
						min,
						min + ( imagePluses.length ) * ( max - min ) );
			}
		}

		// merge.duplicate().show(); // for debugging only
		RGBStackConverter.convertToRGB( merge );

		final CellViewChannel mergedChannel = new CellViewChannel( -1, MERGE, null );
		mergedChannel.imagePlus = merge;
		return mergedChannel;
	}

//	public static List< File > readFileNamesFromDirectoryWithLogging( File inputImagesDirectory )
//	{
//		final long startMillis = System.currentTimeMillis();
//		IJ.log( "Fetching file list. Please wait..." );
//		final List< File > fileList = FileUtils.getFileList( inputImagesDirectory, ".*.tiff", true );
//
////		final String[] fileNames = getValidFileNames( inputImagesDirectory );
//		IJ.log( "Fetched file list in " + ( System.currentTimeMillis() - startMillis) + " ms; number of files: " + fileList.size() );
//
//		return fileList;
//	}

	public static ImagePlus extractChannelFromImagePlus( ImagePlus imp, int slice, String title, double min, double max )
	{
		Duplicator duplicator = new Duplicator();
		final ImagePlus impBf = duplicator.run( imp, slice, slice );
		impBf.getProcessor().setMinAndMax( min, max );
		return new ImagePlus( title, impBf.getProcessor().convertToByteProcessor() );
	}

}
