package develop;

import com.google.gson.Gson;
import de.embl.cba.imflow.devel.callback.TestSomeCommand;
import net.imagej.ImageJ;

public class DevelopCommandFromAndToJson
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final Gson gson = new Gson();
		final TestSomeCommand testSomeCommand = gson.fromJson( "{\"file\":{\"path\":\"/Users/tischer/Documents/fccf/src/test/resources/minimalgated/output/countpath-withQC.csv\"},\"string\":\"hello!\"}", TestSomeCommand.class );

		imageJ.command().run( TestSomeCommand.class, true );
	}
}
