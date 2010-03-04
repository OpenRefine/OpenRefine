/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

/*
 * This package is based on the work done by Keiron Liddle, Aftex Software
 * <keiron@aftexsw.com> to whom the Ant project is very grateful for his
 * great code.
 */
package org.apache.tools.bzip2;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that decompresses from the BZip2 format (without the file
 * header chars) to be read as any other stream.
 *
 * @author <a href="mailto:keiron@aftexsw.com">Keiron Liddle</a>
 */
public class CBZip2InputStream extends InputStream implements BZip2Constants {
    private static void cadvise() {
        System.out.println("CRC Error");
        //throw new CCoruptionError();
    }

    private static void compressedStreamEOF() {
        cadvise();
    }

    private void makeMaps() {
        int i;
        nInUse = 0;
        for (i = 0; i < 256; i++) {
            if (inUse[i]) {
                seqToUnseq[nInUse] = (char) i;
                unseqToSeq[i] = (char) nInUse;
                nInUse++;
            }
        }
    }

    /*
      index of the last char in the block, so
      the block size == last + 1.
    */
    private int  last;

    /*
      index in zptr[] of original string after sorting.
    */
    private int  origPtr;

    /*
      always: in the range 0 .. 9.
      The current block size is 100000 * this number.
    */
    private int blockSize100k;

    private boolean blockRandomised;

    private int bsBuff;
    private int bsLive;
    private CRC mCrc = new CRC();

    private boolean[] inUse = new boolean[256];
    private int nInUse;

    private char[] seqToUnseq = new char[256];
    private char[] unseqToSeq = new char[256];

    private char[] selector = new char[MAX_SELECTORS];
    private char[] selectorMtf = new char[MAX_SELECTORS];

    private int[] tt;
    private char[] ll8;

    /*
      freq table collected to save a pass over the data
      during decompression.
    */
    private int[] unzftab = new int[256];

    private int[][] limit = new int[N_GROUPS][MAX_ALPHA_SIZE];
    private int[][] base = new int[N_GROUPS][MAX_ALPHA_SIZE];
    private int[][] perm = new int[N_GROUPS][MAX_ALPHA_SIZE];
    private int[] minLens = new int[N_GROUPS];

    private InputStream bsStream;

    private boolean streamEnd = false;

    private int currentChar = -1;

    private static final int START_BLOCK_STATE = 1;
    private static final int RAND_PART_A_STATE = 2;
    private static final int RAND_PART_B_STATE = 3;
    private static final int RAND_PART_C_STATE = 4;
    private static final int NO_RAND_PART_A_STATE = 5;
    private static final int NO_RAND_PART_B_STATE = 6;
    private static final int NO_RAND_PART_C_STATE = 7;

    private int currentState = START_BLOCK_STATE;

    private int storedBlockCRC, storedCombinedCRC;
    private int computedBlockCRC, computedCombinedCRC;

    int i2, count, chPrev, ch2;
    int i, tPos;
    int rNToGo = 0;
    int rTPos  = 0;
    int j2;
    char z;

    public CBZip2InputStream(InputStream zStream) {
        ll8 = null;
        tt = null;
        bsSetStream(zStream);
        initialize();
        initBlock();
        setupBlock();
    }

    public int read() {
        if (streamEnd) {
            return -1;
        } else {
            int retChar = currentChar;
            switch(currentState) {
            case START_BLOCK_STATE:
                break;
            case RAND_PART_A_STATE:
                break;
            case RAND_PART_B_STATE:
                setupRandPartB();
                break;
            case RAND_PART_C_STATE:
                setupRandPartC();
                break;
            case NO_RAND_PART_A_STATE:
                break;
            case NO_RAND_PART_B_STATE:
                setupNoRandPartB();
                break;
            case NO_RAND_PART_C_STATE:
                setupNoRandPartC();
                break;
            default:
                break;
            }
            return retChar;
        }
    }

    private void initialize() {
        char magic3, magic4;
        magic3 = bsGetUChar();
        magic4 = bsGetUChar();
        if (magic3 != 'h' || magic4 < '1' || magic4 > '9') {
            bsFinishedWithStream();
            streamEnd = true;
            return;
        }

        setDecompressStructureSizes(magic4 - '0');
        computedCombinedCRC = 0;
    }

