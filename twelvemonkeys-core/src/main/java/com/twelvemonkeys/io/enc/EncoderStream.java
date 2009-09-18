/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.io.enc;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * An {@code OutputStream} that provides on-the-fly encoding to an underlying
 * stream.
 * <p/>
 * @see DecoderStream
 * @see Encoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/EncoderStream.java#2 $
 */
public final class EncoderStream extends FilterOutputStream {

    protected final Encoder mEncoder;
    private final boolean mFlushOnWrite;

    protected int mBufferPos;
    protected final byte[] mBuffer;

    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.
     *
     * @param pStream the underlying output stream
     * @param pEncoder the encoder to use
     */
    public EncoderStream(final OutputStream pStream, final Encoder pEncoder) {
        this(pStream, pEncoder, false);
    }

    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.
     *
     * @param pStream the underlying output stream
     * @param pEncoder the encoder to use
     * @param pFlushOnWrite if {@code true}, calls to the byte-array
     * {@code write} methods will automatically flush the buffer.
     */
    public EncoderStream(final OutputStream pStream, final Encoder pEncoder, final boolean pFlushOnWrite) {
        super(pStream);

        mEncoder = pEncoder;
        mFlushOnWrite = pFlushOnWrite;

        mBuffer = new byte[1024];
        mBufferPos = 0;
    }

    public void close() throws IOException {
        flush();
        super.close();
    }

    public void flush() throws IOException {
        encodeBuffer();
        super.flush();
    }

    private void encodeBuffer() throws IOException {
        if (mBufferPos != 0) {
            // Make sure all remaining data in buffer is written to the stream
            mEncoder.encode(out, mBuffer, 0, mBufferPos);

            // Reset buffer
            mBufferPos = 0;
        }
    }

    public final void write(final byte[] pBytes) throws IOException {
        write(pBytes, 0, pBytes.length);
    }

    // TODO: Verify that this works for the general case (it probably won't)...
    // TODO: We might need a way to explicitly flush the encoder, or specify
    // that the encoder can't buffer. In that case, the encoder should probably
    // tell the EncoderStream how large buffer it prefers... 
    public void write(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
        if (!mFlushOnWrite && mBufferPos + pLength < mBuffer.length) {
            // Buffer data
            System.arraycopy(pBytes, pOffset, mBuffer, mBufferPos, pLength);
            mBufferPos += pLength;
        }
        else {
            // Encode data already in the buffer
            if (mBufferPos != 0) {
                encodeBuffer();
            }

            // Encode rest without buffering
            mEncoder.encode(out, pBytes, pOffset, pLength);
        }
    }

    public void write(final int pByte) throws IOException {
        if (mBufferPos >= mBuffer.length - 1) {
            encodeBuffer(); // Resets mBufferPos to 0
        }

        mBuffer[mBufferPos++] = (byte) pByte;
    }
}
