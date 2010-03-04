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
import java.io.OutputStream;

/**
 * An output stream that compresses into the BZip2 format (without the file
 * header chars) into another stream.
 *
 * @author <a href="mailto:keiron@aftexsw.com">Keiron Liddle</a>
 *
 * TODO:    Update to BZip2 1.0.1
 */
public class CBZip2OutputStream extends OutputStream implements BZip2Constants {
    protected static final int SETMASK = (1 << 21);
    protected static final int CLEARMASK = (~SETMASK);
    protected static final int GREATER_ICOST = 15;
    protected static final int LESSER_ICOST = 0;
    protected static final int SMALL_THRESH = 20;
    protected static final int DEPTH_THRESH = 10;

    /*
      If you are ever unlucky/improbable enough
      to get a stack overflow whilst sorting,
      increase the following constant and try
      again.  In practice I have never seen the
      stack go above 27 elems, so the following
      limit seems very generous.
    */
    protected static final int QSORT_STACK_SIZE = 1000;

    private static void panic() {
        System.out.println("panic");
        //throw new CError();
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

    protected static void hbMakeCodeLengths(char[] len, int[] freq,
                                            int alphaSize, int maxLen) {
        /*
          Nodes and heap entries run from 1.  Entry 0
          for both the heap and nodes is a sentinel.
        */
        int nNodes, nHeap, n1, n2, i, j, k;
        boolean  tooLong;

        int[] heap = new int[MAX_ALPHA_SIZE + 2];
        int[] weight = new int[MAX_ALPHA_SIZE * 2];
        int[] parent = new int[MAX_ALPHA_SIZE * 2];

        for (i = 0; i < alphaSize; i++) {
            weight[i + 1] = (freq[i] == 0 ? 1 : freq[i]) << 8;
        }

        while (true) {
            nNodes = alphaSize;
            nHeap = 0;

            heap[0] = 0;
            weight[0] = 0;
            parent[0] = -2;

            for (i = 1; i <= alphaSize; i++) {
                parent[i] = -1;
                nHeap++;
                heap[nHeap] = i;
                {
                    int zz, tmp;
                    zz = nHeap;
                    tmp = heap[zz];
                    while (weight[tmp] < weight[heap[zz >> 1]]) {
                        heap[zz] = heap[zz >> 1];
                        zz >>= 1;
                    }
                    heap[zz] = tmp;
                }
            }
            if (!(nHeap < (MAX_ALPHA_SIZE + 2))) {
                panic();
            }

            while (nHeap > 1) {
                n1 = heap[1];
                heap[1] = heap[nHeap];
                nHeap--;
                {
                    int zz = 0, yy = 0, tmp = 0;
                    zz = 1;
                    tmp = heap[zz];
                    while (true) {
                        yy = zz << 1;
                        if (yy > nHeap) {
                            break;
                        }
                        if (yy < nHeap
                            && weight[heap[yy + 1]] < weight[heap[yy]]) {
                            yy++;
                        }
                        if (weight[tmp] < weight[heap[yy]]) {
                            break;
                        }
                        heap[zz] = heap[yy];
                        zz = yy;
                    }
                    heap[zz] = tmp;
                }
                n2 = heap[1];
                heap[1] = heap[nHeap];
                nHeap--;
                {
                    int zz = 0, yy = 0, tmp = 0;
                    zz = 1;
                    tmp = heap[zz];
                    while (true) {
                        yy = zz << 1;
                        if (yy > nHeap) {
                            break;
                        }
                        if (yy < nHeap
                            && weight[heap[yy + 1]] < weight[heap[yy]]) {
                            yy++;
                        }
                        if (weight[tmp] < weight[heap[yy]]) {
                            break;
                        }
                        heap[zz] = heap[yy];
                        zz = yy;
                    }
                    heap[zz] = tmp;
                }
                nNodes++;
                parent[n1] = parent[n2] = nNodes;

                weight[nNodes] = ((weight[n1] & 0xffffff00)
                                  + (weight[n2] & 0xffffff00))
                    | (1 + (((weight[n1] & 0x000000ff) >
                             (weight[n2] & 0x000000ff)) ?
                            (weight[n1] & 0x000000ff) :
                            (weight[n2] & 0x000000ff)));

                parent[nNodes] = -1;
                nHeap++;
                heap[nHeap] = nNodes;
                {
                    int zz = 0, tmp = 0;
                    zz = nHeap;
                    tmp = heap[zz];
                    while (weight[tmp] < weight[heap[zz >> 1]]) {
                        heap[zz] = heap[zz >> 1];
                        zz >>= 1;
                    }
                    heap[zz] = tmp;
                }
            }
            if (!(nNodes < (MAX_ALPHA_SIZE * 2))) {
                panic();
            }

            tooLong = false;
            for (i = 1; i <= alphaSize; i++) {
                j = 0;
                k = i;
                while (parent[k] >= 0) {
                    k = parent[k];
                    j++;
                }
                len[i - 1] = (char) j;
                if (j > maxLen) {
                    tooLong = true;
                }
            }

            if (!tooLong) {
                break;
            }

            for (i = 1; i < alphaSize; i++) {
                j = weight[i] >> 8;
                j = 1 + (j / 2);
                weight[i] = j << 8;
            }
        }
    }

    /*
      index of the last char in the block, so
      the block size == last + 1.
    */
    int last;

    /*
      index in zptr[] of original string after sorting.
    */
    int origPtr;

    /*
      always: in the range 0 .. 9.
      The current block size is 100000 * this number.
    */
    int blockSize100k;

    boolean blockRandomised;

    int bytesOut;
    int bsBuff;
    int bsLive;
    CRC mCrc = new CRC();

    private boolean[] inUse = new boolean[256];
    private int nInUse;

    private char[] seqToUnseq = new char[256];
    private char[] unseqToSeq = new char[256];

    private char[] selector = new char[MAX_SELECTORS];
    private char[] selectorMtf = new char[MAX_SELECTORS];

    private char[] block;
    private int[] quadrant;
    private int[] zptr;
    private short[] szptr;
    private int[] ftab;

    private int nMTF;

    private int[] mtfFreq = new int[MAX_ALPHA_SIZE];

    /*
     * Used when sorting.  If too many long comparisons
     * happen, we stop sorting, randomise the block
     * slightly, and try again.
     */
    private int workFactor;
    private int workDone;
    private int workLimit;
    private boolean firstAttempt;
    private int nBlocksRandomised;

    private int currentChar = -1;
    private int runLength = 0;

    public CBZip2OutputStream(OutputStream inStream) throws IOException {
        this(inStream, 9);
    }

    public CBZip2OutputStream(OutputStream inStream, int inBlockSize)
        throws IOException {
        block = null;
        quadrant = null;
        zptr = null;
        ftab = null;

        bsSetStream(inStream);

        workFactor = 50;
        if (inBlockSize > 9) {
            inBlockSize = 9;
        }
        if (inBlockSize < 1) {
            inBlockSize = 1;
        }
        blockSize100k = inBlockSize;
        allocateCompressStructures();
        initialize();
        initBlock();
    }

    /**
     *
     * modified by Oliver Merkel, 010128
     *
     */
    public void write(int bv) throws IOException {
        int b = (256 + bv) % 256;
        if (currentChar != -1) {
            if (currentChar == b) {
                runLength++;
                if (runLength > 254) {
                    writeRun();
                    currentChar = -1;
                    runLength = 0;
                }
            } else {
                writeRun();
                runLength = 1;
                currentChar = b;
            }
        } else {
            currentChar = b;
            runLength++;
        }
    }

    private void writeRun() throws IOException {
        if (last < allowableBlockSize) {
            inUse[currentChar] = true;
            for (int i = 0; i < runLength; i++) {
                mCrc.updateCRC((char) currentChar);
            }
            switch (runLength) {
            case 1:
                last++;
                block[last + 1] = (char) currentChar;
                break;
            case 2:
                last++;
                block[last + 1] = (char) currentChar;
                last++;
                block[last + 1] = (char) currentChar;
                break;
            case 3:
                last++;
                block[last + 1] = (char) currentChar;
                last++;
                block[last + 1] = (char) currentChar;
                last++;
                block[last + 1] = (char) currentChar;
                break;
            default:
                inUse[runLength - 4] = true;
                last++;
                block[last + 1] = (char) currentChar;
                last++;
                block[last + 1] = (char) currentChar;
                last++;
                block[last + 1] = (char) currentChar;
                last++;
                block[last + 1] = (char) currentChar;
                last++;
                block[last + 1] = (char) (runLength - 4);
                break;
            }
        } else {
            endBlock();
            initBlock();
            writeRun();
        }
    }

    boolean closed = false;

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() throws IOException {
        if (closed) {
            return;
        }

        if (runLength > 0) {
            writeRun();
        }
        currentChar = -1;
        endBlock();
        endCompression();
        closed = true;
        super.close();
        bsStream.close();
    }

    public void flush() throws IOException {
        super.flush();
        bsStream.flush();
    }

    private int blockCRC, combinedCRC;

    private void initialize() throws IOException {
        bytesOut = 0;
        nBlocksRandomised = 0;

        /* Write `magic' bytes h indicating file-format == huffmanised,
           followed by a digit indicating blockSize100k.
        */
        bsPutUChar('h');
        bsPutUChar('0' + blockSize100k);

        combinedCRC = 0;
    }

    private int allowableBlockSize;

    private void initBlock() {
        //        blockNo++;
        mCrc.initialiseCRC();
        last = -1;
        //        ch = 0;

        for (int i = 0; i < 256; i++) {
            inUse[i] = false;
        }

        /* 20 is just a paranoia constant */
        allowableBlockSize = baseBlockSize * blockSize100k - 20;
    }

    private void endBlock() throws IOException {
        blockCRC = mCrc.getFinalCRC();
        combinedCRC = (combinedCRC << 1) | (combinedCRC >>> 31);
        combinedCRC ^= blockCRC;

        /* sort the block and establish posn of original string */
        doReversibleTransformation();

        /*
          A 6-byte block header, the value chosen arbitrarily
          as 0x314159265359 :-).  A 32 bit value does not really
          give a strong enough guarantee that the value will not
          appear by chance in the compressed datastream.  Worst-case
          probability of this event, for a 900k block, is about
          2.0e-3 for 32 bits, 1.0e-5 for 40 bits and 4.0e-8 for 48 bits.
          For a compressed file of size 100Gb -- about 100000 blocks --
          only a 48-bit marker will do.  NB: normal compression/
          decompression do *not* rely on these statistical properties.
          They are only important when trying to recover blocks from
          damaged files.
        */
        bsPutUChar(0x31);
        bsPutUChar(0x41);
        bsPutUChar(0x59);
        bsPutUChar(0x26);
        bsPutUChar(0x53);
        bsPutUChar(0x59);

        /* Now the block's CRC, so it is in a known place. */
        bsPutint(blockCRC);

        /* Now a single bit indicating randomisation. */
        if (blockRandomised) {
            bsW(1, 1);
            nBlocksRandomised++;
        } else {
            bsW(1, 0);
        }

        /* Finally, block's contents proper. */
        moveToFrontCodeAndSend();
    }

    private void endCompression() throws IOException {
        /*
          Now another magic 48-bit number, 0x177245385090, to
          indicate the end of the last block.  (sqrt(pi), if
          you want to know.  I did want to use e, but it contains
          too much repetition -- 27 18 28 18 28 46 -- for me
          to feel statistically comfortable.  Call me paranoid.)
        */
        bsPutUChar(0x17);
        bsPutUChar(0x72);
        bsPutUChar(0x45);
        bsPutUChar(0x38);
        bsPutUChar(0x50);
        bsPutUChar(0x90);

        bsPutint(combinedCRC);

        bsFinishedWithStream();
    }

    private void hbAssignCodes (int[] code, char[] length, int minLen,
                                int maxLen, int alphaSize) {
        int n, vec, i;

        vec = 0;
        for (n = minLen; n <= maxLen; n++) {
            for (i = 0; i < alphaSize; i++) {
                if (length[i] == n) {
                    code[i] = vec;
                    vec++;
                }
            };
            vec <<= 1;
        }
    }

    private void bsSetStream(OutputStream f) {
        bsStream = f;
        bsLive = 0;
        bsBuff = 0;
        bytesOut = 0;
    }

    private void bsFinishedWithStream() throws IOException {
        while (bsLive > 0) {
            int ch = (bsBuff >> 24);
            try {
                bsStream.write(ch); // write 8-bit
            } catch (IOException e) {
                throw  e;
            }
            bsBuff <<= 8;
            bsLive -= 8;
            bytesOut++;
        }
    }

    private void bsW(int n, int v) throws IOException {
        while (bsLive >= 8) {
            int ch = (bsBuff >> 24);
            try {
                bsStream.write(ch); // write 8-bit
            } catch (IOException e) {
                throw e;
            }
            bsBuff <<= 8;
            bsLive -= 8;
            bytesOut++;
        }
        bsBuff |= (v << (32 - bsLive - n));
        bsLive += n;
    }

    private void bsPutUChar(int c) throws IOException {
        bsW(8, c);
    }

    private void bsPutint(int u) throws IOException {
        bsW(8, (u >> 24) & 0xff);
        bsW(8, (u >> 16) & 0xff);
        bsW(8, (u >>  8) & 0xff);
        bsW(8,  u        & 0xff);
    }

    private void bsPutIntVS(int numBits, int c) throws IOException {
        bsW(numBits, c);
    }

    private void sendMTFValues() throws IOException {
        char len[][] = new char[N_GROUPS][MAX_ALPHA_SIZE];

        int v, t, i, j, gs, ge, totc, bt, bc, iter;
        int nSelectors = 0, alphaSize, minLen, maxLen, selCtr;
        int nGroups;

        alphaSize = nInUse + 2;
        for (t = 0; t < N_GROUPS; t++) {
            for (v = 0; v < alphaSize; v++) {
                len[t][v] = (char) GREATER_ICOST;
            }
        }

        /* Decide how many coding tables to use */
        if (nMTF <= 0) {
            panic();
        }

        if (nMTF < 200) {
            nGroups = 2;
        } else if (nMTF < 600) {
            nGroups = 3;
        } else if (nMTF < 1200) {
            nGroups = 4;
        } else if (nMTF < 2400) {
            nGroups = 5;
        } else {
            nGroups = 6;
        }

        /* Generate an initial set of coding tables */ {
            int nPart, remF, tFreq, aFreq;

            nPart = nGroups;
            remF  = nMTF;
            gs = 0;
            while (nPart > 0) {
                tFreq = remF / nPart;
                ge = gs - 1;
                aFreq = 0;
                while (aFreq < tFreq && ge < alphaSize - 1) {
                    ge++;
                    aFreq += mtfFreq[ge];
                }

                if (ge > gs && nPart != nGroups && nPart != 1
                    && ((nGroups - nPart) % 2 == 1)) {
                    aFreq -= mtfFreq[ge];
                    ge--;
                }

                for (v = 0; v < alphaSize; v++) {
                    if (v >= gs && v <= ge) {
                        len[nPart - 1][v] = (char) LESSER_ICOST;
                    } else {
                        len[nPart - 1][v] = (char) GREATER_ICOST;
                    }
                }

                nPart--;
                gs = ge + 1;
                remF -= aFreq;
            }
        }

        int[][] rfreq = new int[N_GROUPS][MAX_ALPHA_SIZE];
        int[] fave = new int[N_GROUPS];
        short[] cost = new short[N_GROUPS];
        /*
          Iterate up to N_ITERS times to improve the tables.
        */
        for (iter = 0; iter < N_ITERS; iter++) {
            for (t = 0; t < nGroups; t++) {
                fave[t] = 0;
            }

            for (t = 0; t < nGroups; t++) {
                for (v = 0; v < alphaSize; v++) {
                    rfreq[t][v] = 0;
                }
            }

            nSelectors = 0;
            totc = 0;
            gs = 0;
            while (true) {

                /* Set group start & end marks. */
                if (gs >= nMTF) {
                    break;
                }
                ge = gs + G_SIZE - 1;
                if (ge >= nMTF) {
                    ge = nMTF - 1;
                }

                /*
                  Calculate the cost of this group as coded
                  by each of the coding tables.
                */
                for (t = 0; t < nGroups; t++) {
                    cost[t] = 0;
                }

                if (nGroups == 6) {
                    short cost0, cost1, cost2, cost3, cost4, cost5;
                    cost0 = cost1 = cost2 = cost3 = cost4 = cost5 = 0;
                    for (i = gs; i <= ge; i++) {
                        short icv = szptr[i];
                        cost0 += len[0][icv];
                        cost1 += len[1][icv];
                        cost2 += len[2][icv];
                        cost3 += len[3][icv];
                        cost4 += len[4][icv];
                        cost5 += len[5][icv];
                    }
                    cost[0] = cost0;
                    cost[1] = cost1;
                    cost[2] = cost2;
                    cost[3] = cost3;
                    cost[4] = cost4;
                    cost[5] = cost5;
                } else {
                    for (i = gs; i <= ge; i++) {
                        short icv = szptr[i];
                        for (t = 0; t < nGroups; t++) {
                            cost[t] += len[t][icv];
                        }
                    }
                }

                /*
                  Find the coding table which is best for this group,
                  and record its identity in the selector table.
                */
                bc = 999999999;
                bt = -1;
                for (t = 0; t < nGroups; t++) {
                    if (cost[t] < bc) {
                        bc = cost[t];
                        bt = t;
                    }
                };
                totc += bc;
                fave[bt]++;
                selector[nSelectors] = (char) bt;
                nSelectors++;

                /*
                  Increment the symbol frequencies for the selected table.
                */
                for (i = gs; i <= ge; i++) {
                    rfreq[bt][szptr[i]]++;
                }

                gs = ge + 1;
            }

            /*
              Recompute the tables based on the accumulated frequencies.
            */
            for (t = 0; t < nGroups; t++) {
                hbMakeCodeLengths(len[t], rfreq[t], alphaSize, 20);
            }
        }

        rfreq = null;
        fave = null;
        cost = null;

        if (!(nGroups < 8)) {
            panic();
        }
        if (!(nSelectors < 32768 && nSelectors <= (2 + (900000 / G_SIZE)))) {
            panic();
        }


        /* Compute MTF values for the selectors. */
        {
            char[] pos = new char[N_GROUPS];
            char ll_i, tmp2, tmp;
            for (i = 0; i < nGroups; i++) {
                pos[i] = (char) i;
            }
            for (i = 0; i < nSelectors; i++) {
                ll_i = selector[i];
                j = 0;
                tmp = pos[j];
                while (ll_i != tmp) {
                    j++;
                    tmp2 = tmp;
                    tmp = pos[j];
                    pos[j] = tmp2;
                }
                pos[0] = tmp;
                selectorMtf[i] = (char) j;
            }
        }

        int[][] code = new int[N_GROUPS][MAX_ALPHA_SIZE];

        /* Assign actual codes for the tables. */
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
            if (maxLen > 20) {
                panic();
            }
            if (minLen < 1) {
                panic();
            }
            hbAssignCodes(code[t], len[t], minLen, maxLen, alphaSize);
        }

        /* Transmit the mapping table. */
        {
            boolean[] inUse16 = new boolean[16];
            for (i = 0; i < 16; i++) {
                inUse16[i] = false;
                for (j = 0; j < 16; j++) {
                    if (inUse[i * 16 + j]) {
                        inUse16[i] = true;
                    }
                }
            }

            for (i = 0; i < 16; i++) {
                if (inUse16[i]) {
                    bsW(1, 1);
                } else {
                    bsW(1, 0);
                }
            }

            for (i = 0; i < 16; i++) {
                if (inUse16[i]) {
                    for (j = 0; j < 16; j++) {
                        if (inUse[i * 16 + j]) {
                            bsW(1, 1);
                        } else {
                            bsW(1, 0);
                        }
                    }
                }
            }

        }

        /* Now the selectors. */
        bsW (3, nGroups);
        bsW (15, nSelectors);
        for (i = 0; i < nSelectors; i++) {
            for (j = 0; j < selectorMtf[i]; j++) {
                bsW(1, 1);
            }
            bsW(1, 0);
        }

        /* Now the coding tables. */
        for (t = 0; t < nGroups; t++) {
            int curr = len[t][0];
            bsW(5, curr);
            for (i = 0; i < alphaSize; i++) {
                while (curr < len[t][i]) {
                    bsW(2, 2);
                    curr++; /* 10 */
                }
                while (curr > len[t][i]) {
                    bsW(2, 3);
                    curr--; /* 11 */
                }
                bsW (1, 0);
            }
        }

        /* And finally, the block data proper */
        selCtr = 0;
        gs = 0;
        while (true) {
            if (gs >= nMTF) {
                break;
            }
            ge = gs + G_SIZE - 1;
            if (ge >= nMTF) {
                ge = nMTF - 1;
            }
            for (i = gs; i <= ge; i++) {
                bsW(len[selector[selCtr]][szptr[i]],
                    code[selector[selCtr]][szptr[i]]);
            }

            gs = ge + 1;
            selCtr++;
        }
        if (!(selCtr == nSelectors)) {
            panic();
        }
    }