    private void initBlock() {
        char magic1, magic2, magic3, magic4;
        char magic5, magic6;
        magic1 = bsGetUChar();
        magic2 = bsGetUChar();
        magic3 = bsGetUChar();
        magic4 = bsGetUChar();
        magic5 = bsGetUChar();
        magic6 = bsGetUChar();
        if (magic1 == 0x17 && magic2 == 0x72 && magic3 == 0x45
            && magic4 == 0x38 && magic5 == 0x50 && magic6 == 0x90) {
            complete();
            return;
        }

        if (magic1 != 0x31 || magic2 != 0x41 || magic3 != 0x59
            || magic4 != 0x26 || magic5 != 0x53 || magic6 != 0x59) {
            badBlockHeader();
            streamEnd = true;
            return;
        }

        storedBlockCRC = bsGetInt32();

        if (bsR(1) == 1) {
            blockRandomised = true;
        } else {
            blockRandomised = false;
        }

        //        currBlockNo++;
        getAndMoveToFrontDecode();

        mCrc.initialiseCRC();
        currentState = START_BLOCK_STATE;
    }

    private void endBlock() {
        computedBlockCRC = mCrc.getFinalCRC();
        /* A bad CRC is considered a fatal error. */
        if (storedBlockCRC != computedBlockCRC) {
            crcError();
        }

        computedCombinedCRC = (computedCombinedCRC << 1)
            | (computedCombinedCRC >>> 31);
        computedCombinedCRC ^= computedBlockCRC;
    }

    private void complete() {
        storedCombinedCRC = bsGetInt32();
        if (storedCombinedCRC != computedCombinedCRC) {
            crcError();
        }

        bsFinishedWithStream();
        streamEnd = true;
    }

    private static void blockOverrun() {
        cadvise();
    }

    private static void badBlockHeader() {
        cadvise();
    }

    private static void crcError() {
        cadvise();
    }

    private void bsFinishedWithStream() {
        try {
            if (this.bsStream != null) {
                if (this.bsStream != System.in) {
                    this.bsStream.close();
                    this.bsStream = null;
                }
            }
        } catch (IOException ioe) {
            //ignore
        }
    }

    private void bsSetStream(InputStream f) {
        bsStream = f;
        bsLive = 0;
        bsBuff = 0;
    }

    private int bsR(int n) {
        int v;
        while (bsLive < n) {
            int zzi;
            char thech = 0;
            try {
                thech = (char) bsStream.read();
            } catch (IOException e) {
                compressedStreamEOF();
            }
            if (thech == -1) {
                compressedStreamEOF();
            }
            zzi = thech;
            bsBuff = (bsBuff << 8) | (zzi & 0xff);
            bsLive += 8;
        }

        v = (bsBuff >> (bsLive - n)) & ((1 << n) - 1);
        bsLive -= n;
        return v;
    }

    private char bsGetUChar() {
        return (char) bsR(8);
    }

    private int bsGetint() {
        int u = 0;
        u = (u << 8) | bsR(8);
        u = (u << 8) | bsR(8);
        u = (u << 8) | bsR(8);
        u = (u << 8) | bsR(8);
        return u;
    }

    private int bsGetIntVS(int numBits) {
        return (int) bsR(numBits);
    }

    private int bsGetInt32() {
        return (int) bsGetint();
    }

    private void hbCreateDecodeTables(int[] limit, int[] base,
                                      int[] perm, char[] length,
                                      int minLen, int maxLen, int alphaSize) {
        int pp, i, j, vec;

        pp = 0;
        for (i = minLen; i <= maxLen; i++) {
            for (j = 0; j < alphaSize; j++) {
                if (length[j] == i) {
                    perm[pp] = j;
                    pp++;
                }
            }
        }

        for (i = 0; i < MAX_CODE_LEN; i++) {
            base[i] = 0;
        }
        for (i = 0; i < alphaSize; i++) {
            base[length[i] + 1]++;
        }

        for (i = 1; i < MAX_CODE_LEN; i++) {
            base[i] += base[i - 1];
        }

        for (i = 0; i < MAX_CODE_LEN; i++) {
            limit[i] = 0;
        }
        vec = 0;

        for (i = minLen; i <= maxLen; i++) {
            vec += (base[i + 1] - base[i]);
            limit[i] = vec - 1;
            vec <<= 1;
        }
        for (i = minLen + 1; i <= maxLen; i++) {
            base[i] = ((limit[i - 1] + 1) << 1) - base[i];
        }
    }

