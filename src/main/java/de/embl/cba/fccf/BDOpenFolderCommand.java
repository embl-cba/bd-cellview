package de.embl.cba.fccf;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.io.File;

//@Plugin(type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process Images from Folder" )
@Deprecated
public class BDOpenFolderCommand implements Command
{
	@Parameter
	public LogService logService;

	@Parameter
	public CommandService commandService;

	@Parameter ( label = "Input images directory", style = "directory" )
	public File inputImagesDirectory;

	public void run()
	{
		BDImageViewingAndSavingCommandDeprecated.jTable = null;
		BDImageViewingAndSavingCommandDeprecated.inputImagesDirectory = inputImagesDirectory;

		commandService.run( BDImageViewingAndSavingCommandDeprecated.class, true );
	}
}
