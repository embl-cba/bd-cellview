package de.embl.cba.drosophila.geometry;

import net.imglib2.Point;
import net.imglib2.RealPoint;

import java.util.ArrayList;

public class CentroidsParameters
{
	public ArrayList< Double > axisCoordinates;
	public ArrayList< Double > angles;
	public ArrayList< Double > distances;
	public ArrayList< RealPoint > centroids;
	public ArrayList< Double > numVoxels;


	public CentroidsParameters( )
	{
		this.axisCoordinates = new ArrayList<>(  );
		this.angles = new ArrayList<>(  );
		this.distances = new ArrayList<>(  );
		this.centroids = new ArrayList<>(  );
		this.numVoxels = new ArrayList<>(  );
	}
}
