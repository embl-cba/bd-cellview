import de.embl.cba.tables.commands.LinkedTableAndImageVisualisationCommand;
import net.imagej.ImageJ;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class ExampleTableBdvConnectionCommand
{
	public static void main( String[] args ) throws ExecutionException, InterruptedException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final HashMap< String, Object > parameters = new HashMap<>();

		parameters.put( "inputTableFile",  new File( ExampleTableBdvConnectionCommand.class.getResource( "2d-16bit-labelMask-Morphometry.csv" ).getFile() ) );
		parameters.put( "inputLabelMasksFile", new File( ExampleTableBdvConnectionCommand.class.getResource( "2d-16bit-labelMask.tif" ).getFile() ) );
		parameters.put( "objectLabelsColumnIndex", 0 );
		parameters.put( "inputIntensitiesFile", null );

		ij.command().run( LinkedTableAndImageVisualisationCommand.class, true, parameters );
	}
}
