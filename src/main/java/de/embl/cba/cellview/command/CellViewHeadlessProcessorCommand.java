package de.embl.cba.cellview.command;

import ij.IJ;
import ij.plugin.frame.Recorder;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.*;

@Plugin( type = Command.class, menuPath = "Plugins>BD CellView>" + CellViewHeadlessProcessorCommand.COMMAND_NAME )
public class CellViewHeadlessProcessorCommand implements Command
{
	public static final String COMMAND_NAME = "Process BD CellView Images Headless";
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
			CellViewProcessorCommand command = CellViewProcessorCommand.createCellViewProcessorCommandFromJson( settingsFile );
			command.processImagesHeadless();
			System.out.println("Waiting...");
			System.out.println("Done waiting.");
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new Recorder();

		imageJ.command().run( CellViewHeadlessProcessorCommand.class, true );

		/**
		 *  /Users/tischer/Desktop/Fiji-imflow.app/Contents/MacOS/ImageJ-macosx --headless --run "Process BD CellView Images Headless" "settingsFile='/Users/tischer/Documents/fccf/src/test/resources/minimalgated/batchProcess.json'"
		 */
	}
}
