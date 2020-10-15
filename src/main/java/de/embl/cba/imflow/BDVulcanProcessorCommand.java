package de.embl.cba.imflow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.embl.cba.cluster.Commands;
import de.embl.cba.cluster.PathMapper;
import de.embl.cba.morphometry.Logger;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static de.embl.cba.imflow.FCCF.checkFileSize;
import static org.scijava.ItemVisibility.MESSAGE;

@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process BD Vulcan Images"  )
public class BDVulcanProcessorCommand implements Command, Interactive
{
	private transient static final String NONE = "None";

	@Parameter
	private transient PluginService pluginService;

	@Parameter ( label = "Dataset Tables", persist = true )
	public transient File[] tableFiles;

	@Parameter ( label = "Minimum File Size [kb]" )
	public double minimumFileSizeKiloBytes = 10;

	@Parameter ( label = "Maximum File Size [kb]" )
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

	@Parameter ( label = "Select Table", callback = "selectTableDialog" )
	public transient Button selectTableButton;

//	@Parameter ( label = "Select Gate Column", callback = "selectGateColumn" )
//	public transient Button selectGateColumnButton;

	@Parameter ( label = "Select Gate", callback = "selectGateDialog" )
	public transient Button selectGateButton;

	@Parameter ( label = "Preview Image", callback = "showRandomImage" )
	private transient Button previewImageButton = new MyButton();

	@Parameter ( visibility = MESSAGE )
	private transient String process = "--- Process Images from Selected Gate from All Tables ---";

	@Parameter ( label = "Process on Local Computer", callback = "processImages" )
	private transient Button processImagesButton = new MyButton();

//	@Parameter ( label = "Process on Computer Cluster", callback = "processImagesOnCluster" )
//	private transient Button processImagesOnClusterButton = new MyButton();

	@Parameter ( label = "Save Settings for Headless Processing", callback = "saveSettings" )
	private transient Button saveSettingsCommand = new MyButton();

	public String experimentDirectory;
	public File selectedTableFile;
	public String gateColumnName = "gate";
	public boolean quitAfterRun = false;
	public String selectedGate;
	public int numImagesToBeProcessed;


	private transient HashMap< String, ArrayList< Integer > > gateToRows;
	private transient ImagePlus processedImp;
	private transient int pathColumnIndex;
	private transient JTable jTable;
	private transient File inputImagesDirectory;
	private transient File outputImagesRootDirectory;
	private transient String imagePathColumnName = "path";
	private transient String randomImageFilePath;
	private transient boolean batchMode = true;
	private transient ArrayList< Integer > selectedGateIndices;

