package de.embl.cba.tables.objects.grouping;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.*;

public class Grouping
{
	final JTable table;
	final Integer objectLabelColumnIndex;
	final Integer groupingColumnIndex;
	final private TableModel model;
	private final TreeMap< Object, Integer > labelRowMap;
	private HashSet< Object > groups;

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
			final Object label = model.getValueAt( row, objectLabelColumnIndex );
			labelRowMap.put( label, row );
		}
	}

	public void assignObjectsToGroup( final Collection objectLabels, final Object group )
	{
		for ( Object objectLabel : objectLabels )
		{
			assignObjectToGroup( objectLabel, group );
		}
	}

	private void assignObjectToGroup( Object objectLabel, Object group )
	{
		if ( ! exists( group ) ) groups.add( group );

		model.setValueAt( group, labelRowMap.get( objectLabel ), groupingColumnIndex );
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

		for ( int row = 0; row < rowCount; row++ )
		{
			final Object group = model.getValueAt( row, groupingColumnIndex );
			groups.add( group );
		}
	}

}
