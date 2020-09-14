package de.embl.cba.fccf;

import de.embl.cba.morphometry.Logger;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import loci.common.DebugTools;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static de.embl.cba.fccf.FCCF.checkFileSize;

@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process Dataset"  )
public class BDVulcanDatasetProcessor extends DynamicCommand
{
	public static final String QC = "QC";
	public static final String PATH_PROCESSED_JPEG = "path_processed_jpeg";
	public static final String IMAGES_PROCESSED_JPEG = "images_processed_jpeg";
	public static final String NONE = "None";

	@Parameter
	public LogService logService;

	@Parameter ( label = "Images Table", callback = "loadTable")
	public File tableFile;

	@Parameter ( label = "Glimpse Table", callback = "glimpseTable" )
	public Button glimpseTable;

	//@Parameter ( label = "Image Path Column Name" )
	public String imagePathColumnName = "path";

	@Parameter ( label = "Gate Column Name" )
	public String gateColumnName = "gate";

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

	@Parameter ( label = "Preview Images from Gate", choices = {"None"} )
	public String gateChoice = "";

	@Parameter ( label = "Preview Random Image", callback = "showRandomImage" )
	private Button showRandomImage;

	//@Parameter ( label = "Output Directory" , style = "directory", persist = false)
	public File outputImagesRootDirectory;

	@Parameter ( label = "Maximum Number of Files to Process" )
	private int maxNumFiles;

	private HashMap< String, ArrayList< Integer > > gateToRows;
	private ImagePlus rawImp;
	private ImagePlus processedImp;
	private int gateColumnIndex;
	private int pathColumnIndex;
	private String experimentDirectory;
	private String relativeProcessedImageDirectory;
	private List< File > files;
	private int numFiles;
	private JTable jTable;
	private String recentImageTablePath = "";
	private File inputImagesDirectory;

	public void run()
	{
		DebugTools.setRootLevel("OFF"); // Bio-Formats

		setColorToSliceAndColorToRange();

		if ( jTable == null ) loadTable();

		processAndSaveImages();
	}

	private void setColorToSliceAndColorToRange()
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

