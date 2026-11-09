package com.shut.showcase;

/** Query result: how many items a user has in one category. */
public interface CategoryCount {

    ShowcaseCategory getCategory();

    long getTotal();
}
