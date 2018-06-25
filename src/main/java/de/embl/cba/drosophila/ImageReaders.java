package de.embl.cba.drosophila;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import java.io.IOException;

public class ImageReaders
{

	public static ImagePlus openWithBioFormats( String path ) throws IOException, FormatException
	{

		ImporterOptions opts = new ImporterOptions();
		opts.setId( path );
		opts.setVirtual( true );

		ImportProcess process = new ImportProcess( opts );
		process.execute();

		ImagePlusReader impReader = new ImagePlusReader( process );

		ImagePlus[] imps = impReader.openImagePlus();
		return imps[ 0 ];
	}
}