    private void moveToFrontCodeAndSend () throws IOException {
        bsPutIntVS(24, origPtr);
        generateMTFValues();
        sendMTFValues();
    }

    private OutputStream bsStream;

    private void simpleSort(int lo, int hi, int d) {
        int i, j, h, bigN, hp;
        int v;

        bigN = hi - lo + 1;
        if (bigN < 2) {
            return;
        }

        hp = 0;
        while (incs[hp] < bigN) {
            hp++;
        }
        hp--;

        for (; hp >= 0; hp--) {
            h = incs[hp];

            i = lo + h;
            while (true) {
                /* copy 1 */
                if (i > hi) {
                    break;
                }
                v = zptr[i];
                j = i;
                while (fullGtU(zptr[j - h] + d, v + d)) {
                    zptr[j] = zptr[j - h];
                    j = j - h;
                    if (j <= (lo + h - 1)) {
                        break;
                    }
                }
                zptr[j] = v;
                i++;

                /* copy 2 */
                if (i > hi) {
                    break;
                }
                v = zptr[i];
                j = i;
                while (fullGtU(zptr[j - h] + d, v + d)) {
                    zptr[j] = zptr[j - h];
                    j = j - h;
                    if (j <= (lo + h - 1)) {
                        break;
                    }
                }
                zptr[j] = v;
                i++;

                /* copy 3 */
                if (i > hi) {
                    break;
                }
                v = zptr[i];
                j = i;
                while (fullGtU(zptr[j - h] + d, v + d)) {
                    zptr[j] = zptr[j - h];
                    j = j - h;
                    if (j <= (lo + h - 1)) {
                        break;
                    }
                }
                zptr[j] = v;
                i++;

                if (workDone > workLimit && firstAttempt) {
                    return;
                }
            }
        }
    }

