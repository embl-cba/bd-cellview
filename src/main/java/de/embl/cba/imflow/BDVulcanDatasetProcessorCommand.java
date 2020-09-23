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
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.embl.cba.imflow.FCCF.checkFileSize;
import static org.scijava.ItemVisibility.MESSAGE;

@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process BD Vulcan Dataset"  )
public class BDVulcanDatasetProcessorCommand implements Command, Interactive
{
	private transient static final String NONE = "None";

	@Parameter
	private transient LogService logService;

	@Parameter ( label = "Dataset Tables" )
	public File[] tableFiles;// = new File("Please browse to a dataset table file");


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

	@Parameter ( label = "Maximum Number of Files to Process [-1 = all]" )
	public int maxNumFiles = -1;

	@Parameter ( visibility = MESSAGE )
	private transient String preview = "--- Preview ---";

	@Parameter ( label = "Select Table", callback = "selectTable" )
	public transient Button selectTableButton;

//	@Parameter ( label = "Select Gate Column", callback = "selectGateColumn" )
//	public transient Button selectGateColumnButton;

	@Parameter ( label = "Select Gate", callback = "selectGate" )
	public transient Button selectGateButton;

	@Parameter ( label = "Preview Image", callback = "showRandomImage" )
	private transient Button previewImageButton = new MyButton();

	@Parameter ( visibility = MESSAGE )
	private transient String process = "--- Process Images from Selected Gate from All Tables ---";

	@Parameter ( label = "Process on Local Computer", callback = "processImages" )
	private transient Button processImagesButton = new MyButton();

	@Parameter ( label = "Process on Computer Cluster", callback = "processImagesOnCluster" )
	private transient Button processImagesOnClusterButton = new MyButton();

//	@Parameter ( label = "Save Settings for Running Headless", callback = "saveHeadlessCommand" )
//	private transient Button saveHeadlessCommand = new MyButton();

	public String experimentDirectory;
	public File selectedTableFile;
	public String gateColumnName = "gate";
	private transient String selectedGate;

	private transient HashMap< String, ArrayList< Integer > > gateToRows;
	private transient ImagePlus processedImp;
	private transient int pathColumnIndex;
	private transient JTable jTable;
	private transient String recentImageTablePath = "";
	private transient File inputImagesDirectory;
	private transient File outputImagesRootDirectory;
	private transient String imagePathColumnName = "path";
	private transient String randomImageFilePath;
	private transient boolean batchMode = true;

	public void run()
	{
		batchMode = false;
		DebugTools.setRootLevel("OFF"); // Bio-Formats
	}

	private void showRandomImage()
	{
		if ( tableFiles == null || tableFiles.length == 0 )
		{
			IJ.showMessage( "Please select some tables first!" );
		}

		if ( selectedTableFile == null )
		{
			selectTable();
		}

		if ( jTable == null )
		{
			loadTable( false );
		}

		DebugTools.setRootLevel("OFF"); // Bio-Formats

		randomImageFilePath = getRandomFilePath();
		if ( randomImageFilePath == null ) return;

		if ( ! checkFileSize( randomImageFilePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) ) return;

		showRandomImageFromFilePath();
	}

	private void showRandomImageFromFilePath()
	{
		IJ.log( "Preview image. Table: " + selectedTableFile + "; Gate: " + selectedGate );
		if ( ! setColorToSliceAndColorToRange() ) return;
		if ( randomImageFilePath == null ) return;
		if ( processedImp != null ) processedImp.close();
		processedImp = createProcessedImagePlus( randomImageFilePath );
		processedImp.show();
	}

	private void processImagesOnCluster()
	{
		new Thread( () ->
		{
			IJ.showMessage( "Cluster processing is not yet implemented." );
		});
	}

	private void processImages()
	{
		// the new thread is necessary for the progress logging in the IJ.log window
		new Thread( () ->
		{
			if ( selectedGate == null || ! gateToRows.keySet().contains( selectedGate ) )
				selectGate();

			for ( File file : tableFiles )
			{
				selectedTableFile = file;
				processImagesFromSelectedTableFile();
			}
		}).start();
	}

	// this public function is only for the headless (cluster) batch processing
	public void headlessProcessImagesFromSelectedTableFile()
	{
		// the new thread is necessary for the progress logging in the IJ.log window
		new Thread( () ->
		{
			batchMode = true;
			processImagesFromSelectedTableFile();
		}).start();
	}

	private void processImagesFromSelectedTableFile()
	{
		setColorToSliceAndColorToRange();
		if ( jTable == null ) loadTable( true );
		IJ.log( "Processing table: " + selectedTableFile );
		processAndSaveImages();
	}

