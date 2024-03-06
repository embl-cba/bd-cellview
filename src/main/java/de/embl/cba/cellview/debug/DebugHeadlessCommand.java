package de.embl.cba.cellview.debug;

import ij.IJ;
import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class, menuPath = "Plugins>BD CellView>Debug Headless"  )
public class DebugHeadlessCommand implements Command, Interactive
{
	@Parameter ( label = "Text" )
	public String text;

	public void run()
	{
		System.out.println( "You entered: " + text );

		long start = System.currentTimeMillis();
		for ( int i = 0; i < 1000; i++ )
		{
			System.out.println("Iteration " + i + ", time running [ms] = " + (System.currentTimeMillis() - start ) );
			IJ.wait( 200 );
		}
	}
}
