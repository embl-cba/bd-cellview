import de.embl.cba.tables.commands.TableBdvConnectionCommand;
import net.imagej.ImageJ;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class RunTableBdvConnectionCommand
{
	public static void main( String[] args ) throws ExecutionException, InterruptedException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final HashMap< String, Object > parameters = new HashMap<>();

		parameters.put( "inputTableFile",
				new File( RunTableBdvConnectionCommand.class.getResource(
						"2d-16bit-labelMask-Morphometry.csv" ).getFile() ) );

		parameters.put( "inputLabelMasksFile",
				new File( RunTableBdvConnectionCommand.class.getResource(
						"2d-16bit-labelMask.tif" ).getFile() ) );

		parameters.put( "objectLabelsColumnIndex", 0 );

		parameters.put( "inputIntensitiesFile", null );

		ij.command().run( TableBdvConnectionCommand.class, true, parameters );
	}
}
