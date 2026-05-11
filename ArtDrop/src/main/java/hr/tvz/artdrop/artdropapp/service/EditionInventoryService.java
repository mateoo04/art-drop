package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.model.Artwork;

public interface EditionInventoryService {
    /** Returns null when artwork is not an EDITION; otherwise 0 or positive remaining count. */
    Integer remaining(Artwork artwork);

    boolean canFulfill(Artwork artwork, int quantity);
}
