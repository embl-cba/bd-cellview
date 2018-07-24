package de.embl.cba.metadata;

import loci.formats.IFormatReader;
import loci.formats.meta.IMetadata;
import net.imagej.ops.Ops;

import java.util.LinkedHashMap;
import java.util.Map;

public class MetaData
{
	public static final String IMAGE_DIMENSIONS = "Image dimensions";
	public static final String ENTITY = "Entity";
	public static final String LABEL = "Label";
	public static final String TOTAL_DATA_SIZE = "Total data size:";
	public static final String UNKNOWN = "???";
	public static final String TIME_POINTS = "Time points";
	public static final String SPECIES = "Species";
	public static final String GENES = "Genes";

	final IFormatReader reader;
	final IMetadata meta;
	final int series;
	final Map< String, Object > map;

	public MetaData( IFormatReader reader, IMetadata meta, int series )
	{
		this.reader = reader;
		this.meta = meta;
		this.series = series;

		map = new LinkedHashMap<>( );

		addTotalDataSize();
		addDimensions();
		addChannels();
		addTimePoints();
		addPositions();
		map.put( "Imaging method", UNKNOWN );
		map.put( "Developmental stage", UNKNOWN );
		map.put( "Cell line", UNKNOWN );
		
	}

	public void addSpecies()
	{
		final LinkedHashMap< String, Object > species = new LinkedHashMap<>();
		species.put( "Name", UNKNOWN );
		species.put( "Taxon", UNKNOWN );
		map.put( SPECIES, species );
	}


	public void addGenes()
	{
		final LinkedHashMap< String, Object > genes = new LinkedHashMap<>();
		genes.put( "Symbols", UNKNOWN );
		genes.put( "Identifiers", UNKNOWN );
		map.put( GENES, genes );
	}


	public Object addPositions()
	{
		return map.put( "Positions", UNKNOWN );
	}

	public void addTimePoints()
	{
		map.put( TIME_POINTS, UNKNOWN );
	}

	public void addTotalDataSize()
	{
		map.put( TOTAL_DATA_SIZE, UNKNOWN );
	}

	public Map< String, Object > getMap()
	{
		return map;
	}

	public void addDimensions()
	{
		int sizeX = reader.getSizeX();
		int sizeY = reader.getSizeY();
		int sizeZ = reader.getSizeZ();
		int sizeC = reader.getSizeC();
		int sizeT = reader.getSizeT();

		map.put( IMAGE_DIMENSIONS, sizeX + "x" + sizeY + "x" + sizeZ + "x" + sizeC + "x" + sizeT );
	}

	public void addChannels()
	{
		int numChannels = reader.getSizeC();

		Map< String, Object > channels = new LinkedHashMap<>( );

		for ( int channelId = 0; channelId < numChannels; ++channelId )
		{

			Map< String, Object > channelProperties = new LinkedHashMap<>( );
			channelProperties.put( ENTITY, "..." );
			channelProperties.put( LABEL, "..." );

			channels.put( "Channel " + channelId, channelProperties );
		}

		map.put( "Channels", channels );
	}

}
