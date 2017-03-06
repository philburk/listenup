package com.softsynth.dsp;

/**
 * Filter that removes frequencies higher than the centerFrequency.
 * @author Phil Burk (C) 2003
 */
public class LowpassFilter extends BiquadFilter {

	/*
	 * @see ecg.BiquadFilter#updateCoefficients()
	 */
	public void updateCoefficients() {
		calcCommon();

		double scalar = 1.0 / (1.0 + alpha);
		double omc = (1.0 - cos_omega);
		double A0_A2_Value = omc * 0.5 * scalar;
		A0 = A0_A2_Value;
		A1 = omc * scalar;
		A2 = A0_A2_Value;
		B1 = -2.0 * cos_omega * scalar;
		B2 = (1.0 - alpha) * scalar;
	}

}
