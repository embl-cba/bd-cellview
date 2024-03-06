package de.embl.cba.cellview.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.embl.cba.cellview.CellViewChannel;
import de.embl.cba.cellview.CellViewImageProcessor;
import de.embl.cba.cellview.CellViewUtils;
import de.embl.cba.cluster.Commands;
import de.embl.cba.cluster.PathMapper;
import de.embl.cba.morphometry.Logger;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;

import static de.embl.cba.cellview.CellViewUtils.checkFileSize;
import static de.embl.cba.cellview.CellViewUtils.localDateAndHourAndMinute;

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
