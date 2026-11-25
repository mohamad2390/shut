package com.shut.market;

/**
 * What the market tells people about. The Telegram bot implements it when it is switched on; without the bot nobody
 * is told, and people see offers and answers on the web. A failing implementation must never break the offer.
 */
public interface MarketEvents {

    /** The seller gets the new offer. */
    void offerArrived(Listing listing, Offer offer);

    /** The buyer learns the seller's answer (accepted or declined). */
    void offerAnswered(Listing listing, Offer offer);
}
