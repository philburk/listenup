/******************************************************************************
 *                                                                            *
 * Copyright (c) 1999-2003 Wimba S.A., All Rights Reserved.                   *
 *                                                                            *
 * COPYRIGHT:                                                                 *
 *      This software is the property of Wimba S.A.                           *
 *      This software is redistributed under the Xiph.org variant of          *
 *      the BSD license.                                                      *
 *      Redistribution and use in source and binary forms, with or without    *
 *      modification, are permitted provided that the following conditions    *
 *      are met:                                                              *
 *      - Redistributions of source code must retain the above copyright      *
 *      notice, this list of conditions and the following disclaimer.         *
 *      - Redistributions in binary form must reproduce the above copyright   *
 *      notice, this list of conditions and the following disclaimer in the   *
 *      documentation and/or other materials provided with the distribution.  *
 *      - Neither the name of Wimba, the Xiph.org Foundation nor the names of *
 *      its contributors may be used to endorse or promote products derived   *
 *      from this software without specific prior written permission.         *
 *                                                                            *
 * WARRANTIES:                                                                *
 *      This software is made available by the authors in the hope            *
 *      that it will be useful, but without any warranty.                     *
 *      Wimba S.A. is not liable for any consequence related to the           *
 *      use of the provided software.                                         *
 *                                                                            *
 * Class: OggSpeexWriter.java                                                 *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 *                                                                            *
 * Date: 9th April  2003                                                      *
 *                                                                            *
 ******************************************************************************/

/* $Id: OggSpeexWriter.java,v 1.2 2013/10/21 06:46:23 phil Exp $ */

package org.xiph.speex;

import java.io.*;

import java.util.Random;

/**
 * Ogg Speex Writer
 *
 * @author Marc Gimpel, wimba.com
 */
public class OggSpeexWriter
{
  private Random random;
// PLB June 3, 2003 - Added OutputStream option.
//     Left RandomAccessFile but it may not be needed.
//     Prevents use in Applets because of File security.
// PLB 4/7/06 - Changed version text from "0.2 beta" to "0.6"
  private RandomAccessFile raf;
  private OutputStream outStream;

  private int    mode;
  private int    sampleRate;
  private int    channels;
//  private int    size; // PLB removed, not used
  private int    streamSerialNumber;
  private byte[] dataBuffer;
  private int    dataBufferPtr;
  private byte[] headerBuffer;
  private int    headerBufferPtr;
  private int    pageCount;
  private int    packetCount;
  private long   granulepos;

  /**
   * Constructor.
   */
  public OggSpeexWriter()
  {
    random             = new Random();
    streamSerialNumber = random.nextInt();
    dataBuffer         = new byte[65565];
    dataBufferPtr      = 0;
    headerBuffer       = new byte[255];
    headerBufferPtr    = 0;
    pageCount          = 0;
    packetCount        = 0;
    granulepos         = 0;
  }

  /**
   * Associate an output stream with this writer.
   * @param stream - OutputStream that will be written to.
   */
  public OggSpeexWriter( OutputStream stream )
  {
      this();
      outStream = stream;
  }

  /**
   * Closes the output file.
   */
  public void close()
    throws IOException
  {
    flush(true);
    if( raf != null ) raf.close();
  }

  /**
   * Open the output file.
   * @param filename - file to open.
   */
  private void open(String filename)
    throws IOException
  {
      if( outStream != null )
      {
          throw new IOException("OutputStream already open for this writer.");
      }
    new File(filename).delete();
    raf = new RandomAccessFile(filename, "rw");
  }

/** Write to either the RandomAccessFile or the OutputStream */
  private void write( byte[] data )
    throws IOException
  {
      if( raf != null )
      {
          raf.write( data );
      }
      else
      {
          outStream.write( data );
      }
  }

