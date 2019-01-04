package de.embl.cba.tables.models;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class ColumnClassAwareTableModel extends DefaultTableModel
{
	ArrayList< Class > columnClasses;

	@Override
	public Class getColumnClass( int column )
	{
		return columnClasses.get( column );
	}

	@Override
	public boolean isCellEditable( int row, int column )
	{
		return false;
	}

	/**
	 * Determines column classes from entries in 1st row.
	 */
	public void refreshColumnClasses()
	{
		columnClasses = new ArrayList<>(  );

		for ( int column = 0; column < getColumnCount(); column++ )
		{
			final Object value = this.getValueAt( 1, column );

			columnClasses.add( value.getClass() );
		}
	}

}
