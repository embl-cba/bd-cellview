package run;

import de.embl.cba.cellview.command.CellViewHeadlessProcessorCommand;
import de.embl.cba.cellview.command.CellViewProcessorCommand;
import net.imagej.ImageJ;

import java.io.File;

public class RunHeadless
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		CellViewHeadlessProcessorCommand command = new CellViewHeadlessProcessorCommand();
		command.settingsFile =  new File("/Users/tischer/Downloads/demo_data/json/batch_2channels.json");
		command.run();

		//  /Users/tischer/Desktop/Fiji/Fiji.app/Contents/MacOS/ImageJ-macosx --headless --run "Process BD CellView Images Headless" "settingsFile='/Users/tischer/Downloads/demo_data/json/batch_2channels.json'"
	}
}
