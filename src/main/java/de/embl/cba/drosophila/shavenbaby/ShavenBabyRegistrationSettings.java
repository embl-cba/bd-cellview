package de.embl.cba.drosophila.shavenbaby;

public class ShavenBabyRegistrationSettings
{
	// all spatial values are in micrometer
	// drosophila length: 420
	// drosophila width: 160

	public static double drosophilaLength = 420;
	public static double drosophilaWidth = 160;

	public int shavenBabyChannelIndexOneBased = 1;
	public boolean showIntermediateResults = false;
	public double refractiveIndexScalingCorrectionFactor = 1.6;
	public double registrationResolution = 6.0;
	public double outputResolution = 2.0;
	public double backgroundIntensity = 3155; // TODO: determine from image (maybe min value after averaging)
	public double refractiveIndexIntensityCorrectionDecayLength = 170;

	public double rollAngleMinDistanceToAxis = 0;
	public double rollAngleMinDistanceToCenter = drosophilaLength / 2.0 * 0.5;
	public double rollAngleMaxDistanceToCenter = drosophilaLength / 2.0 - 10.0;

	public double watershedSeedsGlobalDistanceThreshold = drosophilaWidth / 3.0;
	public double watershedSeedsLocalMaximaDistanceThreshold = 3 * registrationResolution; // at least 3 pixels
	public double thresholdAfterBackgroundSubtraction = 500;
	public double closingRadius = 0;
}
