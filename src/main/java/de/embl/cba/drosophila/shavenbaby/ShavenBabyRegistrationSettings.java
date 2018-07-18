package de.embl.cba.drosophila.shavenbaby;

public class ShavenBabyRegistrationSettings
{
	// all spatial values are in micrometer

	public int shavenBabyChannelIndexOneBased = 1;
	public boolean showIntermediateResults = false;
	public double refractiveIndexScalingCorrectionFactor = 1.6;
	public double registrationResolution = 8.0;
	public double outputResolution = 2.0;
	public double backgroundIntensity = 3155; // TODO: determine from image (maybe min value after averaging)
	public double refractiveIndexIntensityCorrectionDecayLength = 170;
	public double minDistanceToAxisForRollAngleComputation = 5;
	public double watershedSeedsDistanceThreshold = 50;
	public double thresholdAfterBackgroundSubtraction = 500;
	public double closingRadius = 16;


}
