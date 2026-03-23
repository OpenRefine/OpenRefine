/*******************************************************************************
 * Copyright (C) 2025, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.clustering.knn;

import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

/**
 * Computes a Normalized Compression Distance (NCD) between two strings using Java's built-in DEFLATE compressor.
 * <p>
 * This replaces the PPM-based arithmetic coding approach from the Vicino library with a faster, well-maintained JDK
 * implementation. The NCD formula used matches Vicino's {@code PseudoMetricDistance}:
 * <p>
 * {@code d(a, b) = 10 * ((Z(a+b) + Z(b+a)) / (Z(a+a) + Z(b+b)) - 1)}
 * <p>
 * where {@code Z(s)} is the compressed size of the string {@code s}.
 */
public class DeflateNCDDistance implements SimilarityDistance {

    @Override
    public double compute(String a, String b) {
        double zab = compressedSize(a + b);
        double zba = compressedSize(b + a);
        double zaa = compressedSize(a + a);
        double zbb = compressedSize(b + b);
        return 10.0 * ((zab + zba) / (zaa + zbb) - 1.0);
    }

    private int compressedSize(String s) {
        byte[] input = s.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(input);
            deflater.finish();
            int totalSize = 0;
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                totalSize += deflater.deflate(buffer);
            }
            return totalSize;
        } finally {
            deflater.end();
        }
    }
}