	private void selectTable()
	{
		final GenericDialog gd = new GenericDialog( "Please select table for image preview" );
		final String[] tablePaths = Arrays.stream( tableFiles ).map( x -> x.toString() ).toArray( String[]::new );
		gd.addChoice( "Table", tablePaths, tablePaths[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;
		selectedTableFile = new File( gd.getNextChoice() );
		IJ.log( "Selected Table: " + selectedTableFile );
		loadTable( false );
	}

	private void selectGate()
	{
		if ( jTable == null )
			selectTable();

		final GenericDialog gd = new GenericDialog( "Please select gate for image preview" );
		final String[] choices = gateToRows.keySet().stream().toArray( String[]::new );
		gd.addChoice( "Gate", choices, choices[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;
		selectedGate = gd.getNextChoice();
		IJ.log( "Selected Gate: " + selectedGate );
	}

	private void saveHeadlessCommand() throws IOException
	{
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String json = gson.toJson( this );

		final File file = new File( experimentDirectory, "batchProcess.json" );
		final FileWriter writer = new FileWriter( file ) ;
		writer.write( json );
		writer.close();

		IJ.log( "Settings: " + json );
		IJ.log( "Wrote settings to file: " + file.getAbsolutePath() );
		IJ.log( "Please run like below (replacing the path to the Fiji executable)" );
		IJ.log( "/Users/tischer/Desktop/Fiji-imflow.app/Contents/MacOS/ImageJ-macosx --headless --run \"Batch Process BD Vulcan Dataset\" \"settingsFile='"+ file.getAbsolutePath() +"'\"");
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

	private void loadTable( boolean batchMode )
	{
		if ( ! selectedTableFile.exists() )
		{
			logService.error( "Table file does not exist: " + selectedTableFile );
			throw new UnsupportedOperationException( "Could not open file: " + selectedTableFile );
		}

		// final String absolutePath = tableFile.getAbsolutePath(); this does not work when loading from json for some reason...
		final String absolutePath = selectedTableFile.toString();

		if ( recentImageTablePath.equals( absolutePath ) ) return;

		final long currentTimeMillis = System.currentTimeMillis();
		IJ.log("Loading table; please wait...");
		jTable = Tables.loadTable( absolutePath );
		IJ.log( "Loaded table in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );

		recentImageTablePath = absolutePath;
		experimentDirectory = new File( selectedTableFile.getParent() ).getParent();
		inputImagesDirectory = new File( experimentDirectory, "images" );
		pathColumnIndex = jTable.getColumnModel().getColumnIndex( imagePathColumnName );

		if ( ! batchMode )
		{
			gateColumnName = getGateColumnNameDialog();
			setGates();
		}

		glimpseTable( jTable );

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

	private String saveProcessedImage( int maxNumItems, long currentTimeMillis, int i, String inputImagePath ) throws IOException
	{
		new Thread( () -> {
			if ( (i+1) % 100 == 0 || i + 1 == 1 || i + 1 == maxNumItems )
				Logger.progress( maxNumItems, i + 1, currentTimeMillis, "Files processed" );
		}).start();

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

	private void processAndSaveImages()
	{

//		Tables.addColumn( jTable, QC, "Passed" );
//		final int columnIndexQC = jTable.getColumnModel().getColumnIndex( QC );
//
//		Tables.addColumn( jTable, PATH_PROCESSED_JPEG, "None" );
//		final int columnIndexPath = jTable.getColumnModel().getColumnIndex( PATH_PROCESSED_JPEG );

		final long currentTimeMillis = System.currentTimeMillis();

		final ArrayList< Integer > rowIndicesOfSelectedGate = gateToRows.get( selectedGate );
		IJ.log( "Processing images from gate: " + selectedGate  );
		IJ.log( "Number of images of above gate in above table: " + rowIndicesOfSelectedGate.size() );

		final int maxNumItems = getMaxNumItems( rowIndicesOfSelectedGate.size() );
		IJ.log( "Number of images to be processed: " + maxNumItems );

		String relativeImageRootDirectory = "images-processed-" + Utils.getLocalDateAndHourAndMinute();
		outputImagesRootDirectory = new File( experimentDirectory, relativeImageRootDirectory );
		IJ.log( "Saving processed images to directory: " + outputImagesRootDirectory );

		for ( int i = 0; i < maxNumItems; i++ )
		{
			int rowIndex = rowIndicesOfSelectedGate.get( i );

			final String inputImagePath = getInputImagePath( jTable, rowIndex );

			try
			{
				String outputImagePath = saveProcessedImage( maxNumItems, currentTimeMillis, rowIndex, inputImagePath );
			}
			catch ( IOException e )
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

	private int getMaxNumItems( int size )
	{
		if ( maxNumFiles == -1 )
			return size;
		else
			return maxNumFiles;
	}

	public void saveTableWithAdditionalColumns()
	{
		IJ.log( "\nSaving table with additional columns..." );
		final File tableOutputFile = new File( selectedTableFile.getAbsolutePath().replace( ".csv", "-processed-images.csv" ) );
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
			if ( selectedGate == null || ! gateToRows.keySet().contains( selectedGate ) )
				selectGate();

			final ArrayList< Integer > rowIndicesOfSelectedGate = gateToRows.get( selectedGate );
			if ( rowIndicesOfSelectedGate == null )
			{
				IJ.showMessage( "There are no images of gate " + selectedGate );
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
		final String imagePath = selectedTableFile.getParent() + File.separator + relativeImagePath;
		return imagePath;
	}

	private File saveImageAsJpeg( String outputPath, ImagePlus outputImp )
	{
		outputPath = outputPath.replace( ".tiff", ".jpg" );
		new File( outputPath ).mkdirs();
		new FileSaver( outputImp ).saveAsJpeg( outputPath  );
		return new File( outputPath );
	}

	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		imageJ.command().run( BDVulcanDatasetProcessorCommand.class, true );
	}
}
