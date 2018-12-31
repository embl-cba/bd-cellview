import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.ObjectTableCoordinateColumnsSelectionCommand;
import net.imagej.ImageJ;
import org.scijava.command.CommandModule;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Deprecated
public class TestObjectTableCoordinateColumnsSelectionCommand
{
	public static void main( String[] args ) throws ExecutionException, InterruptedException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final File file = new File( TestTableLoading.class.getResource( "tab-delim-numeric-and-string-table.csv" ).getFile() );

		final HashMap< String, Object > parameters = new HashMap<>();
		parameters.put( "tableFile", file );

		//ij.command().run( TableObjectColumnSelectionCommand.class, true );

		final Future< CommandModule > run = ij.command().run(
				ObjectTableCoordinateColumnsSelectionCommand.class,
				true,
				parameters );

		final CommandModule commandModule = run.get();

		for ( ObjectCoordinate coordinate : ObjectCoordinate.values() )
		{
			commandModule.getInput( ObjectTableCoordinateColumnsSelectionCommand.getCoordinateParameterName( coordinate ) );
		}
	}
}
