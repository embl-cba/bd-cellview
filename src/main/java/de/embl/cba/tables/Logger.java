package de.embl.cba.tables;

import ij.IJ;

public class Logger
{
	public static void warn( String s )
	{
		IJ.showMessage( s );
	}

	public static void error( String s )
	{
		IJ.showMessage( s );
	}
}
