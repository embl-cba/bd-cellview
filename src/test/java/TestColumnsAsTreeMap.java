import de.embl.cba.tables.TableUtils;

import javax.swing.*;
import java.util.TreeMap;

public class TestColumnsAsTreeMap
{
	public static void main( String[] args )
	{
		JTable jTable = createTable();

		final TreeMap< Number, Number > map = TableUtils.columnsAsTreeMap( jTable, 0, 1 );
	}

	private static JTable createTable()
	{
		int cols = 2;
		int rows = 10;

		String[] colNames = new String[ cols ];
		for ( int col = 0; col < cols ; col++ )
		{
			colNames[ col ] = "Column_" + col;
		}

		Object[][] data = new Object[ rows ][ cols ];
		for ( int i = 0; i < rows; i++ )
		{
			for ( int j = 0; j < cols; j++ )
			{
				data[ i ][ j ] = i * j;
			}
		}

		return new JTable( data, colNames );
	}
}
