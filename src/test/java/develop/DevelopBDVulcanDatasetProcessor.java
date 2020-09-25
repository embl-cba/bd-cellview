package develop;

import de.embl.cba.imflow.BDVulcanProcessorCommand;
import net.imagej.ImageJ;

public class DevelopBDVulcanDatasetProcessor
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		imageJ.command().run( BDVulcanProcessorCommand.class, true );
	}
}
