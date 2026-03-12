package ru.hh.school.unittesting.homework;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {
    
    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LibraryManager libraryManager;

    @BeforeEach
    void setUp() {
        libraryManager.addBook("book1", 1);
        libraryManager.addBook("book2", 10);
        libraryManager.addBook("book3", 12412);
        libraryManager.addBook("book4", -71);
        libraryManager.addBook("book5", 0);
    }


    @Test
    void testGetAvailableCopiesReturnZeroIfBookNotExist() {
        assertEquals(0, libraryManager.getAvailableCopies("book7"));
    }

    @Test
    void testGetAvailableCopiesIfAmountIsPositiveOrZero() {
        assertEquals(1, libraryManager.getAvailableCopies("book1"));
    }

    @Test
    void testIfUserAccountNotActive() {
        when(userService.isUserActive("user2")).thenReturn(false);

        assertFalse(libraryManager.borrowBook("book1", "user2"));

        verify(notificationService).notifyUser("user2", "Your account is not active.");
    }

    @ParameterizedTest
    @CsvSource({
        "book4, user1",
        "book5, user1"
    })
    void testIfAmountBookZeroOrNegative(String bookId, String userId) {
        when(userService.isUserActive("user1")).thenReturn(true);

        assertFalse(libraryManager.borrowBook(bookId, userId));
    }

    @Test
    void testBorrowedInputFine() {
        when(userService.isUserActive("user1")).thenReturn(true);

        assertTrue(libraryManager.borrowBook("book2", "user1"));

        verify(notificationService).notifyUser("user1", "You have borrowed the book: book2");

        assertEquals(9, libraryManager.getAvailableCopies("book2"));
    }

    @Test
    void testIfBookNotBorrowed() {
        assertFalse(libraryManager.returnBook("book2", "user2"));
    }

    @Test
    void testIfBookBorrowedAnotherUser() {
        when(userService.isUserActive("user1")).thenReturn(true);

        libraryManager.borrowBook("book3", "user1");
        assertFalse(libraryManager.returnBook("book3", "user2"));
    }

    @Test
    void testReturnBookInputFine() {
        when(userService.isUserActive("user1")).thenReturn(true);

        assertTrue(libraryManager.borrowBook("book1", "user1"));
        assertTrue(libraryManager.returnBook("book1", "user1"));

        verify(notificationService).notifyUser("user1", "You have returned the book: book1");

        assertEquals(1, libraryManager.getAvailableCopies("book1"));
    }

    @Test
    void testCalculateDynamicLateFeeThrowingException() {
        var exception = assertThrows(IllegalArgumentException.class, () -> libraryManager.calculateDynamicLateFee(-2, true, true));
        assertEquals("Overdue days cannot be negative.", exception.getMessage());
    }

    @Test
    void testCalculateDynamicLateFeeNotThrowIfDaysZero() {
        assertDoesNotThrow(() -> libraryManager.calculateDynamicLateFee(0, false, false));
    }

    static Stream<Arguments> calculateSource() {
        return Stream.of(
            Arguments.of(4, false, false, 2.0),
            Arguments.of(4, false, true, 1.6),
            Arguments.of(4, true, false, 3.0),
            Arguments.of(4, true, true, 2.4),
            Arguments.of(0, true, true, 0.0)
        );
    }

    @ParameterizedTest
    @MethodSource("calculateSource")
    void testCalculateDynamicLateFeeBestsellerPremium(int overdueDays, boolean isBestseller, boolean isPremiumMember, double expectedCalculate) {
        assertEquals(expectedCalculate, libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember));
    }
}
