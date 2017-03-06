package com.softsynth.javasonics.util;

/** Conversion routines for ULaw audio compression format.
 * 
 * @author Phil Burk (C) 2004
 */
public class ULaw_Codec
{
	private static final int MAX_MAGNITUDE = 32635;
	private static final int BIAS = 0x84;

	private static final int SIGN_BIT = (0x80);
	/* Sign bit for a A-law byte. */
	private static final int QUANT_MASK = (0xf); /* Quantization field mask. */
	private static final int SEG_SHIFT = (4);
	/* Left shift for segment number. */
	private static final int SEG_MASK = (0x70); /* Segment field mask. */

	protected static final int expUlaw[] = new int[256];

	// Fill exponent array at class load time.
	static {
		for (int i = 0; i < 256; i++)
		{
			int r = i / 2;
			int n = 0;
			while (r >= 1)
			{
				n++;
				r /= 2;
			}
			expUlaw[i] = n;
		}
	}

	/**
	 * Convert 16 bit signed sample to ULaw format.
	 */

	public static final byte convertLinearToULaw(int sample)
	{
		int sign = 0;

		if (sample < 0)
		{
			sign = 0x0080;
			sample = -sample; // absolute value
		}

		if (sample > MAX_MAGNITUDE)
		{
			sample = MAX_MAGNITUDE; // Clip to maximum level we can handle.
		}

		sample = sample + BIAS;
		int exponent = expUlaw[(sample >> 7) & 0x00FF];
		int mantissa = (sample >> (exponent + 3)) & 0x0F;
		byte ulawbyte = (byte) (~(sign | (exponent << 4) | mantissa));
		if (ulawbyte == 0)
		{
			ulawbyte = 0x02;
		}
		return ulawbyte;
	}

	public static int convertULawToLinear(int u_val)
	{
		int t;
		u_val = (~u_val) & 0x00FF; // make sure it is unsigned

		/*
		 * Extract and bias the quantization bits. Then
		 * shift up by the segment number and subtract out the bias.
		 */
		t = ((u_val & QUANT_MASK) << 3) + BIAS;
		t = t << ((u_val & SEG_MASK) >> SEG_SHIFT);

		return (((u_val & SIGN_BIT) != 0) ? (BIAS - t) : (t - BIAS));
	}

}
