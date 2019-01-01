package de.embl.cba.tables;

import de.embl.cba.tables.objects.ObjectTableModel;
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

		return createJTableFromStringList( rows, delim );
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


	public static JTable createJTableFromStringList( ArrayList< String > strings, String delim )
	{

		if ( delim == null )
		{
			if ( strings.get( 0 ).contains( "\t" ) )
			{
				delim = "\t";
			}
			else if ( strings.get( 0 ).contains( "," )  )
			{
				delim = ",";
			}
		}

		StringTokenizer st = new StringTokenizer( strings.get( 0 ), delim );

		ArrayList< String > colNames = new ArrayList<>();

		while ( st.hasMoreTokens() )
		{
			colNames.add( st.nextToken() );
		}

		/**
		 * Init model and columns
		 */

		ObjectTableModel model = new ObjectTableModel();

		for ( String colName : colNames )
		{
			model.addColumn( colName );
		}

		int numCols = colNames.size();

		/**
		 * Add rows entries
		 */

		for ( int iString = 1; iString < strings.size(); ++iString )
		{
			model.addRow( new Object[ numCols ] );

			st = new StringTokenizer( strings.get( iString ), delim );

			for ( int iCol = 0; iCol < numCols; iCol++ )
			{
				String stringValue = st.nextToken();

				try
				{
					final Double numericValue = Double.parseDouble( stringValue );
					model.setValueAt( numericValue, iString - 1, iCol );
				}
				catch ( Exception e )
				{
					model.setValueAt( stringValue, iString - 1, iCol );
				}
			}

		}

		model.setColumnClassesFromFirstRow();

		return new JTable( model );
	}


	public static ArrayList< String > getColumnNames( JTable jTable )
	{
		final ArrayList< String > columnNames = new ArrayList<>();

		for ( int columnIndex = 0; columnIndex < jTable.getColumnCount(); columnIndex++ )
		{
			columnNames.add( jTable.getColumnName( columnIndex ) );
		}
		return columnNames;
	}

	public static < A, B > TreeMap< A, B > columnsAsTreeMap(
			JTable table,
			String fromColumn,
			String toColumn )
	{

		final int fromColumnIndex = table.getColumnModel().getColumnIndex( fromColumn );
		final int toColumnIndex = table.getColumnModel().getColumnIndex( toColumn );

		final TreeMap< A, B > treeMap = new TreeMap();

		for ( int row = 0; row < table.getRowCount(); row++ )
		{
			treeMap.put( ( A ) table.getValueAt( row, fromColumnIndex ), ( B ) table.getValueAt( row, toColumnIndex ) );
		}

		return treeMap;
	}

	public static double columnMin( JTable jTable, int col )
	{
		double min = Double.MAX_VALUE;

		final int rowCount = jTable.getRowCount();

		for ( int row = 0; row < rowCount; row++ )
		{
			final Double valueAt = ( Double ) jTable.getValueAt( row, col );

			if ( valueAt < min )
			{
				min = valueAt;
			}
		}

		return min;
	}

	public static double columnMax( JTable jTable, int col )
	{
		double max =  - Double.MAX_VALUE;

		final int rowCount = jTable.getRowCount();

		for ( int row = 0; row < rowCount; row++ )
		{
			final Double valueAt = ( Double ) jTable.getValueAt( row, col );

			if ( valueAt > max )
			{
				max = valueAt;
			}
		}

		return max;
	}

}
