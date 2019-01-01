import de.embl.cba.tables.commands.ObjectMeasurementsReviewCommand;
import net.imagej.ImageJ;

import java.io.File;
import java.util.HashMap;


public class TestObjectMeasurementsReviewCommand
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final HashMap< String, Object > parameters = new HashMap<>();

		parameters.put( "inputTableFile",  new File( TestObjectMeasurementsReviewCommand.class.getResource( "2d-16bit-labelMask-Morphometry.csv" ).getFile() ) );
		parameters.put( "inputLabelMasksFile", new File( TestObjectMeasurementsReviewCommand.class.getResource( "2d-16bit-labelMask.tif" ).getFile() ) );
		parameters.put( "objectLabelsColumnIndex", 0 );
		parameters.put( "inputIntensitiesFile", null );

		ij.command().run( ObjectMeasurementsReviewCommand.class, true, parameters );
	}
}
