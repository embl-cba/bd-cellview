package de.embl.cba.tables;

import org.scijava.table.GenericTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class TableUtils
{
	public static JTable asJTable( GenericTable genericTable )
	{
		final int numCols = genericTable.getColumnCount();

		DefaultTableModel model = new DefaultTableModel();

		for ( int col = 0; col < numCols; ++col )
		{
			model.addColumn( genericTable.getColumnHeader( col ) );
		}

		for ( int row = 0; row < genericTable.getRowCount(); ++row )
		{
			final String[] rowEntries = new String[ numCols ];

			for ( int col = 0; col < numCols; ++col )
			{
				rowEntries[ col ] = ( String ) genericTable.get( col, row );
			}

			model.addRow( rowEntries );
		}

		return new JTable( model );
	}

	public static void saveTable( JTable table ) throws IOException
	{
		final JFileChooser jFileChooser = new JFileChooser( "" );

		if ( jFileChooser.showSaveDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			BufferedWriter bfw = new BufferedWriter( new FileWriter( jFileChooser.getSelectedFile() ) );

			// header
			for ( int column = 0; column < table.getColumnCount(); column++ )
			{
				bfw.write( table.getColumnName( column ) + "\t" );
			}
			bfw.write( "\n" );

			// content
			for ( int row = 0; row < table.getRowCount(); row++ )
			{
				for ( int column = 0; column < table.getColumnCount(); column++ )
				{
					bfw.write( table.getValueAt( row, column ) + "\t" );
				}
				bfw.write( "\n" );
			}

			bfw.close();
		}
	}

	public static JTable loadTable( File file, String delim ) throws IOException
	{
		return 	loadTable( file, delim, -1, -1.0 );
	}

	public static JTable loadTable( final File file, final String delim, final int filterColumnIndex, final Double filterValue ) throws IOException
	{

		DefaultTableModel model = null;

		try {
			FileInputStream fin =  new FileInputStream( file );
			BufferedReader br = new BufferedReader(new InputStreamReader(fin));

			// extract column names
			StringTokenizer st1 = new StringTokenizer( br.readLine(), delim );
			ArrayList< String > colNames = new ArrayList<>(  );
			while( st1.hasMoreTokens() )
			{
				colNames.add( st1.nextToken() );
			}

			// extract data
			int numCols = colNames.size();
			String aRow;
			int iRow = 0;

			final Double[] rowEntries = new Double[ numCols ];
			StringTokenizer st2;
			int iCol;

			while ((aRow = br.readLine()) != null)
			{
				st2 = new StringTokenizer( aRow, delim );

				iCol = 0;

				while( st2.hasMoreTokens() )
				{
					rowEntries[ iCol++ ] = Double.parseDouble(  st2.nextToken() );
				}

				if ( iRow == 0 )
				{
					model = new DefaultTableModel()
					{
						@Override
						public Class getColumnClass( int column )
						{
							return Double.class;
						}

						@Override
						public boolean isCellEditable(int row, int column)
						{
							return false;
						}
					};
					iRow++;

					for ( String colName : colNames )
					{
						model.addColumn( colName );
					}
				}

				if ( filterColumnIndex != -1 )
				{
					if ( rowEntries[ filterColumnIndex ] > filterValue )
					{
						model.addRow( rowEntries );
					}
				}
				else
				{
					model.addRow( rowEntries );
				}

			}

			br.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


		return new JTable( model );
	}
}
