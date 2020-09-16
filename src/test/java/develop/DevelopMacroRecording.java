package develop;

import de.embl.cba.imflow.devel.callback.TestSomeCommand;
import ij.plugin.frame.Recorder;
import net.imagej.ImageJ;

public class DevelopMacroRecording
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final Recorder recorder = new Recorder();
		imageJ.command().run( TestSomeCommand.class, true );
	}
}
