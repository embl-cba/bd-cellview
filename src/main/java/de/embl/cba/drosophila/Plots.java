package de.embl.cba.drosophila;

import ij.gui.Plot;

import java.util.ArrayList;

public class Plots
{
	public static void plot( double[] xValues , double[] yValues )
	{
		Plot plot = new Plot("title","x", "y",  xValues, yValues );
		plot.show();
	}


	public static void plot( ArrayList< Double > xValues , ArrayList< Double >  yValues, String xLab, String yLab )
	{
		Plot plot = new Plot("", xLab, yLab,  xValues.stream().mapToDouble(d -> d).toArray(), yValues.stream().mapToDouble(d -> d).toArray() );
		plot.show();
	}

}

