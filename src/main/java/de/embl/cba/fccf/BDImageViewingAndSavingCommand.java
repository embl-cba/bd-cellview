package de.embl.cba.fccf;

import de.embl.cba.morphometry.Logger;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import loci.common.DebugTools;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static de.embl.cba.fccf.FCCF.checkFile;

@Plugin( type = Command.class )
public class BDImageViewingAndSavingCommand extends DynamicCommand implements Initializable
{
	public static final String QC = "QC";
	public static final String PATH_PROCESSED_JPEG = "path_processed_jpeg";
	public static final String IMAGES_PROCESSED_JPEG = "images_processed_jpeg";
	public static final String NONE = "None";
	public static File inputImagesDirectory;
	public static JTable jTable;
	public static File tableFile;
	public static String imagePathColumnName;
	public static String gateColumnName;

	private List< File > files;
	private int numFiles;

	@Parameter
	public LogService logService;

	@Parameter ( label = "Minimum File Size [kb]")
	public double minimumFileSizeKiloBytes = 10;

	@Parameter ( label = "Maximum File Size [kb]")
	public double maximumFileSizeKiloBytes = 100000;

	@Parameter ( label = "Gray Channel Index", choices = { NONE, "1", "2", "3", "4", "5", "6", "7", "8", "9"} )
	public String whiteIndexString;

	@Parameter ( label = "Minimum Gray Intensity" )
	public double minWhite = 0.0;

	@Parameter ( label = "Maximum Gray Intensity" )
	public double maxWhite = 1.0;

	@Parameter ( label = "Green Channel Index", choices = { NONE, "1", "2", "3", "4", "5", "6", "7", "8", "9"} )
	public String greenIndexString;

	@Parameter ( label = "Minimum Green Intensity" )
	public double minGreen = 0.08;

	@Parameter ( label = "Maximum Green Intensity" )
	public double maxGreen = 1.0;

	@Parameter ( label = "Magenta Channel Index", choices = { NONE, "1", "2", "3", "4", "5", "6", "7", "8", "9"} )
	public String magentaIndexString;

	@Parameter ( label = "Minimum Magenta Intensity" )
	public double minMagenta = 0.08;

	@Parameter ( label = "Maximum Magenta Intensity" )
	public double maxMagenta = 1.0;

	@Parameter ( label = "Processing Modality", choices = { FCCF.VIEW_RAW, FCCF.VIEW_PROCESSED_OVERLAY, FCCF.VIEW_PROCESSED_OVERLAY_AND_INDIVIDUAL_CHANNELS } )
	public String viewingModality = FCCF.VIEW_PROCESSED_OVERLAY_AND_INDIVIDUAL_CHANNELS;

	@Parameter ( label = "Preview Images from Gate", choices = {} )
	public String gateChoice = "";

	@Parameter ( label = "Preview Random Image", callback = "showRandomImage" )
	private Button showRandomImage;

	@Parameter ( label = "Output Directory" , style = "directory", persist = false)
	public File outputDirectory;

	@Parameter ( label = "Maximum Number of Files to Process" )
	private int maxNumFiles;

	private HashMap< String, ArrayList< Integer > > gateToRows;
	private ImagePlus rawImp;
	private ImagePlus processedImp;
	private int gateColumnIndex;
	private int pathColumnIndex;
	private static String experimentDirectory;
	private static String relativeProcessedImageDirectory;

	public void run()
	{
		DebugTools.setRootLevel("OFF"); // Bio-Formats

		setColorToSliceAndColorToRange();

		final HashMap< String, Integer > colorToSlice = FCCF.getColorToSlice();

		IJ.log( "\nSaving processed images: " );

		if ( jTable != null )
		{
			saveProcessedImagesAndTableWithQC();
		}
		else if ( inputImagesDirectory != null )
		{
			saveProcessedImages();
		}
	}

