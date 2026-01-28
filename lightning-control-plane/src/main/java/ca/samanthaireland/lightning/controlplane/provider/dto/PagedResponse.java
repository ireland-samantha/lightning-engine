/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ca.samanthaireland.lightning.controlplane.provider.dto;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * @param <T> the type of items in the page
 */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        int totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    /**
     * Creates a paginated response from a list.
     *
     * @param allItems all items to paginate
     * @param page     page number (0-indexed)
     * @param pageSize items per page
     * @param <T>      item type
     * @return the paginated response
     */
    public static <T> PagedResponse<T> of(List<T> allItems, int page, int pageSize) {
        int totalItems = allItems.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        List<T> pageItems = fromIndex < totalItems
                ? allItems.subList(fromIndex, toIndex)
                : List.of();

        return new PagedResponse<>(
                pageItems,
                page,
                pageSize,
                totalItems,
                totalPages,
                page < totalPages - 1,
                page > 0
        );
    }
}
