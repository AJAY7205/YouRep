package com.learning.ytrep.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressTrackingInputStream extends FilterInputStream {

    private final long totalBytes;
    private long bytesRead = 0;
    private final ProgressListener progressListener;
    private int lastReportedPercentage = -1;

    public interface ProgressListener {
        void onProgress(long bytesRead, long totalBytes, int percentage);
    }

    public ProgressTrackingInputStream(InputStream in, long totalBytes, ProgressListener progressListener) {
        super(in);
        this.totalBytes = totalBytes;
        this.progressListener = progressListener;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            bytesRead++;
            notifyProgress();
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int count = super.read(b, off, len);
        if (count > 0) {
            bytesRead += count;
            notifyProgress();
        }
        return count;
    }

    private void notifyProgress() {
        int currentPercentage = (int) ((bytesRead * 100.0) / totalBytes);
        
        // Only notify when percentage changes (1%, 2%, 3%...)
        if (currentPercentage != lastReportedPercentage && progressListener != null) {
            lastReportedPercentage = currentPercentage;
            progressListener.onProgress(bytesRead, totalBytes, currentPercentage);
        }
    }
}