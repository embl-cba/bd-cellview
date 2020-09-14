package de.embl.cba.fccf;

import de.embl.cba.tables.Tables;
import ij.IJ;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.File;
import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process Images from Table" )
@Deprecated
public class BDOpenTableCommandDeprecated implements Command
{
	@Parameter
	public LogService logService;

	@Parameter
	public CommandService commandService;

	@Parameter ( label = "Image Table" )
	public File imageTableFile;

	@Parameter ( label = "Glimpse Table", callback = "logTableInfo" )
	public Button logTableInfo;

	@Parameter ( label = "Image Path Column Name" )
	public String imagePathColumnName = "path";

	@Parameter ( label = "Gate Column Name" )
	public String gateColumnName = "gate";

	private JTable jTable;

	private String recentImageTablePath = "";

	public void run()
	{
		loadTable();

		BDImageViewingAndSavingCommandDeprecated.jTable = jTable;
		BDImageViewingAndSavingCommandDeprecated.tableFile = imageTableFile;
		BDImageViewingAndSavingCommandDeprecated.imagePathColumnName = imagePathColumnName;
		BDImageViewingAndSavingCommandDeprecated.gateColumnName = gateColumnName;

		commandService.run( BDImageViewingAndSavingCommandDeprecated.class, true );
	}

	private void loadTable()
	{
		if ( ! imageTableFile.exists() )
		{
			logService.error( "Table file does not exist: " + imageTableFile );
			throw new UnsupportedOperationException( "Could not open file: " + imageTableFile);
		}

		if ( recentImageTablePath.equals( imageTableFile.getAbsolutePath() ) ) return;

		final long currentTimeMillis = System.currentTimeMillis();
		IJ.log("Loading table; please wait...");
		jTable = Tables.loadTable( imageTableFile.getAbsolutePath() );
		IJ.log( "Loaded table in " + ( System.currentTimeMillis() - currentTimeMillis ) + " ms." );

		recentImageTablePath = imageTableFile.getAbsolutePath();
	}

	public void logTableInfo( )
	{
		loadTable();

		glimpseTable( jTable );
	}

	// TODO: move this to the Tables class
	public static void glimpseTable( JTable jTable )
	{
		IJ.log( "# Table Info"  );
		IJ.log( "Number of rows: " + jTable.getRowCount() );
		final List< String > columnNames = Tables.getColumnNames( jTable );
		for ( String columnName : columnNames )
		{
			final int columnIndex = jTable.getColumnModel().getColumnIndex( columnName );

			String firstRows = "";
			for ( int rowIndex = 0; rowIndex < 5; rowIndex++ )
			{
				firstRows += jTable.getValueAt( rowIndex, columnIndex );
				firstRows += ", ";
			}
			firstRows += "...";

			IJ.log( columnName + ": " + firstRows );
		}
	}
}
