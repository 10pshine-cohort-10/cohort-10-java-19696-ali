package com.shine.book;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContactServiceTest {
    private final ContactRepository repo = mock(ContactRepository.class);
    private final ContactService service = new ContactService(repo);

    @Test void rejectsOtherOwnersBeforeWriting() {
        when(repo.findByIdAndOwnerId(18, 4)).thenReturn(Optional.empty());
        ApiError ex = assertThrows(ApiError.class, () -> service.delete(4, 18));
        assertEquals(404, ex.status.value());
        verify(repo, never()).delete(any(Contact.class));
    }
    @Test void invalidPagesDoNotQueryDatabase() {
        assertThrows(ApiError.class, () -> service.search(1, "", -1));
        verifyNoInteractions(repo);
    }
}
