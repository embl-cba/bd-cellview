package de.embl.cba.tables.objects.grouping;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

public class Grouping
{
	final JTable table;
	final Integer objectLabelColumnIndex;
	final Integer groupingColumnIndex;
	final private TableModel model;
	private final TreeMap< Number, Integer > labelRowMap;

	public Grouping( JTable table, Integer objectLabelColumnIndex, Integer groupingColumnIndex )
	{
		this.table = table;
		this.model = table.getModel();
		this.objectLabelColumnIndex = objectLabelColumnIndex;
		this.groupingColumnIndex = groupingColumnIndex;

		// create TreeMap for fast lookup in which row a given object is
		this.labelRowMap = new TreeMap<>();
		initLabelRowMap();
	}

	private void initLabelRowMap( )
	{
		final int rowCount = table.getRowCount();

		for ( int row = 0; row < rowCount; row++ )
		{
			final Number label = (Number) model.getValueAt( row, objectLabelColumnIndex );
			labelRowMap.put( label, row );
		}
	}

	public void assignObjectsToGroup( final Collection< ? extends Number > objectLabels, final Object group )
	{
		for ( Number objectLabel : objectLabels )
		{
			assignObjectToGroup( objectLabel, group );
		}
	}

	private void assignObjectToGroup( Number objectLabel, Object group )
	{
		model.setValueAt( group, labelRowMap.get( objectLabel ), groupingColumnIndex );
	}

}
