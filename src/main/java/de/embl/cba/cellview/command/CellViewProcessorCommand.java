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
import static de.embl.cba.cellview.CellViewUtils.localDateAndHourAndMinute;

@Plugin( type = Command.class, menuPath = "Plugins>BD CellView>Process BD CellView Images"  )
public class CellViewProcessorCommand implements Command, Interactive
{
	private transient static final String NONE = "None";
	public static final String PROCESS_RANDOM_IMAGE = "Process random image of selected gate from selected table";

	@Parameter ( label = "Please read the usage instructions", callback = "helpButtonCallback" )
	public transient Button helpButton;

	@Parameter ( label = "Image tables", persist = true, required = false )
	public transient File[] tableFiles;

	@Parameter ( label = "Image file size filter (min, max) [kb]" )
	public String fileSizeRangeCSV = "10,100000";

	@Parameter ( label = "Selected channel indices in the raw image" )
	public String channelIndexCSV = "1,2,4,3,5";

	@Parameter ( label = "Colors (*: include in merge)" )
	public String colorCSV = "White*,Green*,Magenta*,White,White";

	@Parameter ( label = "Show only merge in color" )
	public boolean showOnlyMergeInColor = false;

	@Parameter ( label = "Minimum gray levels" )
	public String minimumGrayLevelCSV = "0.0,0.0,0.0,0.0,0.0";

	@Parameter ( label = "Maximum gray levels" )
	public String maximumGrayLevelCSV = "1.0,1.0,1.0,1.0,1.0";

	@Parameter ( label = "Horizontal crop [pixels]" )
	public int horizontalCropPixels = 0;

	@Parameter( label = "Update current image", callback = "processAndShowImageFromFilePath" )
	public transient Button updateButton;

	@Parameter ( label = "Processing modality", choices = { CellViewImageProcessor.RAW, CellViewImageProcessor.PROCESSED_MERGE, CellViewImageProcessor.PROCESSED_MERGE_AND_INDIVIDUAL_CHANNELS } )
	public String viewingModality = CellViewImageProcessor.PROCESSED_MERGE_AND_INDIVIDUAL_CHANNELS;

	@Parameter ( label = "Process and show image file", callback = "processImageFileButtonCallback", required = false )
	private transient File imageFile;

	@Parameter ( label = "Select table", callback = "selectTableButtonCallback" )
	public transient Button selectTableButton;

	@Parameter ( label = "Select gate", callback = "selectGateButtonCallback" )
	public transient Button selectGateButton;

	@Parameter ( label = PROCESS_RANDOM_IMAGE, callback = "processAndShowRandomImageFromSelectedGateButtonCallback" )
	private transient Button processAndShowRandomImageButton;

	@Parameter ( label = "Maximum number of files to batch process [-1 = all]" )
	public int maxNumFiles = -1;

	@Parameter ( label = "Batch process images of selected gate from all tables", callback = "processImagesButtonCallback" )
	private transient Button processAllImagesButton;

	@Parameter ( label = "Save settings for headless batch processing", callback = "saveSettingsButtonCallback" )
	private transient Button saveHeadlessSettingsButton;

	public String experimentDirectory;
	public File selectedTableFile;
	public String gateColumnName = "gate";
	public boolean quitAfterRun = false;
	public String selectedGate;
	public int numImagesToBeProcessed;

	public static boolean verbose = false; // for debugging

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

	public static CellViewProcessorCommand createCellViewProcessorCommandFromJson( File jsonFile ) throws IOException
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

	private void helpButtonCallback()
	{
		FileAndUrlUtils.openURI( "https://github.com/embl-cba/ICS/blob/master/README.md#cellview-image-processing" );
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
			selectedTableFile = CellViewUtils.selectTableDialog( tableFiles );
			jTable = null; // force table loading;
		}

		if ( jTable == null )
		{
			loadTableAndUpdateRelatedFields( selectedTableFile );
			gateColumnName = CellViewUtils.selectGateColumnDialog( jTable );
			setGatesIndices();
			selectedGate = CellViewUtils.selectGateDialog( selectedGate, gateToRows.keySet() );
			setSelectedGateIndices();
		}

		DebugTools.setRootLevel("OFF"); // Bio-Formats

		imageFilePath = getRandomFilePath();
		if ( imageFilePath == null ) return;

		if ( ! checkFileSize( imageFilePath, fileSizeRangeCSV, verbose ) ) return;

