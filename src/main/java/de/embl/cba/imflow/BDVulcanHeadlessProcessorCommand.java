package de.embl.cba.imflow;

import ij.plugin.frame.Recorder;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.*;

@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>" + BDVulcanHeadlessProcessorCommand.COMMAND_NAME )
public class BDVulcanHeadlessProcessorCommand implements Command
{
	public static final String COMMAND_NAME = "Headless Process Single BD Vulcan Dataset";
	private transient static final String NONE = "None";

	@Parameter
	private transient LogService logService;

	@Parameter ( label = "Settings File" )
	public File settingsFile = new File("Please browse to a settings file");
	public static String SETTINGS_FILE = "settingsFile";

	public void run()
	{
		try
		{
			BDVulcanProcessorCommand command = BDVulcanProcessorCommand.createBdVulcanProcessorCommandFromJson( settingsFile );
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

		imageJ.command().run( BDVulcanHeadlessProcessorCommand.class, true );

		/**
		 *  /Users/tischer/Desktop/Fiji-imflow.app/Contents/MacOS/ImageJ-macosx --headless --run "Batch Process BD Vulcan Dataset" "settingsFile='/Users/tischer/Documents/fccf/src/test/resources/minimalgated/batchProcess.json'"
		 */
	}
}
