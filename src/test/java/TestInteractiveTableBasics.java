import de.embl.cba.tables.ObjectTablePanel;

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

		final ObjectTablePanel objectTablePanel = new ObjectTablePanel( table );

		final ObjectTablePanel objectTablePanel2 = new ObjectTablePanel( colNames );

		final Object[] newRow = new Object[ rows ];
		for ( int i = 0; i < rows; i++ )
		{
			newRow[ i ] = "aaa";
		}

		objectTablePanel2.addRow( newRow );

		final Double[] newDoubleRow = new Double[ rows ];
		for ( int i = 0; i < rows; i++ )
		{
			newDoubleRow[ i ] = 1.0;
		}

		objectTablePanel2.addRow( newDoubleRow );

		objectTablePanel2.showTable();

	}
}
