package de.embl.cba.cellview.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.embl.cba.cellview.CellViewChannel;
import de.embl.cba.cellview.CellViewImageProcessor;
import de.embl.cba.cellview.CellViewUtils;
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
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static de.embl.cba.cellview.CellViewUtils.checkFileSize;

@Plugin( type = Command.class, menuPath = "Plugins>CellView>Process CellView Images"  )
public class CellViewProcessorCommand implements Command, Interactive
{
	private transient static final String NONE = "None";

	@Parameter ( label = "Image Tables", persist = true, required = false )
	public transient File[] tableFiles;

	@Parameter ( label = "Minimum File Size [kb]" )
	public double minimumFileSizeKiloBytes = 10;

	@Parameter ( label = "Maximum File Size [kb]" )
	public double maximumFileSizeKiloBytes = 100000;

	@Parameter ( label = "Horizontal Crop [pixels]", callback = "processAndShowImageFromFilePath" )
	public int horizontalCropPixels = 0;

	@Parameter ( label = "Channel Indices" )
	public String channelIndexCSV = "1,2,4,3,5";

	@Parameter ( label = "Colors (* : include in merge)" )
	public String colorCSV = "White*,Green*,Magenta*,White,White";

	@Parameter ( label = "Minimum Gray Levels" )
	public String minimumGrayLevelCSV = "0.0,0.0,0.0,0.0,0.0";

	@Parameter ( label = "Maximum Gray Levels" )
	public String maximumGrayLevelCSV = "1.0,1.0,1.0,1.0,1.0";

	@Parameter( label = "Update Current Image", callback = "processAndShowImageFromFilePath" )
	public Button updateButton;

	@Parameter ( label = "Processing Modality", choices = { CellViewImageProcessor.VIEW_RAW, CellViewImageProcessor.VIEW_PROCESSED_OVERLAY, CellViewImageProcessor.VIEW_PROCESSED_OVERLAY_AND_INDIVIDUAL_CHANNELS } )
	public String viewingModality = CellViewImageProcessor.VIEW_PROCESSED_OVERLAY_AND_INDIVIDUAL_CHANNELS;

	@Parameter ( label = "Process Image File", callback = "processImageFileButtonCallback", required = false )
	private transient File imageFile;

	@Parameter ( label = "Select Table", callback = "selectTableButtonCallback" )
	public transient Button selectTableButton;

	@Parameter ( label = "Select Gate", callback = "selectGateButtonCallback" )
	public transient Button selectGateButton;

	@Parameter ( label = "Process Random Image of Selected Gate from Selected Table", callback = "processAndShowRandomImageFromSelectedGateButtonCallback" )
	private transient Button processAndShowRandomImageButton;

	@Parameter ( label = "Process All Images of Selected Gate from All Tables ", callback = "processImagesButtonCallback" )
	private transient Button processAllImagesButton;

	@Parameter ( label = "Save Settings for Headless Processing", callback = "saveSettingsButtonCallback" )
	private transient Button saveHeadlessSettingsButton;

	@Parameter ( label = "Maximum Number of Files to Process [-1 = all]" )
	public int maxNumFiles = -1;

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
	private transient String imageFilePath;
	private transient ArrayList< Integer > selectedGateIndices;
	private transient CellViewImageProcessor imageProcessor = new CellViewImageProcessor();
	private transient ArrayList< CellViewChannel > channels;

	public static CellViewProcessorCommand createBdVulcanProcessorCommandFromJson( File jsonFile ) throws IOException
	{
		InputStream inputStream = FileAndUrlUtils.getInputStream( jsonFile.getAbsolutePath() );
		final JsonReader reader = new JsonReader( new InputStreamReader( inputStream, "UTF-8" ) );
		DebugTools.setRootLevel("OFF"); // Bio-Formats
		Gson gson = new Gson();
		Type type = new TypeToken< CellViewProcessorCommand >() {}.getType();
		return gson.fromJson( reader, type );
	}

	public void run()
	{
		DebugTools.setRootLevel("OFF"); // Bio-Formats
	}

	private void processImageFileButtonCallback()
	{
		imageFilePath = imageFile.getPath();
		processAndShowImageFromFilePath();
	}

	private void processAndShowRandomImageFromSelectedGateButtonCallback()
	{
		if ( tableFiles == null || tableFiles.length == 0 )
		{
			IJ.showMessage( "Please select some tables first!" );
			return;
		}

		if ( selectedTableFile == null )
		{
			selectedTableFile = selectTableDialog( tableFiles );
			jTable = null; // force table loading;
		}

		if ( jTable == null )
		{
			loadTableAndUpdateRelatedFields( selectedTableFile );
			gateColumnName = selectGateColumnDialog( jTable );
			setGatesIndices();
			selectedGate = selectGateDialog( selectedGate, gateToRows.keySet() );
			setSelectedGateIndices();
		}

		DebugTools.setRootLevel("OFF"); // Bio-Formats

		imageFilePath = getRandomFilePath();
		if ( imageFilePath == null ) return;

		if ( ! checkFileSize( imageFilePath, minimumFileSizeKiloBytes, maximumFileSizeKiloBytes ) ) return;

		processAndShowImageFromFilePath();
	}