		processAndShowImageFromFilePath();
	}

	private void processAndShowImageFromFilePath()
	{
		DebugTools.setRootLevel( "OFF" ); // Bio-Formats
		IJ.log( "Preview image. Table: " + selectedTableFile + "; Gate: " + selectedGate );
		if ( ! setColorToSliceAndColorToRange() ) return;
		if ( imageFilePath == null )
		{
			IJ.log("[ERROR] No image found. Please click ["+PROCESS_RANDOM_IMAGE+"]");
			return;
		}
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
				selectedGate = CellViewUtils.selectGateDialog( selectedGate, gateToRows.keySet() );
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
		processImagesFromSelectedTableFile();
		if ( quitAfterRun ) Commands.quitImageJ();
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

		selectedTableFile = CellViewUtils.selectTableDialog( tableFiles );
	}


	private void selectGateButtonCallback()
	{
		if ( jTable == null )
		{
			if ( selectedTableFile == null )
			{
				selectedTableFile = CellViewUtils.selectTableDialog( tableFiles );
			}

			loadTableAndUpdateRelatedFields( selectedTableFile );
			gateColumnName = CellViewUtils.selectGateColumnDialog( jTable );
			setGatesIndices();
		}

		selectedGate = CellViewUtils.selectGateDialog( selectedGate, gateToRows.keySet() );
		setSelectedGateIndices();
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

		final File settingsFile = new File( experimentDirectory, "batch-process-" + localDateAndHourAndMinute() + ".json" );

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

	private void loadTableAndUpdateRelatedFields( File selectedTableFile )
	{
		final String absolutePath = selectedTableFile.toString();

		if ( ! selectedTableFile.exists()  )
		{
			Logger.log( "Table file does not exist: " + selectedTableFile );
			throw new UnsupportedOperationException( "Could not open file: " + selectedTableFile );
		}

		// final String absolutePath = tableFile.getAbsolutePath(); this does not work when loading from json for some reason...
		//final String absolutePath = selectedTableFile.toString();
		loadTable( absolutePath );

		experimentDirectory = new File( selectedTableFile.getParent() ).getParent();
		inputImagesDirectory = new File( experimentDirectory, "images" );
		pathColumnIndex = jTable.getColumnModel().getColumnIndex( imagePathColumnName );

		CellViewUtils.glimpseTable( jTable );
	}

	private void loadTable( String absolutePath )
	{
		final long currentTimeMillis = System.currentTimeMillis();
		IJ.log("\nLoading table: " + absolutePath );
		jTable = Tables.loadTable( absolutePath );
		IJ.log( "Loaded table in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );
	}

	private String saveProcessedImage( int maxNumItems, long currentTimeMillis, int i, String inputPath ) throws IOException
	{

		final String absoluteInputPath = new File( inputPath ).getCanonicalPath();

		if ( verbose ) IJ.log( "\nAnalyzing " + absoluteInputPath );

		if ( verbose ) IJ.log( "Memory " + IJ.freeMemory() );

		if ( checkFileSize( absoluteInputPath, fileSizeRangeCSV, verbose ) )
		{
			if ( verbose ) IJ.log( "Processing..."  );
			processedImp = createProcessedImagePlus( inputPath, horizontalCropPixels );

			// Images can be in sub-folders, thus we only replace the root path
			final String absoluteInputRootDirectory = inputImagesDirectory.getCanonicalPath();
			final String absoluteOutputRootDirectory = outputImagesRootDirectory.getCanonicalPath();
			String outputImagePath = absoluteInputPath.replace(
					absoluteInputRootDirectory,
					absoluteOutputRootDirectory );

			if ( verbose ) IJ.log( "Saving..." );
			saveImageAsJpeg( outputImagePath, processedImp );

			// log progress
			new Thread( () -> {
				if ( ( i + 1 ) % 100 == 0 || i == 0 || i + 1 == maxNumItems )
				{
					Logger.progress( maxNumItems, i + 1, currentTimeMillis, "Files processed" );
				}
			}).start();

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

		final long currentTimeMillis = System.currentTimeMillis();

		String relativeImageRootDirectory = "images-processed-" + CellViewUtils.localDateAndHourAndMinute();
		outputImagesRootDirectory = new File( experimentDirectory, relativeImageRootDirectory );
		IJ.log( "Saving processed images to directory: " + outputImagesRootDirectory );
		IJ.log( "Number of images to be processed: " + numImagesToBeProcessed );

		for ( int i = 0; i < numImagesToBeProcessed; i++ )
		{
			int rowIndex = selectedGateIndices.get( i );

			final String inputImagePath = getInputImagePath( jTable, rowIndex );

			try
			{
				saveProcessedImage( numImagesToBeProcessed, currentTimeMillis, i, inputImagePath );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
				throw new RuntimeException( "Error during saving of image: " + inputImagePath );
			}
		}
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

	private void setGatesIndices()
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
	}

	private ImagePlus createProcessedImagePlus( String filePath, int horizontalCropNumPixels )
	{
		ImagePlus processedImp = imageProcessor.run( filePath, channels, horizontalCropNumPixels, viewingModality, showOnlyMergeInColor );

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