    private void vswap(int p1, int p2, int n) {
        int temp = 0;
        while (n > 0) {
            temp = zptr[p1];
            zptr[p1] = zptr[p2];
            zptr[p2] = temp;
            p1++;
            p2++;
            n--;
        }
    }

    private char med3(char a, char b, char c) {
        char t;
        if (a > b) {
            t = a;
            a = b;
            b = t;
        }
        if (b > c) {
            t = b;
            b = c;
            c = t;
        }
        if (a > b) {
            b = a;
        }
        return b;
    }

    private static class StackElem {
        int ll;
        int hh;
        int dd;
    }

    private void qSort3(int loSt, int hiSt, int dSt) {
        int unLo, unHi, ltLo, gtHi, med, n, m;
        int sp, lo, hi, d;
        StackElem[] stack = new StackElem[QSORT_STACK_SIZE];
        for (int count = 0; count < QSORT_STACK_SIZE; count++) {
            stack[count] = new StackElem();
        }

        sp = 0;

        stack[sp].ll = loSt;
        stack[sp].hh = hiSt;
        stack[sp].dd = dSt;
        sp++;

        while (sp > 0) {
            if (sp >= QSORT_STACK_SIZE) {
                panic();
            }

            sp--;
            lo = stack[sp].ll;
            hi = stack[sp].hh;
            d = stack[sp].dd;

            if (hi - lo < SMALL_THRESH || d > DEPTH_THRESH) {
                simpleSort(lo, hi, d);
                if (workDone > workLimit && firstAttempt) {
                    return;
                }
                continue;
            }

            med = med3(block[zptr[lo] + d + 1],
                       block[zptr[hi            ] + d  + 1],
                       block[zptr[(lo + hi) >> 1] + d + 1]);

            unLo = ltLo = lo;
            unHi = gtHi = hi;

            while (true) {
                while (true) {
                    if (unLo > unHi) {
                        break;
                    }
                    n = ((int) block[zptr[unLo] + d + 1]) - med;
                    if (n == 0) {
                        int temp = 0;
                        temp = zptr[unLo];
                        zptr[unLo] = zptr[ltLo];
                        zptr[ltLo] = temp;
                        ltLo++;
                        unLo++;
                        continue;
                    };
                    if (n >  0) {
                        break;
                    }
                    unLo++;
                }
                while (true) {
                    if (unLo > unHi) {
                        break;
                    }
                    n = ((int) block[zptr[unHi] + d + 1]) - med;
                    if (n == 0) {
                        int temp = 0;
                        temp = zptr[unHi];
                        zptr[unHi] = zptr[gtHi];
                        zptr[gtHi] = temp;
                        gtHi--;
                        unHi--;
                        continue;
                    };
                    if (n <  0) {
                        break;
                    }
                    unHi--;
                }
                if (unLo > unHi) {
                    break;
                }
                int temp = 0;
                temp = zptr[unLo];
                zptr[unLo] = zptr[unHi];
                zptr[unHi] = temp;
                unLo++;
                unHi--;
            }

            if (gtHi < ltLo) {
                stack[sp].ll = lo;
                stack[sp].hh = hi;
                stack[sp].dd = d + 1;
                sp++;
                continue;
            }

            n = ((ltLo - lo) < (unLo - ltLo)) ? (ltLo - lo) : (unLo - ltLo);
            vswap(lo, unLo - n, n);
            m = ((hi - gtHi) < (gtHi - unHi)) ? (hi - gtHi) : (gtHi - unHi);
            vswap(unLo, hi - m + 1, m);

            n = lo + unLo - ltLo - 1;
            m = hi - (gtHi - unHi) + 1;

            stack[sp].ll = lo;
            stack[sp].hh = n;
            stack[sp].dd = d;
            sp++;

            stack[sp].ll = n + 1;
            stack[sp].hh = m - 1;
            stack[sp].dd = d + 1;
            sp++;

            stack[sp].ll = m;
            stack[sp].hh = hi;
            stack[sp].dd = d;
            sp++;
        }
    }

