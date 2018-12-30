import de.embl.cba.tables.ObjectTableCoordinateColumnsSelectionCommand;
import net.imagej.ImageJ;

import java.io.File;
import java.util.HashMap;

public class TestObjectTableCoordinateColumnsSelectionCommand
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final File file = new File( TestTableLoading.class.getResource( "tab-delim-numeric-and-string-table.csv" ).getFile() );

		final HashMap< String, Object > parameters = new HashMap<>();
		parameters.put( "tableFile", file );

		//ij.command().run( TableObjectColumnSelectionCommand.class, true );

		ij.command().run(
				ObjectTableCoordinateColumnsSelectionCommand.class,
				true,
				parameters );
	}
}