	public static BDVulcanProcessorCommand createBdVulcanProcessorCommandFromJson( File jsonFile ) throws IOException
	{
		InputStream inputStream = FileAndUrlUtils.getInputStream( jsonFile.getAbsolutePath() );
		final JsonReader reader = new JsonReader( new InputStreamReader( inputStream, "UTF-8" ) );
		DebugTools.setRootLevel("OFF"); // Bio-Formats
		Gson gson = new Gson();
		Type type = new TypeToken< BDVulcanProcessorCommand >() {}.getType();
		return gson.fromJson( reader, type );
	}

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
			selectTableDialog();
		}

		if ( jTable == null )
		{
			loadTable();
			gateColumnName = selectGateColumnDialog();
			setGatesIndices();
			selectedGate = selectGateDialog();
			setSelectedGateIndices();
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
				selectedGate = selectGateDialog();

			for ( File file : tableFiles )
			{
				selectedTableFile = file;
				processImagesFromSelectedTableFile();
			}
		}).start();
	}

	// this public function is only for the headless (cluster) batch processing
	public void processImagesHeadless()
	{
		new Thread( () ->
		{
			batchMode = true;
			quitAfterRun = true; // this is also in the settings file, thus could be removed here
			processImagesFromSelectedTableFile();
			if ( quitAfterRun ) Commands.quitImageJ(); // otherwise fiji does not quit when running headless
		}).start();
	}

	private void processImagesFromSelectedTableFile()
	{
		IJ.log( "Processing table: " + selectedTableFile );
		loadTable();
		setColorToSliceAndColorToRange();
		setGatesIndices();
		setSelectedGateIndices();
		setNumImagesToBeProcessed();
		processAndSaveImages();
	}

	private void selectTableDialog()
	{
		final GenericDialog gd = new GenericDialog( "Please select table for image preview" );
		final String[] tablePaths = Arrays.stream( tableFiles ).map( x -> x.toString() ).toArray( String[]::new );
		gd.addChoice( "Table", tablePaths, tablePaths[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;
		selectedTableFile = new File( gd.getNextChoice() );
		IJ.log( "Selected Table: " + selectedTableFile );
		loadTable();
	}

	private String selectGateDialog()
	{
		if ( jTable == null )
		{
			selectedTableFile = tableFiles[ 0 ];
			loadTable();
			setGatesIndices();
		}

		final GenericDialog gd = new GenericDialog( "Please select gate for image preview" );
		final String[] choices = gateToRows.keySet().stream().toArray( String[]::new );

		gd.addChoice( "Gate", choices, selectedGate );
		gd.showDialog();

		if ( gd.wasCanceled() )
			return selectedGate;

		String selectedGate = gd.getNextChoice();
		setSelectedGateIndices();

		return selectedGate;
	}


	/**
	 * Save a settings file that can be used to run the processing in headless mode,
	 * for example on a computer cluster.
	 *
	 * The settings file can be executed by the {@code BDVulcanHeadlessProcessorCommand}
	 *
	 * @throws IOException
	 */
	private void saveSettings()
	{
		new Thread( () -> {

			if ( jTable == null )
			{
				selectedTableFile = tableFiles[ 0 ];
				loadTable();
			}

			if ( selectedGate == null || !gateToRows.keySet().contains( selectedGate ) )
				selectedGate = selectGateDialog();

			// remember fields
			File selectedTableFile = this.selectedTableFile;
			String experimentDirectory = this.experimentDirectory;
			quitAfterRun = true; // important for running headless on cluster, otherwise the job does not end!
			int numImagesToBeProcessed = this.numImagesToBeProcessed;

			for ( File file : tableFiles )
			{
				try
				{
					createSettingsJsonFile( file );
				} catch ( IOException e )
				{
					e.printStackTrace();
				}
			}

			// reset fields
			quitAfterRun = false;
			this.selectedTableFile = selectedTableFile;
			this.experimentDirectory = experimentDirectory;
			this.numImagesToBeProcessed = numImagesToBeProcessed;

			//		final PluginInfo saveInputsPreprocessorInfo =
			//				pluginService.getPlugin( SaveInputsPreprocessor.class, PreprocessorPlugin.class );
			//
			//		final PreprocessorPlugin saveInputsPreprocessor =
			//				pluginService.createInstance( saveInputsPreprocessorInfo );
			//
			//		saveInputsPreprocessor.process(this);

			Logger.log( "Done saving settings!" );

		}).start();
	}

	private void createSettingsJsonFile( File tableFile ) throws IOException
	{
		loadTable();
		setGatesIndices();
		setSelectedGateIndices();
		setNumImagesToBeProcessed();

		final File settingsFile = new File( experimentDirectory, "batchProcess.json" );

		// adapt fields for the specific table file
		this.selectedTableFile = new File( PathMapper.asEMBLClusterMounted( tableFile ) );
		this.experimentDirectory = PathMapper.asEMBLClusterMounted( experimentDirectory );

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String json = gson.toJson( this );
		final FileWriter writer = new FileWriter( settingsFile ) ;
		writer.write( json );
		writer.close();

		IJ.log( "Settings: " + json );
		IJ.log( "Wrote settings to file:\n" + settingsFile.getAbsolutePath() );
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

	public void loadTable()
	{
		if ( ! selectedTableFile.exists()  )
		{
			Logger.log( "Table file does not exist: " + selectedTableFile );
			throw new UnsupportedOperationException( "Could not open file: " + selectedTableFile );
		}

		// final String absolutePath = tableFile.getAbsolutePath(); this does not work when loading from json for some reason...
		final String absolutePath = selectedTableFile.toString();

		loadTable( absolutePath );
		experimentDirectory = new File( selectedTableFile.getParent() ).getParent();
		inputImagesDirectory = new File( experimentDirectory, "images" );
		pathColumnIndex = jTable.getColumnModel().getColumnIndex( imagePathColumnName );

		glimpseTable( jTable );
	}

	private void loadTable( String absolutePath )
	{
		final long currentTimeMillis = System.currentTimeMillis();
		IJ.log("Loading table; please wait...");
		jTable = Tables.loadTable( absolutePath );
		IJ.log( "Loaded table in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );
	}

	private String selectGateColumnDialog()
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

	private String saveProcessedImage( int maxNumItems, long currentTimeMillis, int i, String inputPath ) throws IOException
	{
		new Thread( () -> {
			if ( (i+1) % 100 == 0 || i + 1 == 1 || i + 1 == maxNumItems )
				Logger.progress( maxNumItems, i + 1, currentTimeMillis, "Files processed" );
		}).start();

		final String absoluteInputPath = new File( inputPath ).getCanonicalPath();

		if ( checkFileSize( absoluteInputPath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) )
		{
			processedImp = createProcessedImagePlus( inputPath );

			// Images can be in subfolders, thus we only
			// replace the root path
			final String absoluteInputRootDirectory = inputImagesDirectory.getCanonicalPath();
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
		DebugTools.setRootLevel("OFF"); // Bio-Formats

//		Tables.addColumn( jTable, QC, "Passed" );
//		final int columnIndexQC = jTable.getColumnModel().getColumnIndex( QC );
//
//		Tables.addColumn( jTable, PATH_PROCESSED_JPEG, "None" );
//		final int columnIndexPath = jTable.getColumnModel().getColumnIndex( PATH_PROCESSED_JPEG );

		final long currentTimeMillis = System.currentTimeMillis();

		String relativeImageRootDirectory = "images-processed-" + Utils.getLocalDateAndHourAndMinute();
		outputImagesRootDirectory = new File( experimentDirectory, relativeImageRootDirectory );
		IJ.log( "Saving processed images to directory: " + outputImagesRootDirectory );
		IJ.log( "Number of images to be processed: " + numImagesToBeProcessed );

		for ( int i = 0; i < numImagesToBeProcessed; i++ )
		{
			int rowIndex = selectedGateIndices.get( i );

			final String inputImagePath = getInputImagePath( jTable, rowIndex );

			try
			{
				String outputImagePath = saveProcessedImage( numImagesToBeProcessed, currentTimeMillis, i, inputImagePath );
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

	private void setSelectedGateIndices()
	{
		selectedGateIndices = gateToRows.get( selectedGate );
		IJ.log( "Selected gate: " + selectedGate );
		IJ.log( "Images matching gate: " + selectedGateIndices.size() );
	}

	private void setNumImagesToBeProcessed()
	{
		if ( maxNumFiles == -1 )
			numImagesToBeProcessed = selectedGateIndices.size();
		else
			numImagesToBeProcessed = Math.min( maxNumFiles, selectedGateIndices.size() );
	}

	public void saveTableWithAdditionalColumns()
	{
		IJ.log( "\nSaving table with additional columns..." );
		final File tableOutputFile = new File( selectedTableFile.getAbsolutePath().replace( ".csv", "-processed-images.csv" ) );
		Tables.saveTable( jTable, tableOutputFile );
		IJ.log( "...done: " + tableOutputFile );
		IJ.log( " " );
		glimpseTable( jTable );
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


	public void setGatesIndices()
	{
		try
		{
			int gateColumnIndex = jTable.getColumnModel().getColumnIndex( gateColumnName );
			gateToRows = Tables.uniqueColumnEntries( jTable, gateColumnIndex );
			final String gates = gateToRows.keySet().stream().collect( Collectors.joining( "," ) );
			IJ.log( "Gates: " + gates );
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
		if ( selectedGateIndices == null )
		{
			IJ.showMessage( "There are no images of gate " + selectedGate );
			return null;
		}

		final Random random = new Random();
		int randomRowIndex = selectedGateIndices.get( random.nextInt( selectedGateIndices.size() ) );

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

		imageJ.command().run( BDVulcanProcessorCommand.class, true );
	}
}
