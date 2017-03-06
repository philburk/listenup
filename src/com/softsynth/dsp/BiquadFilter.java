package com.softsynth.dsp;

/**
 * 2nd order digital IIR filter.
 * This filter can be used as a lowPass, highPass, bandPass,
 * notch or other types of filter by changing the coefficients.
 * The coefficients are based on formulae provided by Robert Bristow-Johnson (RBJ)
 * to the music-dsp mail list.
 * We have optimized the IIR to avoid dividing by a coefficient.
<pre>
	Here is the equation for this filter:
	 y(n) = A0*x(n) + A1*x(n-1)  + A2*x(n-2) - B1*y(n-1)  - B2*y(n-2)
	 
	Here is the equation that Robert Bristow-Johnson uses:
	 y[n] = (b0/a0)*x[n] + (b1/a0)*x[n-1] + (b2/a0)*x[n-2]
	      - (a1/a0)*y[n-1] - (a2/a0)*y[n-2]
	
	So to translate between Our coefficients and RBJ coefficients:
	Us       RBJ
	A0       b0/a0
	A1       b1/a0
	A2       b2/a0
	B1       a1/a0
	B2       a2/a0
</pre>
 *
 * @author Phil Burk (C) 2003
 */
public abstract class BiquadFilter extends BufferedSignalProcessor {

	// filter coefficients
	protected double A0;
	protected double A1;
	protected double A2;
	protected double B1;
	protected double B2;
	// input and output delay lines
	private double X1;
	private double X2;
	private double Y1;
	private double Y2;
	// intermediate calculations
	double cos_omega;
	double sin_omega;
	double alpha;

	private double normalizedFrequency;
	private double Q;

	public BiquadFilter() {
		setNormalizedFrequency(0.1);
		setQ(1.0);
	}
	/**
	* Calculate coefficients common to many parametric biquad filters.
	*/
	protected void calcCommon() {
		double omega;
		omega = 2.0 * Math.PI * normalizedFrequency;
		cos_omega = Math.cos(omega);
		sin_omega = Math.sin(omega);
		alpha = sin_omega / (2.0 * Q);
	}

	public void process(float[] input, float[] output)
	{
		for (int i = 0; i < input.length; i++) {
			double X0 = input[i];
			double Y0 =
				(A0 * X0) + (A1 * X1) + (A2 * X2) - (B1 * Y1) - (B2 * Y2);
			output[i] = (float) Y0;
			X2 = X1;
			X1 = X0;
			Y2 = Y1;
			Y1 = Y0;
		}

		// Apply a small bipolar impulse to filter to prevent arithmetic underflow.
		Y1 += 1.0e-26;
		Y2 += -1.0e-26;
	}

	/* Perform core IIR filter calculation.
	 */
	public void write(float[] data, int offset, int numSamples)
	{
		for (int i = 0; i < numSamples; i++) {
			double X0 = data[i+offset];
			double Y0 =
				(A0 * X0) + (A1 * X1) + (A2 * X2) - (B1 * Y1) - (B2 * Y2);
			output((float) Y0);
			X2 = X1;
			X1 = X0;
			Y2 = Y1;
			Y1 = Y0;
		}

		// Apply a small bipolar impulse to filter to prevent arithmetic underflow.
		Y1 += 1.0e-26;
		Y2 += -1.0e-26;
	}

	/**
	 * Set center or cutoff frequency, depending on filter type.
	 * @normalizedFrequency = frequency / sampleRate
	 */
	public void setNormalizedFrequency(double normalizedFrequency) {
		// Don't get close to Nyquist because it will blow up.
		if (normalizedFrequency >= 0.499f) {
			normalizedFrequency = 0.499f;
		}
		this.normalizedFrequency = normalizedFrequency;
		updateCoefficients();
	}

	/**
	 * Calculate coefficients based on current frequency and Q.
	 * The implementation will determine the fileter type, eg. lowPass, highPass.
	 */
	protected abstract void updateCoefficients();

	/**
	 * @return normalizedFrequency = frequency / sampleRate
	 */
	public double getNormalizedFrequency() {
		return normalizedFrequency;
	}
	/**
	 * @return Q
	 */
	public double getQ() {
		return Q;
	}

	/**
	 * @param Q inverse of bandwidth, normally 1.0
	 */
	public void setQ(double Q) {
		this.Q = Q;
		updateCoefficients();
	}

}
