package de.embl.cba.cellview;

import ij.ImagePlus;

public class CellViewChannel
{
	final public int sliceIndex;
	final public String color;
	final public double[] contrastLimits;
	public ImagePlus imagePlus;

	public CellViewChannel( int sliceIndex, String color, double[] contrastLimits )
	{
		this.sliceIndex = sliceIndex;
		this.color = color;
		this.contrastLimits = contrastLimits;
	}
}
