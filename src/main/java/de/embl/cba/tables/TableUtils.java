package de.embl.cba.tables;

import net.imagej.table.GenericTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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

	public static void saveTable( JTable table )
	{

		final JFileChooser jFileChooser = new JFileChooser( "" );

		try
		{
			BufferedWriter bfw = new BufferedWriter( new FileWriter( jFileChooser.getSelectedFile() ) );

			// header
			for( int column = 0; column < table.getColumnCount(); column++)
			{
				bfw.write( table.getColumnName(column) + "\t" );
			}
			bfw.write( "\n" );

			// content
			for( int row = 0; row < table.getRowCount(); row++ )
			{
				for( int column = 0; column < table.getColumnCount(); column++)
				{
					bfw.write( table.getValueAt(row, column) + "\t" );
				}
				bfw.write( "\n" );
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return;
		}


	}
}