	public void setColorToSliceAndColorToRange()
	{
		final HashMap< String, Integer > colorToSlice = FCCF.getColorToSlice();
		colorToSlice.clear();
		final HashMap< String, double[] > colorToRange = FCCF.getColorToRange();
		colorToRange.clear();

		if ( ! whiteIndexString.equals( NONE ) )
		{
			colorToSlice.put( FCCF.WHITE, Integer.parseInt( whiteIndexString ) );
			colorToRange.put( FCCF.WHITE, new double[]{ minWhite, maxWhite} );
		}

		if ( ! greenIndexString.equals( NONE ) )
		{
			colorToSlice.put( FCCF.GREEN, Integer.parseInt( greenIndexString ) );
			colorToRange.put( FCCF.GREEN, new double[]{ minGreen, maxGreen} );
		}

		if ( ! magentaIndexString.equals( NONE ) )
		{
			colorToSlice.put( FCCF.MAGENTA, Integer.parseInt( magentaIndexString ) );
			colorToRange.put( FCCF.MAGENTA, new double[]{ minMagenta, maxMagenta} );
		}
	}

	/**
	 * This is for the folder-based parsing.
	 *
	 */
	public void saveProcessedImages()
	{
		final long currentTimeMillis = System.currentTimeMillis();

		for ( int i = 0; i < maxNumFiles; i++ )
		{
			String inputImagePath = getInputImagePath( i );
			if ( checkFile( inputImagePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) )
			{
				processedImp = createProcessedImagePlus( inputImagePath );
				String outputImagePath = inputImagePath.replace(
						inputImagesDirectory.getAbsolutePath(),
						outputDirectory.getAbsolutePath() );
				saveImageAsJpeg( outputImagePath, processedImp );
			}
			Logger.progress( maxNumFiles, i + 1, currentTimeMillis, "Files saved" );
		}
	}

	/**
	 * This is for the table-based parsing.
	 *
	 */
	public void saveProcessedImagesAndTableWithQC()
	{
		Tables.addColumn( jTable, QC, "Passed" );
		final int columnIndexQC = jTable.getColumnModel().getColumnIndex( QC );

		Tables.addColumn( jTable, PATH_PROCESSED_JPEG, "None" );
		final int columnIndexPath = jTable.getColumnModel().getColumnIndex( PATH_PROCESSED_JPEG );

		final long currentTimeMillis = System.currentTimeMillis();

		int rowCount = jTable.getRowCount();

		final int rowMax = Math.min( maxNumFiles, rowCount );

		for ( int rowIndex = 0; rowIndex < rowMax; rowIndex++ )
		{
			final String inputImagePath = getInputImagePath( jTable, rowIndex );

			if ( ! checkFile( inputImagePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) )
			{
				jTable.setValueAt( "Failed_FileSizeTooSmall", rowIndex, columnIndexQC );
				continue;
			}

			processedImp = createProcessedImagePlus( inputImagePath );

			final File path = saveImageAsJpeg( new File( inputImagePath ).getName(), processedImp );

			jTable.setValueAt( relativeProcessedImageDirectory + File.separator + path.getName(), rowIndex, columnIndexPath );

			Logger.progress( rowCount, rowIndex + 1, currentTimeMillis, "Files saved" );
		}

		saveTableWithAdditionalColumns();
	}

	public void saveTableWithAdditionalColumns()
	{
		IJ.log( "\nSaving table with additional columns..." );
		final File tableOutputFile = new File( tableFile.getAbsolutePath().replace( ".csv", "-processed-images.csv" ) );
		Tables.saveTable( jTable, tableOutputFile );
		IJ.log( "...done: " + tableOutputFile );
		IJ.log( " " );
		BDOpenTableCommand.glimpseTable( jTable );
	}

	@Override
	public void initialize()
	{
		getInfo(); // HACK: Workaround for bug in SJC.

		if ( jTable != null )
		{
			setGates();
			setProcessedJpegImagesOutputDirectory();
			pathColumnIndex = jTable.getColumnModel().getColumnIndex( imagePathColumnName );
		}
		else
		{
			setNoGateChoice();
			fetchFiles();
			maxNumFiles = files.size();
		}
	}

