package de.embl.cba.tables.objects.grouping;

import de.embl.cba.tables.objects.ObjectTablePanel;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.*;

public class Grouping
{
	final ObjectTablePanel objectTablePanel;
	final String groupingColumn;
	final private TableModel model;
	private final JTable table;
	private HashSet< Object > groups;

	public Grouping( ObjectTablePanel objectTablePanel, String groupingColumn )
	{
		this.objectTablePanel = objectTablePanel;
		this.model = objectTablePanel.getTable().getModel();
		this.table = objectTablePanel.getTable();

		this.groupingColumn = groupingColumn;
	}

	public void assignObjectsToGroup( final Set< Double > objectLabels, final Object group )
	{
		for ( Double objectLabel : objectLabels )
		{
			assignObjectToGroup( objectLabel, group );
		}
	}

	private void assignObjectToGroup( Double objectLabel, Object group )
	{
		if ( ! exists( group ) ) groups.add( group );

		model.setValueAt(
				group,
				objectTablePanel.getRow( objectLabel ),
				getGroupingColumnIndex() );
	}

	private int getGroupingColumnIndex()
	{
		return table.getColumnModel().getColumnIndex( groupingColumn );
	}

	public Set< Object > getGroups()
	{
		if ( groups == null ) initGroups();

		return groups;
	}

	public boolean exists( Object group )
	{
		if ( groups == null ) initGroups();

		return groups.contains( group );
	}


	public void initGroups()
	{
		final int rowCount = table.getRowCount();

		groups = new HashSet();

		final int groupingColumnIndex = getGroupingColumnIndex();

		for ( int row = 0; row < rowCount; row++ )
		{
			final Object group = model.getValueAt( row, groupingColumnIndex );
			groups.add( group );
		}
	}

}
