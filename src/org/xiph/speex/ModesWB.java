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
 * Class: ModesWB.java                                                        *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: ModesWB.java,v 1.1 2011/08/09 22:12:29 phil Exp $ */

/* Copyright (C) 2002 Jean-Marc Valin 

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:
   
   - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
   
   - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
   
   - Neither the name of the Xiph.org Foundation nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR
   CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.xiph.speex;

/**
 * Wideband Modes
 */
public class ModesWB
  extends ModesNB
{
  public static SubMode wbsubmodes[];
  public static SubMode uwbsubmodes[];

  /**
   * Initialisation
   */
  public void init()
  {
    super.init();
    
    HighLspQuant highLU = new HighLspQuant();
    
    SplitShapeSearch ssCbHighLbrSearch = new SplitShapeSearch(40, 10, 4, hexc_10_32_table, 5, 0);
    SplitShapeSearch ssCbHighSearch    = new SplitShapeSearch(40, 8, 5, hexc_table, 7, 1);
    
    wbsubmodes  = new SubMode[8];
    uwbsubmodes = new SubMode[8];
    wbsubmodes[1]  = new SubMode(0, 0, 1, 0, highLU, null, null, .75f, .75f, -1, 36);
    wbsubmodes[2]  = new SubMode(0, 0, 1, 0, highLU, null, ssCbHighLbrSearch, .85f, .6f, -1, 112);
    wbsubmodes[3]  = new SubMode(0, 0, 1, 0, highLU, null, ssCbHighSearch, .75f, .7f, -1, 192);
    wbsubmodes[4]  = new SubMode(0, 0, 1, 1, highLU, null, ssCbHighSearch, .75f, .75f, -1, 352);
    uwbsubmodes[1] = new SubMode(0, 0, 1, 0, highLU, null, null, .75f, .75f, -1, 2);
  }
}
