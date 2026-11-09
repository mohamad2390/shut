package com.shut.showcase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShowcaseServiceTest {

    private static final Long USER = 1L;

    private final ShowcaseItemRepository repository = mock(ShowcaseItemRepository.class);
    private final ShowcaseService service = new ShowcaseService(repository);

    @Test
    void rejectsAFileThatIsAlreadyInThePortfolio() {
        when(repository.existsByUserIdAndFileUniqueId(USER, "u1")).thenReturn(true);

        assertThatThrownBy(() -> service.checkCanAdd(USER, ShowcaseCategory.PHOTOGRAPHY, "u1"))
                .isInstanceOfSatisfying(ShowcaseItemRejectedException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.reason())
                                .isEqualTo(ShowcaseItemRejectedException.Reason.DUPLICATE));
    }

    @Test
    void rejectsANewFileWhenTheCategoryIsFull() {
        when(repository.countByUserIdAndCategory(USER, ShowcaseCategory.AERIAL))
                .thenReturn((long) ShowcaseService.MAX_ITEMS_PER_CATEGORY);

        assertThatThrownBy(() -> service.checkCanAdd(USER, ShowcaseCategory.AERIAL, "u2"))
                .isInstanceOfSatisfying(ShowcaseItemRejectedException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.reason())
                                .isEqualTo(ShowcaseItemRejectedException.Reason.LIMIT_REACHED));
    }

    @Test
    void acceptsANewFileWhenThereIsRoom() {
        when(repository.countByUserIdAndCategory(USER, ShowcaseCategory.TEASER)).thenReturn(3L);

        assertThatCode(() -> service.checkCanAdd(USER, ShowcaseCategory.TEASER, "u3")).doesNotThrowAnyException();
    }
}
