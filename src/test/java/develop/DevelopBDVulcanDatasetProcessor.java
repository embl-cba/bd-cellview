package develop;

import de.embl.cba.fccf.BDVulcanDatasetProcessor;
import net.imagej.ImageJ;

public class DevelopBDVulcanDatasetProcessor
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		imageJ.command().run( BDVulcanDatasetProcessor.class, true );
	}
}