    private void mainSort() {
        int i, j, ss, sb;
        int[] runningOrder = new int[256];
        int[] copy = new int[256];
        boolean[] bigDone = new boolean[256];
        int c1, c2;
        int numQSorted;

        /*
          In the various block-sized structures, live data runs
          from 0 to last+NUM_OVERSHOOT_BYTES inclusive.  First,
          set up the overshoot area for block.
        */

        //   if (verbosity >= 4) fprintf ( stderr, "   sort initialise ...\n" );
        for (i = 0; i < NUM_OVERSHOOT_BYTES; i++) {
            block[last + i + 2] = block[(i % (last + 1)) + 1];
        }
        for (i = 0; i <= last + NUM_OVERSHOOT_BYTES; i++) {
            quadrant[i] = 0;
        }

        block[0] = (char) (block[last + 1]);

        if (last < 4000) {
            /*
              Use simpleSort(), since the full sorting mechanism
              has quite a large constant overhead.
            */
            for (i = 0; i <= last; i++) {
                zptr[i] = i;
            }
            firstAttempt = false;
            workDone = workLimit = 0;
            simpleSort(0, last, 0);
        } else {
            numQSorted = 0;
            for (i = 0; i <= 255; i++) {
                bigDone[i] = false;
            }

            for (i = 0; i <= 65536; i++) {
                ftab[i] = 0;
            }

            c1 = block[0];
            for (i = 0; i <= last; i++) {
                c2 = block[i + 1];
                ftab[(c1 << 8) + c2]++;
                c1 = c2;
            }

            for (i = 1; i <= 65536; i++) {
                ftab[i] += ftab[i - 1];
            }

            c1 = block[1];
            for (i = 0; i < last; i++) {
                c2 = block[i + 2];
                j = (c1 << 8) + c2;
                c1 = c2;
                ftab[j]--;
                zptr[ftab[j]] = i;
            }

            j = ((block[last + 1]) << 8) + (block[1]);
            ftab[j]--;
            zptr[ftab[j]] = last;

            /*
              Now ftab contains the first loc of every small bucket.
              Calculate the running order, from smallest to largest
              big bucket.
            */

            for (i = 0; i <= 255; i++) {
                runningOrder[i] = i;
            }

            {
                int vv;
                int h = 1;
                do {
                    h = 3 * h + 1;
                }
                while (h <= 256);
                do {
                    h = h / 3;
                    for (i = h; i <= 255; i++) {
                        vv = runningOrder[i];
                        j = i;
                        while ((ftab[((runningOrder[j - h]) + 1) << 8]
                                - ftab[(runningOrder[j - h]) << 8]) >
                               (ftab[((vv) + 1) << 8] - ftab[(vv) << 8])) {
                            runningOrder[j] = runningOrder[j - h];
                            j = j - h;
                            if (j <= (h - 1)) {
                                break;
                            }
                        }
                        runningOrder[j] = vv;
                    }
                } while (h != 1);
            }

            /*
              The main sorting loop.
            */
            for (i = 0; i <= 255; i++) {

                /*
                  Process big buckets, starting with the least full.
                */
                ss = runningOrder[i];

                /*
                  Complete the big bucket [ss] by quicksorting
                  any unsorted small buckets [ss, j].  Hopefully
                  previous pointer-scanning phases have already
                  completed many of the small buckets [ss, j], so
                  we don't have to sort them at all.
                */
                for (j = 0; j <= 255; j++) {
                    sb = (ss << 8) + j;
                    if (!((ftab[sb] & SETMASK) == SETMASK)) {
                        int lo = ftab[sb] & CLEARMASK;
                        int hi = (ftab[sb + 1] & CLEARMASK) - 1;
                        if (hi > lo) {
                            qSort3(lo, hi, 2);
                            numQSorted += (hi - lo + 1);
                            if (workDone > workLimit && firstAttempt) {
                                return;
                            }
                        }
                        ftab[sb] |= SETMASK;
                    }
                }

                /*
                  The ss big bucket is now done.  Record this fact,
                  and update the quadrant descriptors.  Remember to
                  update quadrants in the overshoot area too, if
                  necessary.  The "if (i < 255)" test merely skips
                  this updating for the last bucket processed, since
                  updating for the last bucket is pointless.
                */
                bigDone[ss] = true;

                if (i < 255) {
                    int bbStart  = ftab[ss << 8] & CLEARMASK;
                    int bbSize   = (ftab[(ss + 1) << 8] & CLEARMASK) - bbStart;
                    int shifts   = 0;

                    while ((bbSize >> shifts) > 65534) {
                        shifts++;
                    }

                    for (j = 0; j < bbSize; j++) {
                        int a2update = zptr[bbStart + j];
                        int qVal = (j >> shifts);
                        quadrant[a2update] = qVal;
                        if (a2update < NUM_OVERSHOOT_BYTES) {
                            quadrant[a2update + last + 1] = qVal;
                        }
                    }

                    if (!(((bbSize - 1) >> shifts) <= 65535)) {
                        panic();
                    }
                }

                /*
                  Now scan this big bucket so as to synthesise the
                  sorted order for small buckets [t, ss] for all t != ss.
                */
                for (j = 0; j <= 255; j++) {
                    copy[j] = ftab[(j << 8) + ss] & CLEARMASK;
                }

                for (j = ftab[ss << 8] & CLEARMASK;
                     j < (ftab[(ss + 1) << 8] & CLEARMASK); j++) {
                    c1 = block[zptr[j]];
                    if (!bigDone[c1]) {
                        zptr[copy[c1]] = zptr[j] == 0 ? last : zptr[j] - 1;
                        copy[c1]++;
                    }
                }

                for (j = 0; j <= 255; j++) {
                    ftab[(j << 8) + ss] |= SETMASK;
                }
            }
        }
    }

