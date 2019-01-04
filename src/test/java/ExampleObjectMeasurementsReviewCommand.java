import de.embl.cba.tables.commands.ObjectMeasurementsReviewCommand;
import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.objects.ObjectTablePanel;
import net.imagej.ImageJ;
import org.scijava.command.CommandModule;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class ExampleObjectMeasurementsReviewCommand
{
	public static void main( String[] args ) throws ExecutionException, InterruptedException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final HashMap< String, Object > parameters = new HashMap<>();

		parameters.put( "inputTableFile",  new File( ExampleObjectMeasurementsReviewCommand.class.getResource( "2d-16bit-labelMask-Morphometry.csv" ).getFile() ) );
		parameters.put( "inputLabelMasksFile", new File( ExampleObjectMeasurementsReviewCommand.class.getResource( "2d-16bit-labelMask.tif" ).getFile() ) );
		parameters.put( "objectLabelsColumnIndex", 0 );
		parameters.put( "inputIntensitiesFile", null );

		ij.command().run( ObjectMeasurementsReviewCommand.class, true, parameters );
	}
}
