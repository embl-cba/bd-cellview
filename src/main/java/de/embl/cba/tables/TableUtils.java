package de.embl.cba.tables;

import org.scijava.table.GenericTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.util.*;

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


	public static void saveTableUI( JTable table ) throws IOException
	{
		final JFileChooser jFileChooser = new JFileChooser( "" );

		if ( jFileChooser.showSaveDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			final File selectedFile = jFileChooser.getSelectedFile();

			saveTable( table, selectedFile );
		}
	}

	public static void saveTable( JTable table, File file ) throws IOException
	{
		BufferedWriter bfw = new BufferedWriter( new FileWriter( file ) );

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

	public static JTable loadTable( final File file, String delim ) throws IOException
	{
		ArrayList< String > rows = readRows( file );

		return createJTableFromRows( rows, delim );
	}

	private static ArrayList< String > readRows( File file )
	{
		ArrayList< String > rows = new ArrayList<>();

		try
		{
			FileInputStream fin = new FileInputStream( file );
			BufferedReader br = new BufferedReader( new InputStreamReader( fin ) );

			String aRow;

			while ( ( aRow = br.readLine() ) != null )
			{
				rows.add( aRow );
			}

			br.close();
		} catch ( Exception e )
		{
			e.printStackTrace();
		}
		return rows;
	}


	public static JTable createJTableFromRows( ArrayList< String > rows, String delim )
	{

		DefaultTableModel model = null;


		StringTokenizer st = new StringTokenizer( rows.get( 0 ), delim );

		ArrayList< String > colNames = new ArrayList<>();

		while ( st.hasMoreTokens() )
		{
			colNames.add( st.nextToken() );
		}

		/**
		 * Init model
		 */

		model = new DefaultTableModel()
		{
			@Override
			public Class getColumnClass( int column )
			{
				return Double.class;
			}

			@Override
			public boolean isCellEditable( int row, int column )
			{
				return false;
			}
		};

		/**
		 * Set columns
		 */
		for ( String colName : colNames )
		{
			model.addColumn( colName );
		}


		// extract data
		int numCols = colNames.size();
		final Double[] rowEntries = new Double[ numCols ];
		int iCol;
		String s;

		for ( int iRow = 1; iRow < rows.size(); ++iRow )
		{
			st = new StringTokenizer( rows.get( iRow ), delim );

			iCol = 0;

			while ( st.hasMoreTokens() )
			{
				rowEntries[ iCol++ ] = Double.parseDouble( st.nextToken() );
			}

			model.addRow( rowEntries );

		}

		return new JTable( model );
	}


}
