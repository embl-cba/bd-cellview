package de.embl.cba.tables;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Plugin(type = Command.class, initializer = "init")
public class ObjectTableCoordinateColumnsSelectionCommand extends DynamicCommand
{
	@Parameter ()
	File tableFile;
	private JTable jTable;
	private ArrayList< String > columnNames;

	@Override
	public void run()
	{
		final ObjectTablePanel objectTablePanel = new ObjectTablePanel( jTable );

		for ( ObjectCoordinate objectCoordinate : ObjectCoordinate.values() )
		{
			objectTablePanel.setObjectCoordinateColumn(
					objectCoordinate,
					getCoordinateColumnIndex( objectCoordinate ) );
		}

		objectTablePanel.showPanel();
	}

	public void init() throws IOException
	{
		jTable = TableUtils.loadTable( tableFile, "\t" );

		initColumnNames();

		addUserInterfaceParameters();
	}

	private void addUserInterfaceParameters()
	{
		for ( ObjectCoordinate objectCoordinate : ObjectCoordinate.values())
		{
			addCoordinateParameter( objectCoordinate, columnNames );
		}
	}

	private void initColumnNames() throws IOException
	{
		columnNames = new ArrayList<>( );
		columnNames.add( "None" );
		columnNames.addAll( TableUtils.getColumnNames( jTable ) );
	}

	public static String getCoordinateParameterName( ObjectCoordinate objectCoordinate )
	{
		return "ColumnIndex" + objectCoordinate;
	}

	private void addCoordinateParameter( ObjectCoordinate objectCoordinate, ArrayList< String > columnNames )
	{
		final MutableModuleItem< String > axisItem = addInput( getCoordinateParameterName( objectCoordinate ), String.class );
		axisItem.setChoices( columnNames );
		axisItem.setValue(this, columnNames.get( 0 ) );
		axisItem.setPersisted( true );
	}

	private Integer getCoordinateColumnIndex( ObjectCoordinate objectCoordinate )
	{
		final String selectedColumnName = ( String ) getInput( getCoordinateParameterName( objectCoordinate ) );

		// -1 is subtracted here, because in initColumnNames()
		// "None" was added in front of the actual column names;
		final int coordinateColumnIndex = columnNames.indexOf( selectedColumnName ) - 1;

		return coordinateColumnIndex;
	}

}