    private void randomiseBlock() {
        int i;
        int rNToGo = 0;
        int rTPos  = 0;
        for (i = 0; i < 256; i++) {
            inUse[i] = false;
        }

        for (i = 0; i <= last; i++) {
            if (rNToGo == 0) {
                rNToGo = (char) rNums[rTPos];
                rTPos++;
                if (rTPos == 512) {
                    rTPos = 0;
                }
            }
            rNToGo--;
            block[i + 1] ^= ((rNToGo == 1) ? 1 : 0);
            // handle 16 bit signed numbers
            block[i + 1] &= 0xFF;

            inUse[block[i + 1]] = true;
        }
    }

    private void doReversibleTransformation() {
        int i;

        workLimit = workFactor * last;
        workDone = 0;
        blockRandomised = false;
        firstAttempt = true;

        mainSort();

        if (workDone > workLimit && firstAttempt) {
            randomiseBlock();
            workLimit = workDone = 0;
            blockRandomised = true;
            firstAttempt = false;
            mainSort();
        }

        origPtr = -1;
        for (i = 0; i <= last; i++) {
            if (zptr[i] == 0) {
                origPtr = i;
                break;
            }
        };

        if (origPtr == -1) {
            panic();
        }
    }

    private boolean fullGtU(int i1, int i2) {
        int k;
        char c1, c2;
        int s1, s2;

        c1 = block[i1 + 1];
        c2 = block[i2 + 1];
        if (c1 != c2) {
            return (c1 > c2);
        }
        i1++;
        i2++;

        c1 = block[i1 + 1];
        c2 = block[i2 + 1];
        if (c1 != c2) {
            return (c1 > c2);
        }
        i1++;
        i2++;

        c1 = block[i1 + 1];
        c2 = block[i2 + 1];
        if (c1 != c2) {
            return (c1 > c2);
        }
        i1++;
        i2++;

        c1 = block[i1 + 1];
        c2 = block[i2 + 1];
        if (c1 != c2) {
            return (c1 > c2);
        }
        i1++;
        i2++;

        c1 = block[i1 + 1];
        c2 = block[i2 + 1];
        if (c1 != c2) {
            return (c1 > c2);
        }
        i1++;
        i2++;

        c1 = block[i1 + 1];
        c2 = block[i2 + 1];
        if (c1 != c2) {
            return (c1 > c2);
        }
        i1++;
        i2++;

        k = last + 1;

        do {
            c1 = block[i1 + 1];
            c2 = block[i2 + 1];
            if (c1 != c2) {
                return (c1 > c2);
            }
            s1 = quadrant[i1];
            s2 = quadrant[i2];
            if (s1 != s2) {
                return (s1 > s2);
            }
            i1++;
            i2++;

            c1 = block[i1 + 1];
            c2 = block[i2 + 1];
            if (c1 != c2) {
                return (c1 > c2);
            }
            s1 = quadrant[i1];
            s2 = quadrant[i2];
            if (s1 != s2) {
                return (s1 > s2);
            }
            i1++;
            i2++;

            c1 = block[i1 + 1];
            c2 = block[i2 + 1];
            if (c1 != c2) {
                return (c1 > c2);
            }
            s1 = quadrant[i1];
            s2 = quadrant[i2];
            if (s1 != s2) {
                return (s1 > s2);
            }
            i1++;
            i2++;

            c1 = block[i1 + 1];
            c2 = block[i2 + 1];
            if (c1 != c2) {
                return (c1 > c2);
            }
            s1 = quadrant[i1];
            s2 = quadrant[i2];
            if (s1 != s2) {
                return (s1 > s2);
            }
            i1++;
            i2++;

            if (i1 > last) {
                i1 -= last;
                i1--;
            };
            if (i2 > last) {
                i2 -= last;
                i2--;
            };

            k -= 4;
            workDone++;
        } while (k >= 0);

        return false;
    }

