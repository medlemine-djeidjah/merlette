package com.merlette.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FileStorageServiceTest {

    @Test
    void detectsJpegPngWebp() {
        assertEquals("jpg", FileStorageService.detectImageType(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));
        assertEquals("png", FileStorageService.detectImageType(new byte[] {(byte) 0x89, 'P', 'N', 'G'}));
        assertEquals("webp", FileStorageService.detectImageType("RIFF0000WEBP".getBytes()));
    }

    @Test
    void rejectsText() {
        assertNull(FileStorageService.detectImageType("hello world".getBytes()));
    }
}
