package application.adapters.rest.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.adapters.rest.dtos.responses.PageResponse;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers the slicing arithmetic of the page envelope, which is the only logic the REST adapter owns
 * by itself: everything else there delegates to an entry port.
 */
@DisplayName("Page envelope of the listing endpoints")
class PageResponseTest {

    private static final List<String> SEVEN = IntStream.rangeClosed(1, 7)
            .mapToObj(index -> "e" + index)
            .toList();

    @Nested
    @DisplayName("Slicing")
    class Slicing {

        @Test
        void firstPageCarriesTheLeadingElementsAndTheTotals() {
            PageResponse<String> page = PageResponse.of(SEVEN, 0, 3);

            assertEquals(List.of("e1", "e2", "e3"), page.content());
            assertEquals(7, page.totalElements());
            assertEquals(3, page.totalPages());
            assertTrue(page.first());
            assertFalse(page.last());
        }

        /**
         * The last page is the one that does not fill: it must not pad, and it must report itself as
         * last so a client can stop without knowing the total beforehand.
         */
        @Test
        void lastPageIsShorterAndFlagsItself() {
            PageResponse<String> page = PageResponse.of(SEVEN, 2, 3);

            assertEquals(List.of("e7"), page.content());
            assertFalse(page.first());
            assertTrue(page.last());
        }

        @Test
        void pageBeyondTheEndIsEmptyRatherThanAnError() {
            PageResponse<String> page = PageResponse.of(SEVEN, 9, 3);

            assertTrue(page.content().isEmpty());
            assertEquals(7, page.totalElements());
            assertTrue(page.last());
        }

        /**
         * An empty listing still has one page, otherwise {@code last} would be false on a result that
         * has nothing after it.
         */
        @Test
        void emptyListingReportsOnePage() {
            PageResponse<String> page = PageResponse.of(List.of(), 0, 20);

            assertEquals(1, page.totalPages());
            assertTrue(page.first());
            assertTrue(page.last());
        }

        @Test
        void sizeLargerThanTheListingReturnsEverythingInOnePage() {
            PageResponse<String> page = PageResponse.of(SEVEN, 0, 50);

            assertEquals(7, page.content().size());
            assertEquals(1, page.totalPages());
            assertTrue(page.last());
        }

        @Test
        void contentIsImmutable() {
            PageResponse<String> page = PageResponse.of(SEVEN, 0, 3);

            assertThrows(UnsupportedOperationException.class, () -> page.content().add("intruder"));
        }
    }

    @Nested
    @DisplayName("Rejected parameters")
    class RejectedParameters {

        @Test
        void negativePageIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> PageResponse.of(SEVEN, -1, 3));
        }

        @Test
        void sizeBelowOneIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> PageResponse.of(SEVEN, 0, 0));
        }

        /**
         * An unbounded size would defeat the point of paginating, so it is capped rather than
         * honoured.
         */
        @Test
        void sizeAboveTheMaximumIsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> PageResponse.of(SEVEN, 0, PageResponse.MAXIMUM_SIZE + 1));
        }

        @Test
        void maximumSizeItselfIsAccepted() {
            assertEquals(7, PageResponse.of(SEVEN, 0, PageResponse.MAXIMUM_SIZE).content().size());
        }
    }
}
