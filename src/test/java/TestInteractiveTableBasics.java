import de.embl.cba.tables.InteractiveTablePanel;

import javax.swing.*;

public class TestInteractiveTableBasics
{
	public static void main( String[] args )
	{

		int cols = 300;
		int rows = 10;

		String[] colNames = new String[ cols ];
		Object[][] data = new Object[ rows ][ cols ];

		for ( int i = 0; i < cols ; i++ )
		{
			colNames[ i ] = "AAAAA";
		}

		for ( int i = 0; i < rows; i++ )
		{
			for ( int j = 0; j < cols; j++ )
			{
				data[ i ][ j ] = i * j;
			}
		}

		JTable table = new JTable( data, colNames );

		final InteractiveTablePanel interactiveTablePanel = new InteractiveTablePanel( table );

		final InteractiveTablePanel interactiveTablePanel2 = new InteractiveTablePanel( colNames );

		final Object[] newRow = new Object[ rows ];
		for ( int i = 0; i < rows; i++ )
		{
			newRow[ i ] = "aaa";
		}

		interactiveTablePanel2.addRow( newRow );

		final Double[] newDoubleRow = new Double[ rows ];
		for ( int i = 0; i < rows; i++ )
		{
			newDoubleRow[ i ] = 1.0;
		}

		interactiveTablePanel2.addRow( newDoubleRow );

	}
}
