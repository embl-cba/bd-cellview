package de.embl.cba.cellview.devel.callback;


import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;

import java.io.File;

//@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Test Callback")
public class TestCallbackCommand extends DynamicCommand
{
	public static CommandService cs;

	@Parameter( label = "file", callback = "callback" )
	public File file;
	private File recentFile;

	// one needs two parameters otherwise the command will be executed immediately
	@Parameter( label = "hello" )
	private String string = "world";


	@Override
	public void run()
	{

	}

	private void callback()
	{
		if ( file == recentFile ) return;
		recentFile = file;
//		TestSomeCommand.testCallbackCommand = this;
//
//		cs.run( TestSomeCommand.class, true );
	}
}