	private void loadTable()
	{
		if ( ! tableFile.exists() )
		{
			logService.error( "Table file does not exist: " + tableFile );
			throw new UnsupportedOperationException( "Could not open file: " + tableFile );
		}

		if ( recentImageTablePath.equals( tableFile.getAbsolutePath() ) ) return;

		final long currentTimeMillis = System.currentTimeMillis();
		IJ.log("Loading table; please wait...");
		jTable = Tables.loadTable( tableFile.getAbsolutePath() );
		IJ.log( "Loaded table in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );

		recentImageTablePath = tableFile.getAbsolutePath();
		experimentDirectory = new File( tableFile.getParent() ).getParent();

		setGates();
		setMaxNumFiles();
	}

	private void glimpseTable()
	{
		if ( jTable == null ) loadTable();

		glimpseTable( jTable );
	}

	public static void glimpseTable( JTable jTable )
	{
		IJ.log( "# Table Info"  );
		IJ.log( "Number of rows: " + jTable.getRowCount() );
		final List< String > columnNames = Tables.getColumnNames( jTable );
		for ( String columnName : columnNames )
		{
			final int columnIndex = jTable.getColumnModel().getColumnIndex( columnName );

			String firstRows = "";
			for ( int rowIndex = 0; rowIndex < 5; rowIndex++ )
			{
				firstRows += jTable.getValueAt( rowIndex, columnIndex );
				firstRows += ", ";
			}
			firstRows += "...";

			IJ.log( columnName + ": " + firstRows );
		}
	}

	private String saveProcessedImage( long currentTimeMillis, int i, String inputImagePath ) throws IOException
	{
		Logger.progress( maxNumFiles, i + 1, currentTimeMillis, "Files processed" );

		if ( checkFileSize( inputImagePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) )
		{
			processedImp = createProcessedImagePlus( inputImagePath );

			// Images can be in subfolders, thus we only
			// replace the root path
			final String absoluteInputRootDirectory = inputImagesDirectory.getCanonicalPath();
			final String absoluteInputPath = new File( inputImagePath ).getCanonicalPath();
			final String absoluteOutputRootDirectory = outputImagesRootDirectory.getCanonicalPath();
			String outputImagePath = absoluteInputPath.replace(
					absoluteInputRootDirectory,
					absoluteOutputRootDirectory );
			saveImageAsJpeg( outputImagePath, processedImp );

			return outputImagePath;
		}
		else
		{
			return null;
		}
	}

	public void processAndSaveImages()
	{
		String relativeImageRootDirectory = "images-processed-" + Utils.getLocalDateAndHourAndMinute();
		outputImagesRootDirectory = new File( experimentDirectory, relativeImageRootDirectory );

		IJ.log( "\nSaving processed images to directory: " + outputImagesRootDirectory );

//		Tables.addColumn( jTable, QC, "Passed" );
//		final int columnIndexQC = jTable.getColumnModel().getColumnIndex( QC );
//
//		Tables.addColumn( jTable, PATH_PROCESSED_JPEG, "None" );
//		final int columnIndexPath = jTable.getColumnModel().getColumnIndex( PATH_PROCESSED_JPEG );

		final long currentTimeMillis = System.currentTimeMillis();

		int rowCount = jTable.getRowCount();

		final int rowMax = Math.min( maxNumFiles, rowCount );

		for ( int rowIndex = 0; rowIndex < rowMax; rowIndex++ )
		{
			final String inputImagePath = getInputImagePath( jTable, rowIndex );

			String outputImagePath = null;
			try
			{
				outputImagePath = saveProcessedImage( currentTimeMillis, rowIndex, inputImagePath );
			} catch ( IOException e )
			{
				e.printStackTrace();
				throw new RuntimeException( "Error during saving of image: " + inputImagePath );
			}

//			if ( outputImagePath != null )
//			{
//				final String pathRelativeToTable = outputImagePath.replace( experimentDirectory, ".." );
//				jTable.setValueAt( pathRelativeToTable , rowIndex, columnIndexPath );
//			}
//			else
//			{
//				jTable.setValueAt( "Failed_FileSizeTooSmall", rowIndex, columnIndexQC );
//			}
		}

//		saveTableWithAdditionalColumns();
	}

	public void saveTableWithAdditionalColumns()
	{
		IJ.log( "\nSaving table with additional columns..." );
		final File tableOutputFile = new File( tableFile.getAbsolutePath().replace( ".csv", "-processed-images.csv" ) );
		Tables.saveTable( jTable, tableOutputFile );
		IJ.log( "...done: " + tableOutputFile );
		IJ.log( " " );
		BDOpenTableCommandDeprecated.glimpseTable( jTable );
	}

//	@Override
//	public void initialize()
//	{
//		getInfo(); // HACK: Workaround for bug in SJC.
//
//		if ( jTable != null )
//		{
//			setGates();
//			initProcessedJpegImagesOutputDirectory();
//			pathColumnIndex = jTable.getColumnModel().getColumnIndex( imagePathColumnName );
//		}
//		else
//		{
//			setNoGateChoice();
//			fetchFiles();
//			maxNumFiles = files.size();
//		}
//	}

	public void initProcessedJpegImagesOutputDirectory()
	{
		final MutableModuleItem<File> mutableInput = getInfo().getMutableInput("outputImagesDirectory", File.class);

		experimentDirectory = new File( tableFile.getParent() ).getParent();
		inputImagesDirectory = new File( experimentDirectory, "images" );
		relativeProcessedImageDirectory = "../" + IMAGES_PROCESSED_JPEG;
		final File defaultValue = new File( experimentDirectory + File.separator +
				IMAGES_PROCESSED_JPEG );
		mutableInput.setValue( this, defaultValue );
		mutableInput.setDefaultValue( defaultValue );
		mutableInput.setVisibility( ItemVisibility.MESSAGE );
	}

	public void setGates()
	{
		gateColumnIndex = jTable.getColumnModel().getColumnIndex( gateColumnName );
		gateToRows = Tables.uniqueColumnEntries( jTable, gateColumnIndex );

		final MutableModuleItem<String> gateChoiceItem = getInfo().getMutableInput("gateChoice", String.class);

		final ArrayList< String > gates = new ArrayList<>( gateToRows.keySet() );
		gateChoiceItem.setChoices( gates );
		final String next = gates.iterator().next();
		gateChoiceItem.setValue( this, next );
	}

	public void setMaxNumFiles()
	{
		final MutableModuleItem<Integer> item = //
				getInfo().getMutableInput("maxNumFiles", Integer.class);
		item.setDefaultValue( jTable.getRowCount() );
		item.setValue( this, jTable.getRowCount() );
	}

	private void showRandomImage()
	{
		DebugTools.setRootLevel("OFF"); // Bio-Formats

		setColorToSliceAndColorToRange();

		if ( processedImp != null ) processedImp.close();

		final String filePath = getRandomFilePath();

		if ( ! checkFileSize( filePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) ) return;

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

	private String getInputImagePath( int i )
	{
		return files.get( i ).getAbsolutePath();
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
