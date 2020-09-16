package de.embl.cba.imflow.devel.callback;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.IJ;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

import java.io.File;

//@Plugin( type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Command")
public class TestSomeCommand implements Command
{
	@Parameter( label = "Selected File" )
	public File file;

	@Parameter( label = "Other stuff" )
	public String string = "hello!";

//	private HashMap< String, ArrayList< Integer > > gateToRows;
	private transient ImagePlus processedImp;
//	private int gateColumnIndex;
//	private int pathColumnIndex;
//	private String experimentDirectory;
//	private JTable jTable;
//	private String recentImageTablePath = "";
//	private File inputImagesDirectory;
	private transient File outputImagesRootDirectory;
	private transient String imagePathColumnName = "path";


	@Override
	public void run()
	{
		Gson gson = new GsonBuilder().create(); //.setPrettyPrinting()
		final String json = gson.toJson( this );
		IJ.log( json );
	}
}
