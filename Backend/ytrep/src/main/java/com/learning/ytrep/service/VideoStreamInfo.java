package com.learning.ytrep.service;

import java.io.InputStream;

public record VideoStreamInfo(InputStream stream, long totalSize, long start, long end, long contentLength) {
}
