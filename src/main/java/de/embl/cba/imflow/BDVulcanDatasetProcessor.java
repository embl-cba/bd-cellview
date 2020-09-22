package de.embl.cba.imflow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.embl.cba.imflow.devel.deprecated.BDOpenTableCommandDeprecated;
import de.embl.cba.morphometry.Logger;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import loci.common.DebugTools;
import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
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
import java.util.stream.Collectors;

import static de.embl.cba.imflow.FCCF.checkFileSize;

@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process BD Vulcan Dataset"  )
public class BDVulcanDatasetProcessor implements Command, Interactive
{
	private transient static final String NONE = "None";

	@Parameter
	private transient LogService logService;

	@Parameter ( label = "Dataset Table",  callback = "loadTable", persist = false )
	public File tableFile = new File("Please browse to a dataset table file");

//	@Parameter ( label = "Glimpse Table", callback = "glimpseTable" )
//	public Button glimpseTable;

	//@Parameter ( label = "Image Path Column Name" )

	//@Parameter ( label = "Gate Column Name" )
	public String gateColumnName = "gate";

	@Parameter ( label = "Minimum File Size [kb]")
	public double minimumFileSizeKiloBytes = 10;

	@Parameter ( label = "Maximum File Size [kb]")
	public double maximumFileSizeKiloBytes = 100000;

	@Parameter ( label = "Gray Channel Index", choices = { NONE, "1", "2", "3", "4", "5", "6", "7", "8", "9"} )
	public String whiteIndexString = "1";

	@Parameter ( label = "Minimum Gray Intensity", callback = "showRandomImageFromFilePath" )
	public double minWhite = 0.0;

	@Parameter ( label = "Maximum Gray Intensity", callback = "showRandomImageFromFilePath" )
	public double maxWhite = 1.0;

	@Parameter ( label = "Green Channel Index", choices = { NONE, "1", "2", "3", "4", "5", "6", "7", "8", "9"} )
	public String greenIndexString = "2";

	@Parameter ( label = "Minimum Green Intensity", callback = "showRandomImageFromFilePath"  )
	public double minGreen = 0.08;

	@Parameter ( label = "Maximum Green Intensity", callback = "showRandomImageFromFilePath"  )
	public double maxGreen = 1.0;

	@Parameter ( label = "Magenta Channel Index", choices = { NONE, "1", "2", "3", "4", "5", "6", "7", "8", "9"} )
	public String magentaIndexString = "3";

	@Parameter ( label = "Minimum Magenta Intensity", callback = "showRandomImageFromFilePath"  )
	public double minMagenta = 0.08;

	@Parameter ( label = "Maximum Magenta Intensity", callback = "showRandomImageFromFilePath"  )
	public double maxMagenta = 1.0;

	@Parameter ( label = "Processing Modality", choices = { FCCF.VIEW_RAW, FCCF.VIEW_PROCESSED_OVERLAY, FCCF.VIEW_PROCESSED_OVERLAY_AND_INDIVIDUAL_CHANNELS } )
	public String viewingModality = FCCF.VIEW_PROCESSED_OVERLAY_AND_INDIVIDUAL_CHANNELS;

	@Parameter ( label = "Preview Images from Gate" )
	public String gateChoice = "";

	@Parameter ( label = "Maximum Number of Files to Process [-1 = all]" )
	public int maxNumFiles = -1;

	@Parameter ( label = "Preview Random Image", callback = "showRandomImage" )
	public Button showRandomImage = new MyButton();

	@Parameter ( label = "Process Images", callback = "processImages" )
	public Button processImages = new MyButton();

	@Parameter ( label = "Print Headless Command", callback = "printHeadlessCommand" )
	private transient Button printHeadlessCommand = new MyButton();

	private transient  HashMap< String, ArrayList< Integer > > gateToRows;
	private transient  ImagePlus processedImp;
	private transient  int pathColumnIndex;
	private transient  String experimentDirectory;
	private transient  JTable jTable;
	private transient  String recentImageTablePath = "";
	private transient  File inputImagesDirectory;
	private transient  File outputImagesRootDirectory;
	private transient  String imagePathColumnName = "path";
	private String randomImageFilePath;

	public void run()
	{
		DebugTools.setRootLevel("OFF"); // Bio-Formats
	}

	private void showRandomImage()
	{
		if ( tableFile == null )
		{
			IJ.showMessage( "Please select [ Browse ] a dataset table first." );
			return;
		}

		DebugTools.setRootLevel("OFF"); // Bio-Formats

		randomImageFilePath = getRandomFilePath();
		if ( randomImageFilePath == null ) return;

		if ( ! checkFileSize( randomImageFilePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) ) return;

		showRandomImageFromFilePath();
	}

	private void showRandomImageFromFilePath()
	{
		if ( ! setColorToSliceAndColorToRange() ) return;
		if ( randomImageFilePath == null ) return;
		if ( processedImp != null ) processedImp.close();
		processedImp = createProcessedImagePlus( randomImageFilePath );
		processedImp.show();
	}

