import de.embl.cba.tables.InteractiveTablePanel;

import javax.swing.*;

public class TestInteractiveTable
{
	public static void main( String[] args )
	{

		int n = 300;
		int m = 10;

		String[] colNames = new String[ n ];
		Object[][] data = new Object[ m ][ n ];

		for ( int i = 0; i < n ; i++ )
		{
			colNames[ i ] = "AAAAA";
		}

		for ( int i = 0; i < m; i++ )
		{
			for ( int j = 0; j < n; j++ )
			{
				data[ i ][ j ] = i * j;
			}
		}

		JTable table = new JTable( data, colNames );

		new InteractiveTablePanel( table );

	}
}
