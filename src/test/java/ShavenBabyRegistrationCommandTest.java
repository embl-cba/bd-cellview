import de.embl.cba.drosophila.dapi.DapiRegistrationCommand;
import de.embl.cba.drosophila.dapi.ShavenBabyRegistrationCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

public class ShavenBabyRegistrationCommandTest
{

	public static void main(final String... args) throws Exception
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/E3NWT-04-downscaled-svb.tif";

		// Load and show data
		ImagePlus imp = IJ.openImage( path );
		//imp.show();

		// invoke the plugin
		ij.command().run( ShavenBabyRegistrationCommand.class, true );
	}

}