    private void recvDecodingTables() {
        char len[][] = new char[N_GROUPS][MAX_ALPHA_SIZE];
        int i, j, t, nGroups, nSelectors, alphaSize;
        int minLen, maxLen;
        boolean[] inUse16 = new boolean[16];

        /* Receive the mapping table */
        for (i = 0; i < 16; i++) {
            if (bsR(1) == 1) {
                inUse16[i] = true;
            } else {
                inUse16[i] = false;
            }
        }

        for (i = 0; i < 256; i++) {
            inUse[i] = false;
        }

        for (i = 0; i < 16; i++) {
            if (inUse16[i]) {
                for (j = 0; j < 16; j++) {
                    if (bsR(1) == 1) {
                        inUse[i * 16 + j] = true;
                    }
                }
            }
        }

        makeMaps();
        alphaSize = nInUse + 2;

        /* Now the selectors */
        nGroups = bsR(3);
        nSelectors = bsR(15);
        for (i = 0; i < nSelectors; i++) {
            j = 0;
            while (bsR(1) == 1) {
                j++;
            }
            selectorMtf[i] = (char) j;
        }

        /* Undo the MTF values for the selectors. */
        {
            char[] pos = new char[N_GROUPS];
            char tmp, v;
            for (v = 0; v < nGroups; v++) {
                pos[v] = v;
            }

            for (i = 0; i < nSelectors; i++) {
                v = selectorMtf[i];
                tmp = pos[v];
                while (v > 0) {
                    pos[v] = pos[v - 1];
                    v--;
                }
                pos[0] = tmp;
                selector[i] = tmp;
            }
        }

        /* Now the coding tables */
        for (t = 0; t < nGroups; t++) {
            int curr = bsR(5);
            for (i = 0; i < alphaSize; i++) {
                while (bsR(1) == 1) {
                    if (bsR(1) == 0) {
                        curr++;
                    } else {
                        curr--;
                    }
                }
                len[t][i] = (char) curr;
            }
        }

        /* Create the Huffman decoding tables */
        for (t = 0; t < nGroups; t++) {
            minLen = 32;
            maxLen = 0;
            for (i = 0; i < alphaSize; i++) {
                if (len[t][i] > maxLen) {
                    maxLen = len[t][i];
                }
                if (len[t][i] < minLen) {
                    minLen = len[t][i];
                }
            }
            hbCreateDecodeTables(limit[t], base[t], perm[t], len[t], minLen,
                                 maxLen, alphaSize);
            minLens[t] = minLen;
        }
    }

