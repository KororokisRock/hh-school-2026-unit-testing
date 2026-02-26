package ru.hh.school.unittesting.homework;

import java.util.stream.Stream;

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


    static Stream<Arguments> calculateSource() {
        return Stream.of(
            Arguments.of(4, false, false, 2.0),
            Arguments.of(4, false, true, 1.6),
            Arguments.of(4, true, false, 3.0),
            Arguments.of(4, true, true, 2.4)
        );
    }

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
        assertEquals(libraryManager.getAvailableCopies("book7"), 0);
    }

    @ParameterizedTest
    @CsvSource({
        "book1, 1",
        "book2, 10",
        "book5, 0"
    })
    void testGetAvailableCopiesIfAmmountIsPositiveOrZero(String bookId, Integer expectedCopies) {
        assertEquals(libraryManager.getAvailableCopies(bookId), expectedCopies);
    }

    @Test
    void testIfUserAccountNotActive() {
        when(userService.isUserActive("user2")).thenReturn(false);

        assertFalse(libraryManager.borrowBook("book1", "user2"));
    }

    @ParameterizedTest
    @CsvSource({
        "book4, user1",
        "book5, user1"
    })
    void testIfAmmountBookZeroOrNegative(String bookId, String userId) {
        assertFalse(libraryManager.borrowBook(bookId, userId));
    }

    @ParameterizedTest
    @CsvSource({
        "book1, user1, 0",
        "book2, user1, 9"
    })
    void testBorrowedInputFine(String bookId, String userId, Integer expectedCopies) {
        when(userService.isUserActive("user1")).thenReturn(true);

        assertTrue(libraryManager.borrowBook(bookId, userId));

        assertEquals(expectedCopies, libraryManager.getAvailableCopies(bookId));
    }

    @Test
    void testIfBookNotBorrowedOrBorrowedAnotherUser() {
        libraryManager.borrowBook("book3", "user1");

        assertFalse(libraryManager.returnBook("book2", "user2"));
        assertFalse(libraryManager.returnBook("book3", "user2"));
    }

    @Test
    void testReturnBookInputFine() {
        when(userService.isUserActive("user1")).thenReturn(true);

        assertTrue(libraryManager.borrowBook("book1", "user1"));
        assertTrue(libraryManager.returnBook("book1", "user1"));

        assertEquals(libraryManager.getAvailableCopies("book1"), 1);
    }

    @Test
    void testCalculateDynamicLateFeeThrowingException() {
        var exception = assertThrows(IllegalArgumentException.class, () -> libraryManager.calculateDynamicLateFee(-2, true, true));
        assertEquals("Overdue days cannot be negative.", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("calculateSource")
    void testCalculateDynamicLateFeeBestsellerPremium(int overdueDays, boolean isBesteller, boolean isPremiumMember, double expectedCalculate) {
        assertEquals(libraryManager.calculateDynamicLateFee(4, false, false), 2.0);

        assertEquals(libraryManager.calculateDynamicLateFee(4, false, true), 1.6);

        assertEquals(libraryManager.calculateDynamicLateFee(4, true, false), 3.0);

        assertEquals(libraryManager.calculateDynamicLateFee(4, true, true), 2.4);
    }
}
