package de.embl.cba.metadata;

import loci.formats.IFormatReader;
import loci.formats.meta.IMetadata;

import java.util.LinkedHashMap;
import java.util.Map;

public class MetaData
{
	public static final String IMAGE_DIMENSIONS = "Image dimensions";
	public static final String ENTITY = "Entity";
	public static final String LABEL = "Label";

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

		addDimensions();
		addChannels();
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
