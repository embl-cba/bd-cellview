package de.embl.cba.drosophila;

import ij.gui.Plot;

public class Plots
{
	public static void plot( double[] xValues , double[] yValues )
	{
		Plot plot = new Plot("title","x", "y",  xValues, yValues );
		plot.show();
	}

}

