package com.merlette.backend.observation;

import com.merlette.backend.api.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PhotoStorageTests {
    @TempDir Path directory;
    @Test void rejectsDisguisedTextAndRemovesEarlierFilesOnRollback() throws Exception {
        var storage = new PhotoStorage(directory.toString());
        var photo = new MockMultipartFile("photos", "photo.png", "image/png", new byte[]{(byte)137,80,78,71,13,10,26,10});
        var fake = new MockMultipartFile("photos", "fake.jpg", "image/jpeg", "not an image".getBytes());
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(ApiException.class, () -> storage.save(List.of(photo, fake)));
            try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