    private void getAndMoveToFrontDecode() {
        char[] yy = new char[256];
        int i, j, nextSym, limitLast;
        int EOB, groupNo, groupPos;

        limitLast = baseBlockSize * blockSize100k;
        origPtr = bsGetIntVS(24);

        recvDecodingTables();
        EOB = nInUse + 1;
        groupNo = -1;
        groupPos = 0;

        /*
          Setting up the unzftab entries here is not strictly
          necessary, but it does save having to do it later
          in a separate pass, and so saves a block's worth of
          cache misses.
        */
        for (i = 0; i <= 255; i++) {
            unzftab[i] = 0;
        }

        for (i = 0; i <= 255; i++) {
            yy[i] = (char) i;
        }

        last = -1;

        {
            int zt, zn, zvec, zj;
            if (groupPos == 0) {
                groupNo++;
                groupPos = G_SIZE;
            }
            groupPos--;
            zt = selector[groupNo];
            zn = minLens[zt];
            zvec = bsR(zn);
            while (zvec > limit[zt][zn]) {
                zn++;
                {
                    {
                        while (bsLive < 1) {
                            int zzi;
                            char thech = 0;
                            try {
                                thech = (char) bsStream.read();
                            } catch (IOException e) {
                                compressedStreamEOF();
                            }
                            if (thech == -1) {
                                compressedStreamEOF();
                            }
                            zzi = thech;
                            bsBuff = (bsBuff << 8) | (zzi & 0xff);
                            bsLive += 8;
                        }
                    }
                    zj = (bsBuff >> (bsLive - 1)) & 1;
                    bsLive--;
                }
                zvec = (zvec << 1) | zj;
            }
            nextSym = perm[zt][zvec - base[zt][zn]];
        }

        while (true) {

            if (nextSym == EOB) {
                break;
            }

            if (nextSym == RUNA || nextSym == RUNB) {
                char ch;
                int s = -1;
                int N = 1;
                do {
                    if (nextSym == RUNA) {
                        s = s + (0 + 1) * N;
                    } else if (nextSym == RUNB) {
                        s = s + (1 + 1) * N;
                           }
                    N = N * 2;
                    {
                        int zt, zn, zvec, zj;
                        if (groupPos == 0) {
                            groupNo++;
                            groupPos = G_SIZE;
                        }
                        groupPos--;
                        zt = selector[groupNo];
                        zn = minLens[zt];
                        zvec = bsR(zn);
                        while (zvec > limit[zt][zn]) {
                            zn++;
                            {
                                {
                                    while (bsLive < 1) {
                                        int zzi;
                                        char thech = 0;
                                        try {
                                            thech = (char) bsStream.read();
                                        } catch (IOException e) {
                                            compressedStreamEOF();
                                        }
                                        if (thech == -1) {
                                            compressedStreamEOF();
                                        }
                                        zzi = thech;
                                        bsBuff = (bsBuff << 8) | (zzi & 0xff);
                                        bsLive += 8;
                                    }
                                }
                                zj = (bsBuff >> (bsLive - 1)) & 1;
                                bsLive--;
                            }
                            zvec = (zvec << 1) | zj;
                        }
                        nextSym = perm[zt][zvec - base[zt][zn]];
                    }
                } while (nextSym == RUNA || nextSym == RUNB);

                s++;
                ch = seqToUnseq[yy[0]];
                unzftab[ch] += s;

                while (s > 0) {
                    last++;
                    ll8[last] = ch;
                    s--;
                }

                if (last >= limitLast) {
                    blockOverrun();
                }
                continue;
            } else {
                char tmp;
                last++;
                if (last >= limitLast) {
                    blockOverrun();
                }

                tmp = yy[nextSym - 1];
                unzftab[seqToUnseq[tmp]]++;
                ll8[last] = seqToUnseq[tmp];

                /*
                  This loop is hammered during decompression,
                  hence the unrolling.

                  for (j = nextSym-1; j > 0; j--) yy[j] = yy[j-1];
                */

                j = nextSym - 1;
                for (; j > 3; j -= 4) {
                    yy[j]     = yy[j - 1];
                    yy[j - 1] = yy[j - 2];
                    yy[j - 2] = yy[j - 3];
                    yy[j - 3] = yy[j - 4];
                }
                for (; j > 0; j--) {
                    yy[j] = yy[j - 1];
                }

                yy[0] = tmp;
                {
                    int zt, zn, zvec, zj;
                    if (groupPos == 0) {
                        groupNo++;
                        groupPos = G_SIZE;
                    }
                    groupPos--;
                    zt = selector[groupNo];
                    zn = minLens[zt];
                    zvec = bsR(zn);
                    while (zvec > limit[zt][zn]) {
                        zn++;
                        {
                            {
                                while (bsLive < 1) {
                                    int zzi;
                                    char thech = 0;
                                    try {
                                        thech = (char) bsStream.read();
                                    } catch (IOException e) {
                                        compressedStreamEOF();
                                    }
                                    zzi = thech;
                                    bsBuff = (bsBuff << 8) | (zzi & 0xff);
                                    bsLive += 8;
                                }
                            }
                            zj = (bsBuff >> (bsLive - 1)) & 1;
                            bsLive--;
                        }
                        zvec = (zvec << 1) | zj;
                    }
                    nextSym = perm[zt][zvec - base[zt][zn]];
                }
                continue;
            }
        }
    }

