package com.softsynth.javasonics.recplay;

import java.io.*;

import org.xiph.speex.*;

import com.softsynth.javasonics.util.Logger;

public class JSpeexDecoder {

	/**
	 * Decodes a speex array to PCM.
	 */
	protected void decode(InputStream speexStream, DynamicRecording recording)
		throws IOException {
		byte[] header = new byte[2048];
		byte[] payload = new byte[65536];
		short[] decdat = new short[44100 * 2];
		final int HEADERSIZE = 27;
		final int SEGOFFSET = 26;
		final String OGGID = "OggS";
		int segments = 0, curseg = 0, i = 0, bodybytes = 0, decsize = 0;
		int packetCount = 0;

		// construct a new decoder
		SpeexDecoder speexDecoder = new SpeexDecoder();
		// open the input stream
		DataInputStream dis = new DataInputStream(speexStream);

		int origchksum;
		int chksum;
		try {
			// read until we get to EOF
			while (true) {
				// read the OGG header
				dis.readFully(header, 0, HEADERSIZE);
				origchksum =
					((header[25] & 0xFF) << 24)
						| ((header[24] & 0xFF) << 16)
						| ((header[23] & 0xFF) << 8)
						| (header[22] & 0xFF);
				header[22] = 0;
				header[23] = 0;
				header[24] = 0;
				header[25] = 0;
				chksum = OggCrc.checksum(0, header, 0, HEADERSIZE);

				// make sure its a OGG header
				String headerID = new String(header, 0, 4);
				Logger.println(3, "JSpeexDecoder: header ID = " + headerID);
				if (!OGGID.equals(headerID))
				{
					throw new IOException("OGG missing ID, got " + headerID );
				}

				/* how many segments are there? */
				segments = header[SEGOFFSET] & 0xFF;
				Logger.println(3, "JSpeexDecoder: numSegments = " + segments);
				dis.readFully(header, HEADERSIZE, segments);
				chksum = OggCrc.checksum(chksum, header, HEADERSIZE, segments);

				/* decode each segment, writing output to DynamicRecording */
				for (curseg = 0; curseg < segments; curseg++) {
					/* get the number of bytes in the segment */
					bodybytes = header[HEADERSIZE + curseg] & 0xFF;
					if (bodybytes == 255) {
						throw new IOException("OGG cannot handle size of 255!");
					}
					dis.readFully(payload, 0, bodybytes);
					chksum = OggCrc.checksum(chksum, payload, 0, bodybytes);

					/* decode the segment */
					speexDecoder.processData(payload, 0, bodybytes);

					/* if first packet, initialize the wave writer with output format */
					if (packetCount == 0) {
						double sampleRate = speexDecoder.getSampleRate();
						recording.setFrameRate(sampleRate);
						Logger.println(
							3,
							"JSpeexDecoder: sampleRate = " + sampleRate);
						int numChannels = speexDecoder.getChannels();
						recording.setSamplesPerFrame(numChannels);
						Logger.println(
							3,
							"JSpeexDecoder: numChannels = " + numChannels);
						recording.erase();
					}

					/* get the amount of decoded data */
					decsize = speexDecoder.getProcessedData(decdat, 0);
					if (decsize > 0) {
						recording.write(decdat, 0, decsize);
					}

					packetCount++;

					gotPacket(bodybytes);
				}
				if (chksum != origchksum)
				{
					throw new IOException("Ogg CheckSums do not match");
				}
			}
		} catch (EOFException eof) {
			Logger.println("JSpeexDecoder: Hit end of Speex file.");
		}
	}

	/**
	 * Decoded another packet.
	 * @param numBytes
	 */
	public void gotPacket(int numBytes)
	{
	}

}