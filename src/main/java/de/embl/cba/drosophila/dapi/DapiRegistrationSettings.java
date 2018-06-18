package de.embl.cba.drosophila.dapi;

public class DapiRegistrationSettings
{
	public boolean showIntermediateResults = false;
	public double resolutionDuringRegistrationInMicrometer = 4.0;
	public double threshold = 10;
	public double finalResolutionInMicrometer = 1.0;
	public double refractiveIndexCorrectionAxialScalingFactor = 1.6;
	public int derivativeDeltaInMicrometer = 20;
	public double projectionRangeMinDistanceToCenterInMicrometer = +20.0;
	public double projectionRangeMaxDistanceToCenterInMicrometer = +80.0;
	public double sigmaForBlurringAverageProjectionInMicrometer = 20.0;

}