    private void setupBlock() {
        int[] cftab = new int[257];
        char ch;

        cftab[0] = 0;
        for (i = 1; i <= 256; i++) {
            cftab[i] = unzftab[i - 1];
        }
        for (i = 1; i <= 256; i++) {
            cftab[i] += cftab[i - 1];
        }

        for (i = 0; i <= last; i++) {
            ch = (char) ll8[i];
            tt[cftab[ch]] = i;
            cftab[ch]++;
        }
        cftab = null;

        tPos = tt[origPtr];

        count = 0;
        i2 = 0;
        ch2 = 256;   /* not a char and not EOF */

        if (blockRandomised) {
            rNToGo = 0;
            rTPos = 0;
            setupRandPartA();
        } else {
            setupNoRandPartA();
        }
    }

    private void setupRandPartA() {
        if (i2 <= last) {
            chPrev = ch2;
            ch2 = ll8[tPos];
            tPos = tt[tPos];
            if (rNToGo == 0) {
                rNToGo = rNums[rTPos];
                rTPos++;
                if (rTPos == 512) {
                    rTPos = 0;
                }
            }
            rNToGo--;
            ch2 ^= (int) ((rNToGo == 1) ? 1 : 0);
            i2++;

            currentChar = ch2;
            currentState = RAND_PART_B_STATE;
            mCrc.updateCRC(ch2);
        } else {
            endBlock();
            initBlock();
            setupBlock();
        }
    }

    private void setupNoRandPartA() {
        if (i2 <= last) {
            chPrev = ch2;
            ch2 = ll8[tPos];
            tPos = tt[tPos];
            i2++;

            currentChar = ch2;
            currentState = NO_RAND_PART_B_STATE;
            mCrc.updateCRC(ch2);
        } else {
            endBlock();
            initBlock();
            setupBlock();
        }
    }

    private void setupRandPartB() {
        if (ch2 != chPrev) {
            currentState = RAND_PART_A_STATE;
            count = 1;
            setupRandPartA();
        } else {
            count++;
            if (count >= 4) {
                z = ll8[tPos];
                tPos = tt[tPos];
                if (rNToGo == 0) {
                    rNToGo = rNums[rTPos];
                    rTPos++;
                    if (rTPos == 512) {
                        rTPos = 0;
                    }
                }
                rNToGo--;
                z ^= ((rNToGo == 1) ? 1 : 0);
                j2 = 0;
                currentState = RAND_PART_C_STATE;
                setupRandPartC();
            } else {
                currentState = RAND_PART_A_STATE;
                setupRandPartA();
            }
        }
    }

    private void setupRandPartC() {
        if (j2 < (int) z) {
            currentChar = ch2;
            mCrc.updateCRC(ch2);
            j2++;
        } else {
            currentState = RAND_PART_A_STATE;
            i2++;
            count = 0;
            setupRandPartA();
        }
    }

    private void setupNoRandPartB() {
        if (ch2 != chPrev) {
            currentState = NO_RAND_PART_A_STATE;
            count = 1;
            setupNoRandPartA();
        } else {
            count++;
            if (count >= 4) {
                z = ll8[tPos];
                tPos = tt[tPos];
                currentState = NO_RAND_PART_C_STATE;
                j2 = 0;
                setupNoRandPartC();
            } else {
                currentState = NO_RAND_PART_A_STATE;
                setupNoRandPartA();
            }
        }
    }

    private void setupNoRandPartC() {
        if (j2 < (int) z) {
            currentChar = ch2;
            mCrc.updateCRC(ch2);
            j2++;
        } else {
            currentState = NO_RAND_PART_A_STATE;
            i2++;
            count = 0;
            setupNoRandPartA();
        }
    }

    private void setDecompressStructureSizes(int newSize100k) {
        if (!(0 <= newSize100k && newSize100k <= 9 && 0 <= blockSize100k
               && blockSize100k <= 9)) {
            // throw new IOException("Invalid block size");
        }

        blockSize100k = newSize100k;

        if (newSize100k == 0) {
            return;
        }

        int n = baseBlockSize * newSize100k;
        ll8 = new char[n];
        tt = new int[n];
    }
}

