package develop;

import de.embl.cba.fccf.BDVulcanDatasetProcessor;
import ij.plugin.frame.Recorder;
import net.imagej.ImageJ;

public class DevelopBDVulcanDatasetProcessor
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final Recorder recorder = new Recorder();

		imageJ.command().run( BDVulcanDatasetProcessor.class, true );
	}
}