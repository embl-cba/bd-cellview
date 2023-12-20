package run;

import de.embl.cba.cellview.command.CellViewProcessorCommand;
import net.imagej.ImageJ;

public class RunEventProcessor
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		imageJ.command().run( CellViewProcessorCommand.class, true );
	}
}
