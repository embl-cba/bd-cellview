package de.embl.cba.imflow;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.embl.cba.tables.FileAndUrlUtils;
import ij.plugin.frame.Recorder;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Type;

@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Batch Process BD Vulcan Dataset"  )
public class BDVulcanBatchProcessorCommand implements Command
{
	private transient static final String NONE = "None";

	@Parameter
	private transient LogService logService;

	@Parameter ( label = "Settings File" )
	public File settingsFile = new File("Please browse to a dataset table file");

	public void run()
	{
		try
		{
			InputStream inputStream = FileAndUrlUtils.getInputStream( settingsFile.getAbsolutePath() );
			final JsonReader reader = new JsonReader( new InputStreamReader( inputStream, "UTF-8" ) );
			DebugTools.setRootLevel("OFF"); // Bio-Formats
			Gson gson = new Gson();
			Type type = new TypeToken< BDVulcanDatasetProcessorCommand >() {}.getType();
			BDVulcanDatasetProcessorCommand command = gson.fromJson( reader, type );
			command.headlessProcessImagesFromSelectedTableFile();
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new Recorder();

		imageJ.command().run( BDVulcanBatchProcessorCommand.class, true );

		/**
		 *  /Users/tischer/Desktop/Fiji-imflow.app/Contents/MacOS/ImageJ-macosx --headless --run "Batch Process BD Vulcan Dataset" "settingsFile='/Users/tischer/Documents/fccf/src/test/resources/minimalgated/batchProcess.json'"
		 */
	}
}