  private void write( byte[] data, int offset, int len )
    throws IOException
  {
      if( raf != null )
      {
          raf.write( data, offset, len );
      }
      else
      {
          outStream.write( data, offset, len );
      }
  }
  /**
   * Sets the output format.
   * Must be called before WriteHeader().
   */
  public void setFormat(int mode, int sampleRate, int channels)
  {
    this.mode       = mode;
    this.sampleRate = sampleRate;
    this.channels   = channels;
  }

  /**
   *  Writes the header pages that start the Ogg Speex file.
   *  Prepares file for data to be written.
   */
  public void writeHeader()
    throws IOException
  {
    // writes the OGG header page
    ByteArrayOutputStream baos = new ByteArrayOutputStream(108);
    baos.write("OggS".getBytes(), 0, 4); //  0 -  3: capture_pattern
    baos.write(0xff & 0);                //       4: stream_structure_version
    baos.write(0xff & 2);                //       5: header_type_flag (2=bos: beginning of sream)
    writeLong(baos, 0);                  //  6 - 13: absolute granule position
    writeInt(baos, streamSerialNumber);  // 14 - 17: stream serial number
    writeInt(baos, pageCount++);         // 18 - 21: page sequence no
    writeInt(baos, 0);                   // 22 - 25: page checksum
    baos.write(0xff & 1);                //      26: page_segments
    baos.write(0xff & 80);               //      27: segment_table (1 segment, size 80 = Speex Header)
    /* writes the Speex header */
    baos.write("Speex   ".getBytes(), 0, 8);  //  0 -  7: speex_string
    baos.write("speex-1.0".getBytes(), 0, 9); //  8 - 27: speex_version
    baos.write(new byte[11], 0, 11);          //        : speex_version (fill in up to 20 bytes)
    writeInt(baos, 1);                        // 28 - 31: speex_version_id
    writeInt(baos, 80);                       // 32 - 35: header_size
    writeInt(baos, sampleRate);               // 36 - 39: rate
    writeInt(baos, mode);                     // 40 - 43: mode (0=narrowband, 1=wb, 2=uwb)
    writeInt(baos, 4);                        // 44 - 47: mode_bitstream_version
    writeInt(baos, channels);                 // 48 - 51: nb_channels
    writeInt(baos, -1);                       // 52 - 55: bitrate
    writeInt(baos, 160);                      // 56 - 59: frame_size
    writeInt(baos, 0);                        // 60 - 63: vbr
    writeInt(baos, 1);                        // 64 - 67: frames_per_packet
    writeInt(baos, 0);                        // 68 - 71: extra_headers
    writeInt(baos, 0);                        // 72 - 75: reserved1
    writeInt(baos, 0);                        // 76 - 79: reserved2
    /* Calculate Checksum */
    byte[] ogg = baos.toByteArray();
    int chksum = OggCrc.checksum(0, ogg, 0, ogg.length);
    ogg[22] = (byte)(0xff & chksum);
    ogg[23] = (byte)(0xff & (chksum >>>  8));
    ogg[24] = (byte)(0xff & (chksum >>> 16));
    ogg[25] = (byte)(0xff & (chksum >>> 24));
    write(ogg);
    /* writes the OGG comment page */
    baos = new ByteArrayOutputStream(64);
    String comment = "Encoded with jspeex 0.6";
    baos.write("OggS".getBytes(), 0, 4); //  0 -  3: capture_pattern
    baos.write(0xff & 0);                //       4: stream_structure_version
    baos.write(0xff & 0);                //       5: header_type_flag
    writeLong(baos, 0);                  //  6 - 13: absolute granule position
    writeInt(baos, streamSerialNumber);  // 14 - 17: stream serial number
    writeInt(baos, pageCount++);         // 18 - 21: page sequence no
    writeInt(baos, 0);                   // 22 - 25: page checksum
    baos.write(0xff & 1);                //      26: page_segments
    baos.write(0xff & (comment.length()+8)); //      27: segment_table (1 segment, size 80 = Speex Header)
    /* writes the Comment */
    writeInt(baos, comment.length());                    // comment size
    baos.write(comment.getBytes(), 0, comment.length()); // comment
    writeInt(baos, 0);                                   // ?
    /* Calculate Checksum */
    ogg = baos.toByteArray();
    chksum = OggCrc.checksum(0, ogg, 0, ogg.length);
    ogg[22] = (byte)(0xff & chksum);
    ogg[23] = (byte)(0xff & (chksum >>>  8));
    ogg[24] = (byte)(0xff & (chksum >>> 16));
    ogg[25] = (byte)(0xff & (chksum >>> 24));
    write(ogg);
  }

