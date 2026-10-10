package application.adapters.rest.dtos.responses;

import java.util.List;

/**
 * Envelope of a paged listing.
 *
 * <p>Pagination lives entirely in the REST adapter: the output ports return whole collections
 * because the domain never expresses "page two of the orders" as a concept, so introducing page
 * parameters down to the repositories would push a transport concern into the model (RD-ALC-01).
 * The slice is therefore taken here, over the list the entry port already returned.</p>
 *
 * <p>The consequence is explicit: the query still reads every matching row before the page is cut,
 * so this bounds the size of the <em>response</em>, not the work of the database. It keeps a client
 * from having to swallow an unbounded list, which is what it is for; it is not a performance
 * optimization, and a catalogue large enough to need one would require paged output ports.</p>
 *
 * @param content       elements of the requested page.
 * @param page          zero-based index of the returned page.
 * @param size          maximum number of elements per page.
 * @param totalElements total number of elements across every page.
 * @param totalPages    number of pages the total is split into, at least 1.
 * @param first         whether this is the first page.
 * @param last          whether this is the last page.
 */
public record PageResponse<T>(List<T> content,
                              int page,
                              int size,
                              int totalElements,
                              int totalPages,
                              boolean first,
                              boolean last) {

    /**
     * Largest page a client may ask for. An unbounded {@code size} would defeat the purpose of
     * paginating at all.
     */
    public static final int MAXIMUM_SIZE = 200;

    /**
     * Cuts the requested page out of a complete listing.
     *
     * <p>A page beyond the end is not an error: it returns an empty page, which is what lets a
     * client walk forward until {@code last} without having to know the total upfront.</p>
     *
     * @throws IllegalArgumentException when the page is negative or the size is outside
     *                                  {@code [1, MAXIMUM_SIZE]}, which the REST layer reports as
     *                                  HTTP 400.
     */
    public static <T> PageResponse<T> of(List<T> elements, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative, received " + page);
        }
        if (size < 1 || size > MAXIMUM_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAXIMUM_SIZE + ", received " + size);
        }
        int totalElements = elements.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalElements / size));
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);
        return new PageResponse<>(List.copyOf(elements.subList(from, to)), page, size, totalElements, totalPages,
                page == 0, page >= totalPages - 1);
    }
}
