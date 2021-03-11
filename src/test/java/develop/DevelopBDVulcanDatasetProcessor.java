package develop;

import de.embl.cba.cellview.CellViewProcessorCommand;
import net.imagej.ImageJ;

public class DevelopBDVulcanDatasetProcessor
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		imageJ.command().run( CellViewProcessorCommand.class, true );
	}
}
