import de.embl.cba.tables.InteractiveTablePanel;
import de.embl.cba.tables.TableUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class TestTableLoading
{
	public static void main( String[] args ) throws IOException
	{
		final File file = new File( "/Users/tischer/Desktop/filtered.csv" );

		final JTable jTable = TableUtils.loadTable( file, "\t", 2, 1000.0 );

		new InteractiveTablePanel( jTable );
	}
}
