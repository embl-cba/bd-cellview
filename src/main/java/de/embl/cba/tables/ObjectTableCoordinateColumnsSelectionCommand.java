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

	@Override
	public void run()
	{
		final ObjectTablePanel objectTablePanel = new ObjectTablePanel( jTable );

		for ( Coordinate coordinate : Coordinate.values() )
		{
			objectTablePanel.setObjectCoordinateColumn( coordinate, getCoordinateColumn( coordinate ) );
		}

		objectTablePanel.showTable();
	}

	public void init() throws IOException
	{
		jTable = TableUtils.loadTable( tableFile, "\t" );

		final ArrayList< String > columnNames = new ArrayList<>( );
		columnNames.add( ObjectTablePanel.NONE );
		columnNames.addAll( TableUtils.getColumnNames( jTable ) );

		for ( Coordinate coordinate : Coordinate.values())
		{
			addCoordinateParameter( coordinate, columnNames );
		}
	}

	public static String getCoordinateParameterName( Coordinate coordinate )
	{
		return "ColumnIndex" + coordinate;
	}

	private void addCoordinateParameter( Coordinate coordinate, ArrayList< String > columnNames )
	{
		final MutableModuleItem< String > axisItem = addInput( getCoordinateParameterName( coordinate ), String.class );
		axisItem.setChoices( columnNames );
		axisItem.setValue(this, columnNames.get( 0 ) );
		axisItem.setPersisted( true );
	}

	private String getCoordinateColumn( Coordinate coordinate )
	{
		return (String) getInput( getCoordinateParameterName( coordinate ) );
	}

}