  /**
   *  Writes a packet of audio.
   *  @param data audio data
   */
  public void writePacket(byte[] data, int offset, int len)
    throws IOException
  {
    if (len <= 0) { // nothing to write
      return;
    }
    if (packetCount > 250) { // this mustn't go beyond 255
      flush(false);
    }
    System.arraycopy(data, offset, dataBuffer, dataBufferPtr, len);
    dataBufferPtr += len;
    headerBuffer[headerBufferPtr++]=(byte)len;
    packetCount++;
    granulepos += (mode==0 ? 160 : 320);
  }

  /**
   * flush the Ogg page out of the buffers into the file.
   */
  public void flush(boolean eos)
    throws IOException
  {
    /* writes the OGG header page */
    ByteArrayOutputStream baos = new ByteArrayOutputStream(284);
    baos.write("OggS".getBytes(), 0, 4); //  0 -  3: capture_pattern
    baos.write(0xff & 0);                //       4: stream_structure_version
    baos.write(0xff & (eos ? 4 : 0));    //       5: header_type_flag (4=eos: end of sream)
    writeLong(baos, granulepos);         //  6 - 13: absolute granule position
    writeInt(baos, streamSerialNumber);  // 14 - 17: stream serial number
    writeInt(baos, pageCount++);         // 18 - 21: page sequence no
    writeInt(baos, 0);                   // 22 - 25: page checksum
    baos.write(0xff & packetCount);      //      26: page_segments
    baos.write(headerBuffer, 0, packetCount); // 27 -  x: segment_table (1 segment, size 80 = Speex Header)
    /* Calculate Checksum */
    byte[] ogg = baos.toByteArray();
    int chksum = OggCrc.checksum(0, ogg, 0, ogg.length);
    chksum = OggCrc.checksum(chksum, dataBuffer, 0, dataBufferPtr);
    ogg[22] = (byte)(0xff & chksum);
    ogg[23] = (byte)(0xff & (chksum >>>  8));
    ogg[24] = (byte)(0xff & (chksum >>> 16));
    ogg[25] = (byte)(0xff & (chksum >>> 24));
    write(ogg);
    write(dataBuffer, 0, dataBufferPtr);
    dataBufferPtr   = 0;
    headerBufferPtr = 0;
    packetCount     = 0;
  }

  /**
   * Writes a Little-endian short
   */
  private static void writeShort(OutputStream os, short v)
    throws IOException
  {
    os.write((0xff & v));
    os.write((0xff & (v >>> 8)));
  }

  /**
   * Writes a Little-endian int
   */
  private static void writeInt(OutputStream os, int v)
    throws IOException
  {
    os.write(0xff & v);
    os.write(0xff & (v >>>  8));
    os.write(0xff & (v >>> 16));
    os.write(0xff & (v >>> 24));
  }

  /**
   * Writes a Little-endian long
   */
  private static void writeLong(OutputStream os, long v) throws IOException
  {
    os.write((int)(0xff & v));
    os.write((int)(0xff & (v >>>  8)));
    os.write((int)(0xff & (v >>> 16)));
    os.write((int)(0xff & (v >>> 24)));
    os.write((int)(0xff & (v >>> 32)));
    os.write((int)(0xff & (v >>> 40)));
    os.write((int)(0xff & (v >>> 48)));
    os.write((int)(0xff & (v >>> 56)));
  }
}
