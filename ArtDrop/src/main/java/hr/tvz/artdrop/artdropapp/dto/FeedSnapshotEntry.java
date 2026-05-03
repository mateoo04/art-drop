package hr.tvz.artdrop.artdropapp.dto;

/**
 * One row in the cached home-feed snapshot (artwork id or challenge promo id).
 */
public record FeedSnapshotEntry(FeedSnapshotRowKind kind, long id) {}