    /*
      Knuth's increments seem to work better
      than Incerpi-Sedgewick here.  Possibly
      because the number of elems to sort is
      usually small, typically <= 20.
    */
    private int[] incs = { 1, 4, 13, 40, 121, 364, 1093, 3280,
                           9841, 29524, 88573, 265720,
                           797161, 2391484 };

    private void allocateCompressStructures () {
        int n = baseBlockSize * blockSize100k;
        block = new char[(n + 1 + NUM_OVERSHOOT_BYTES)];
        quadrant = new int[(n + NUM_OVERSHOOT_BYTES)];
        zptr = new int[n];
        ftab = new int[65537];

        if (block == null || quadrant == null || zptr == null
            || ftab == null) {
            //int totalDraw = (n + 1 + NUM_OVERSHOOT_BYTES) + (n + NUM_OVERSHOOT_BYTES) + n + 65537;
            //compressOutOfMemory ( totalDraw, n );
        }

        /*
          The back end needs a place to store the MTF values
          whilst it calculates the coding tables.  We could
          put them in the zptr array.  However, these values
          will fit in a short, so we overlay szptr at the
          start of zptr, in the hope of reducing the number
          of cache misses induced by the multiple traversals
          of the MTF values when calculating coding tables.
          Seems to improve compression speed by about 1%.
        */
        //    szptr = zptr;


        szptr = new short[2 * n];
    }

