-- Widget messages are no longer only price widgets: the calendar widget is stored in the same table.
-- Existing rows are price widgets.
ALTER TABLE price_widgets RENAME TO widget_messages;
ALTER TABLE widget_messages ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'PRICES';
ALTER TABLE widget_messages ALTER COLUMN kind DROP DEFAULT;
