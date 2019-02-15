package de.embl.cba.tables;

import de.embl.cba.tables.objects.ObjectTablePanel;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class UIs
{
	public static void addColumnUI( ObjectTablePanel objectTablePanel )
	{
		final GenericDialog gd = new GenericDialog( "New column" );
		gd.addStringField( "Column name", "MyNewColumn", 30 );
		gd.addStringField( "Default value [String or Number]", "None", 30 );

		gd.showDialog();
		if( gd.wasCanceled() ) return;

		final String columnName = gd.getNextString();

		final String defaultValueString = gd.getNextString();

		Object defaultValue;

		try	{
			defaultValue = Double.parseDouble( defaultValueString );
		}
		catch ( Exception e )
		{
			defaultValue = defaultValueString;
		}

		objectTablePanel.addColumn( columnName, defaultValue );
	}

	public static void saveTableUI( JTable table ) throws IOException
	{
		final JFileChooser jFileChooser = new JFileChooser( "" );

		if ( jFileChooser.showSaveDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			final File selectedFile = jFileChooser.getSelectedFile();

			TableUtils.saveTable( table, selectedFile );
		}
	}


}