    private void generateMTFValues() {
        char[] yy = new char[256];
        int  i, j;
        char tmp;
        char tmp2;
        int zPend;
        int wr;
        int EOB;

        makeMaps();
        EOB = nInUse + 1;

        for (i = 0; i <= EOB; i++) {
            mtfFreq[i] = 0;
        }

        wr = 0;
        zPend = 0;
        for (i = 0; i < nInUse; i++) {
            yy[i] = (char) i;
        }


        for (i = 0; i <= last; i++) {
            char ll_i;

            ll_i = unseqToSeq[block[zptr[i]]];

            j = 0;
            tmp = yy[j];
            while (ll_i != tmp) {
                j++;
                tmp2 = tmp;
                tmp = yy[j];
                yy[j] = tmp2;
            };
            yy[0] = tmp;

            if (j == 0) {
                zPend++;
            } else {
                if (zPend > 0) {
                    zPend--;
                    while (true) {
                        switch (zPend % 2) {
                        case 0:
                            szptr[wr] = (short) RUNA;
                            wr++;
                            mtfFreq[RUNA]++;
                            break;
                        case 1:
                            szptr[wr] = (short) RUNB;
                            wr++;
                            mtfFreq[RUNB]++;
                            break;
                        };
                        if (zPend < 2) {
                            break;
                        }
                        zPend = (zPend - 2) / 2;
                    };
                    zPend = 0;
                }
                szptr[wr] = (short) (j + 1);
                wr++;
                mtfFreq[j + 1]++;
            }
        }

        if (zPend > 0) {
            zPend--;
            while (true) {
                switch (zPend % 2) {
                case 0:
                    szptr[wr] = (short) RUNA;
                    wr++;
                    mtfFreq[RUNA]++;
                    break;
                case 1:
                    szptr[wr] = (short) RUNB;
                    wr++;
                    mtfFreq[RUNB]++;
                    break;
                }
                if (zPend < 2) {
                    break;
                }
                zPend = (zPend - 2) / 2;
            }
        }

        szptr[wr] = (short) EOB;
        wr++;
        mtfFreq[EOB]++;

        nMTF = wr;
    }
}


