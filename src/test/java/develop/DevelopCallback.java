package develop;

import de.embl.cba.fccf.BDVulcanDatasetProcessor;
import de.embl.cba.fccf.devel.callback.TestCallbackCommand;
import net.imagej.ImageJ;

public class DevelopCallback
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		TestCallbackCommand.cs = imageJ.command();

		imageJ.command().run( TestCallbackCommand.class, true );
	}
}
