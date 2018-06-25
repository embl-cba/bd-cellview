package de.embl.cba.drosophila.shavenbaby;

public class ShavenBabyRegistrationSettings
{
	// all spatial values are in micrometer

	public int shavenBabyChannelIndex = 0;
	public boolean showIntermediateResults = false;
	public double refractiveIndexScalingCorrectionFactor = 1.6;
	public double registrationResolution = 8.0;
	public double outputResolution = 1.0;
	public double backgroundIntensity = 3155; // TODO: determine from image (maybe min value after averaging)
	public double refractiveIndexIntensityCorrectionDecayLength = 170;
	public double minDistanceToAxisForRollAngleComputation = 6;
	public double drosophilaRadius = 80;

	public double thresholdAfterOffsetSubtraction = 500;
	public double closingRadius = 16;


}