	public void processImages()
	{
		new Thread( () ->
		{
			setColorToSliceAndColorToRange();

			if ( jTable == null ) loadTable();

			processAndSaveImages();
		}).start();
	}

	private void printHeadlessCommand()
	{
		Gson gson = new GsonBuilder().create(); //.setPrettyPrinting()
		final String json = gson.toJson( this );
		IJ.log( json );
	}

	private boolean setColorToSliceAndColorToRange()
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
		if ( FCCF.getColorToSlice().size() == 0 )
		{
			IJ.showMessage( "Please set at least two of the Channel Indices (Gray, Green, or Magenta)." );
			return false;
		}
		else
		{
			return true;
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
		inputImagesDirectory = new File( experimentDirectory, "images" );
		pathColumnIndex = jTable.getColumnModel().getColumnIndex( imagePathColumnName );
		gateColumnName = getGateColumnNameDialog();

		glimpseTable( jTable );
		setGates();
		setMaxNumFiles();
	}

	private String getGateColumnNameDialog()
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
		new Thread( () -> {
			Logger.progress( maxNumFiles, i + 1, currentTimeMillis, "Files processed" );
		}).start();

		if ( checkFileSize( inputImagePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) )
		{
			IJ.wait( 500 );
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

		new Thread( () -> {
			IJ.log( "\nSaving processed images to directory: " + outputImagesRootDirectory );
		} ).start();

//		Tables.addColumn( jTable, QC, "Passed" );
//		final int columnIndexQC = jTable.getColumnModel().getColumnIndex( QC );
//
//		Tables.addColumn( jTable, PATH_PROCESSED_JPEG, "None" );
//		final int columnIndexPath = jTable.getColumnModel().getColumnIndex( PATH_PROCESSED_JPEG );

		final long currentTimeMillis = System.currentTimeMillis();

		final int rowMax = getRowMax();

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

	private int getRowMax()
	{
		int rowCount = jTable.getRowCount();

		if ( maxNumFiles == -1 )
			maxNumFiles = rowCount;

		return Math.min( maxNumFiles, rowCount );
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


	public void setGates()
	{
		try
		{
			int gateColumnIndex = jTable.getColumnModel().getColumnIndex( gateColumnName );
			gateToRows = Tables.uniqueColumnEntries( jTable, gateColumnIndex );
			final String gates = gateToRows.keySet().stream().collect( Collectors.joining( "," ) );
			IJ.log( "gates: " + gates );
		}
		catch ( Exception e )
		{
			IJ.showMessage( "Gate column not found in table: " + gateColumnName );
			gateToRows = null;
		}
		//setGatesDropdown();
	}

	private void setGatesDropdown()
	{
//		final MutableModuleItem<String> gateChoiceItem = getInfo().getMutableInput("gateChoice", String.class);
//
//		final ArrayList< String > gates = new ArrayList<>( gateToRows.keySet() );
//		gateChoiceItem.setChoices( gates );
//		final String next = gates.iterator().next();
//		gateChoiceItem.setValue( this, next );
	}

	private void setMaxNumFiles()
	{
//		final MutableModuleItem<Integer> item = //
//				getInfo().getMutableInput("maxNumFiles", Integer.class);
//		item.setDefaultValue( jTable.getRowCount() );
//		item.setValue( this, jTable.getRowCount() );
	}

	private ImagePlus createProcessedImagePlus( String filePath )
	{
		ImagePlus processedImp = FCCF.createProcessedImage( filePath, FCCF.getColorToRange(), FCCF.getColorToSlice(), viewingModality );
		return processedImp;
	}

	private String getRandomFilePath()
	{
		final Random random = new Random();

		int randomRowIndex;
		if ( gateToRows != null )
		{
			final ArrayList< Integer > rowIndicesOfSelectedGate = gateToRows.get( gateChoice );
			if ( rowIndicesOfSelectedGate == null )
			{
				IJ.showMessage( "There are no images of gate " + gateChoice );
				return null;
			}
			randomRowIndex = rowIndicesOfSelectedGate.get( random.nextInt( rowIndicesOfSelectedGate.size() ) );
		}
		else
		{
			randomRowIndex = random.nextInt( jTable.getRowCount() );
		}

		final String absoluteImagePath = getInputImagePath( jTable, randomRowIndex );
		return absoluteImagePath;
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
		final String imagePath = tableFile.getParent() + File.separator + relativeImagePath;
		return imagePath;
	}

	private File saveImageAsJpeg( String outputPath, ImagePlus outputImp )
	{
		outputPath = outputPath.replace( ".tiff", ".jpg" );
		new File( outputPath ).mkdirs();
		new FileSaver( outputImp ).saveAsJpeg( outputPath  );
		return new File( outputPath );
	}
}
