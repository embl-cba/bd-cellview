package de.embl.cba.fccf.devel.record;

import de.embl.cba.fccf.devel.callback.TestCallbackCommand;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

//@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Test Some Command")
public class TestCommand extends DynamicCommand implements Initializable
{
	public static TestCallbackCommand testCallbackCommand;

	@Parameter( label = "Selected File" )
	String someString;

	@Parameter( label = "Other stuff" )
	String string = "hello!";

	@Override
	public void run()
	{

	}

	@Override
	public void initialize()
	{
		getInfo(); // HACK: Workaround for bug in SJC.
		final MutableModuleItem<String> someStringItem = getInfo().getMutableInput("someString", String.class);

		someStringItem.setValue( this, testCallbackCommand.file.getAbsolutePath() );
	}

}