	private void processAndShowImageFromFilePath()
	{
		DebugTools.setRootLevel( "OFF" ); // Bio-Formats
		IJ.log( "Preview image. Table: " + selectedTableFile + "; Gate: " + selectedGate );
		if ( ! setColorToSliceAndColorToRange() ) return;
		if ( imageFilePath == null ) return;
		if ( processedImp != null ) processedImp.close();
		processedImp = createProcessedImagePlus( imageFilePath, horizontalCropPixels );
		processedImp.show();
	}

	private void processImagesButtonCallback()
	{
		// the new thread is necessary for the progress logging in the IJ.log window
		new Thread( () ->
		{
			/**
			 * Ensure that there is a valid gate selected.
			 * For this we need to load a table.
			 */
			if ( jTable == null )
			{
				selectedTableFile = tableFiles[ 0 ];
				loadTableAndUpdateRelatedFields( selectedTableFile );
				setGatesIndices();
			}

			if ( selectedGate == null || ! gateToRows.keySet().contains( selectedGate ) )
			{
				selectedGate = selectGateDialog( selectedGate, gateToRows.keySet() );
				setSelectedGateIndices();
			}

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
			processImagesFromSelectedTableFile();
			if ( quitAfterRun ) Commands.quitImageJ();
		}).start();
	}

	private void processImagesFromSelectedTableFile()
	{
		IJ.log( "Processing table: " + selectedTableFile );
		loadTableAndUpdateRelatedFields( selectedTableFile );
		setColorToSliceAndColorToRange();
		setGatesIndices();
		setSelectedGateIndices();
		setNumImagesToBeProcessed();
		processAndSaveImages();
	}

	private void selectTableButtonCallback()
	{
		if ( tableFiles == null || tableFiles.length == 0 )
		{
			IJ.showMessage( "Please add some tables first." );
			return;
		}

		selectedTableFile = selectTableDialog( tableFiles );
	}


	private static File selectTableDialog( File[] tableFiles )
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

	private void selectGateButtonCallback()
	{
		if ( jTable == null )
		{
			if ( selectedTableFile == null )
			{
				selectedTableFile = selectTableDialog( tableFiles );
			}

			loadTableAndUpdateRelatedFields( selectedTableFile );
			gateColumnName = selectGateColumnDialog( jTable );
			setGatesIndices();
		}

		selectedGate = selectGateDialog( selectedGate, gateToRows.keySet() );
		setSelectedGateIndices();
	}

	private static String selectGateDialog( String currentSelectedGate, Set< String > gates )
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

	/**
	 * Save a settings file that can be used to run the processing in headless mode,
	 * for example on a computer cluster.
	 *
	 * The settings file can be executed by the {@code BDVulcanHeadlessProcessorCommand}
	 *
	 * @throws IOException
	 */
	private void saveSettingsButtonCallback()
	{
		new Thread( () -> {

			if ( selectedGate == null || ! gateToRows.keySet().contains( selectedGate ) )
			{
				IJ.showMessage( "Please first select a Gate from which to process the images!" );
				return;
			}

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
			this.quitAfterRun = false;
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
		loadTableAndUpdateRelatedFields( tableFile );
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
		try
		{
			final int[] channelIndices = Arrays.stream( channelIndexCSV.split( "," ) ).mapToInt( x -> Integer.parseInt( x.trim() ) ).toArray();

			final double[] minValues = Arrays.stream( minimumGrayLevelCSV.split( "," ) ).mapToDouble( x -> Double.parseDouble( x.trim() ) ).toArray();

			final double[] maxValues = Arrays.stream( maximumGrayLevelCSV.split( "," ) ).mapToDouble( x -> Double.parseDouble( x.trim() ) ).toArray();

			final String[] colors = colorCSV.split( "," );

			if ( minValues.length != channelIndices.length )
			{
				// TODO: implement more error checking
			}

			channels = new ArrayList<>( );

			for ( int i = 0; i < channelIndices.length; i++ )
			{
				channels.add( new CellViewChannel( channelIndices[ i ], colors[ i ], new double[]{ minValues[ i ], maxValues[ i ]} ) );
			}

			return true;
		}
		catch	( Exception e )
		{
			e.printStackTrace();
			IJ.showMessage( "Error parsing the image conversion settings." );
			return false;
		}
	}

	public void loadTableAndUpdateRelatedFields( File selectedTableFile )
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
		IJ.log("\nLoading table: " + absolutePath );
		jTable = Tables.loadTable( absolutePath );
		IJ.log( "Loaded table in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );
	}

	private static String selectGateColumnDialog( JTable jTable )
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
			processedImp = createProcessedImagePlus( inputPath, horizontalCropPixels );

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

		String relativeImageRootDirectory = "images-processed-" + CellViewUtils.getLocalDateAndHourAndMinute();
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

	private ImagePlus createProcessedImagePlus( String filePath, int horizontalCropNumPixels )
	{
		ImagePlus processedImp = imageProcessor.run( filePath, channels, horizontalCropNumPixels, viewingModality );

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

		imageJ.command().run( CellViewProcessorCommand.class, true );
	}
}