	public void setProcessedJpegImagesOutputDirectory()
	{
		final MutableModuleItem<File> mutableInput = //
				getInfo().getMutableInput("outputDirectory", File.class);

		experimentDirectory = new File( tableFile.getParent() ).getParent();
		relativeProcessedImageDirectory = "../" + IMAGES_PROCESSED_JPEG;
		final File defaultValue = new File( experimentDirectory + File.separator +
				IMAGES_PROCESSED_JPEG );
		mutableInput.setValue( this, defaultValue );
		mutableInput.setDefaultValue( defaultValue );
	}

	public void setGates()
	{
		gateColumnIndex = jTable.getColumnModel().getColumnIndex( gateColumnName );
		gateToRows = Tables.uniqueColumnEntries( jTable, gateColumnIndex );

		final MutableModuleItem<String> gateChoiceItem = //
				getInfo().getMutableInput("gateChoice", String.class);

		gateChoiceItem.setChoices( new ArrayList<>( gateToRows.keySet() ) );
	}

	public void setNoGateChoice()
	{
		final MutableModuleItem<String> gateChoiceItem = //
				getInfo().getMutableInput("gateChoice", String.class);

		final ArrayList< String> choices = new ArrayList<>( );
		choices.add( "Option not available for folder based browsing" );
		gateChoiceItem.setChoices( choices );
	}

	public void setMaxNumFiles()
	{
		final MutableModuleItem<Integer> item = //
				getInfo().getMutableInput("maxNumFiles", Integer.class);
		item.setDefaultValue( files.size() );
	}

	private void showRandomImage()
	{
		DebugTools.setRootLevel("OFF"); // Bio-Formats

		setColorToSliceAndColorToRange();

		if ( processedImp != null ) processedImp.close();

		final String filePath = getRandomFilePath();

		if ( ! checkFile( filePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) ) return;

		processedImp = createProcessedImagePlus( filePath );

		processedImp.show();
	}

	private ImagePlus createProcessedImagePlus( String filePath )
	{
		ImagePlus processedImp = FCCF.createProcessedImage(
				filePath, FCCF.getColorToRange(), FCCF.getColorToSlice(), viewingModality );
		return processedImp;
	}


	private String getRandomFilePath()
	{
		if ( jTable != null )
		{
			final Random random = new Random();
			final ArrayList< Integer > rowIndices = gateToRows.get( gateChoice );
			final Integer rowIndex = rowIndices.get( random.nextInt( rowIndices.size() ) );
			final String absoluteImagePath = getInputImagePath( jTable, rowIndex );
			return absoluteImagePath;
		}
		else if ( inputImagesDirectory != null )
		{
			fetchFiles();
			final Random random = new Random();
			final int randomInteger = random.nextInt( files.size() );
			final String absoluteImagePath = getInputImagePath( randomInteger );
			return absoluteImagePath;
		}
		else
		{
			return null;
		}
	}

	private String getInputImagePath( int randomInteger )
	{
		return files.get( randomInteger ).getAbsolutePath();
	}


	private void fetchFiles()
	{
		if ( files != null ) return;
		files = FCCF.readFileNamesFromDirectoryWithLogging( inputImagesDirectory );
	}

	/**
	 *
	 * @param jTable
	 * @param rowIndex
	 * @return
	 */
	private String getInputImagePath( JTable jTable, Integer rowIndex )
	{
		final String relativeImagePath = ( String ) jTable.getValueAt( rowIndex, pathColumnIndex );
		return tableFile.getParent() + File.separator + relativeImagePath;
	}

	private File saveImageAsJpeg( String outputPath, ImagePlus outputImp )
	{
		outputPath = outputPath.replace( ".tiff", ".jpg" );
		new File( outputPath ).mkdirs();
		new FileSaver( outputImp ).saveAsJpeg( outputPath  );
		return new File( outputPath );
	}

}
