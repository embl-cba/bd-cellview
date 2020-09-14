package de.embl.cba.fccf.devel.callback;

import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

//@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Command")
public class TestSomeCommand extends DynamicCommand
{
	@Parameter( label = "Selected File" )
	File file;

	@Parameter( label = "Other stuff" )
	String string = "hello!";

	@Override
	public void run()
	{

	}
}
