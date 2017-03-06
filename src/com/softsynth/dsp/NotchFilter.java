package com.softsynth.dsp;

/**
 * Filter that removes frequencies at the centerFrequency.
 * @author Phil Burk (C) 2003
 */
public class NotchFilter extends BiquadFilter {

	/*
	 * @see ecg.BiquadFilter#updateCoefficients()
	 */
	public void updateCoefficients() {
		calcCommon();

		double scalar = 1.0 / (1.0 + alpha);
		double A1_B1_Value = -2.0 * cos_omega * scalar;

		A0 = scalar;
		A1 = A1_B1_Value;
		A2 = scalar;
		B1 = A1_B1_Value;
		B2 = (1.0 - alpha) * scalar;
	}

}
