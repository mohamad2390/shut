-- The members widget can show the result of a search; the words searched for are kept to answer its paging.
ALTER TABLE widget_messages ADD COLUMN query VARCHAR(200);
