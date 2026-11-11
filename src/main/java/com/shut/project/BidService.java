package com.shut.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Offers providers make on projects. In a tender the offers are sealed: only the project's owner can read them,
 * so every method that shows other people's offers checks that the caller owns the project.
 */
@Service
public class BidService {

    public static final int MAX_NOTE_LENGTH = 500;

    private final BidRepository bids;
    private final ProjectRepository projects;
    private final Clock clock;

    public BidService(BidRepository bids, ProjectRepository projects, Clock clock) {
        this.bids = bids;
        this.projects = projects;
        this.clock = clock;
    }

    /**
     * Places an offer, or replaces the provider's earlier one.
     *
     * @param note optional words for the project owner; blank means none
     * @throws BidRefusedException      if the project is missing, closed to offers, or the bidder's own
     * @throws IllegalArgumentException if the amount is not positive or the note is too long
     */
    @Transactional
    public PlacedBid place(Long projectId, Long bidderId, long amount, String note) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        String cleanedNote = note == null || note.isBlank() ? null : note.strip();
        if (cleanedNote != null && cleanedNote.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Note is longer than " + MAX_NOTE_LENGTH + " characters");
        }

        Project project = projects.findById(projectId)
                .orElseThrow(() -> new BidRefusedException(BidRefusal.PROJECT_NOT_FOUND));
        if (project.getOwnerId().equals(bidderId)) {
            throw new BidRefusedException(BidRefusal.OWN_PROJECT);
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!project.isAcceptingBids(now)) {
            throw new BidRefusedException(BidRefusal.NOT_TAKING_BIDS);
        }

        Optional<Bid> existing = bids.findByProjectIdAndBidderId(projectId, bidderId);
        if (existing.isPresent()) {
            existing.get().revise(amount, cleanedNote, now);
            return new PlacedBid(existing.get(), project, true);
        }
        return new PlacedBid(bids.save(new Bid(projectId, bidderId, amount, cleanedNote, now)), project, false);
    }

    /** Takes back the provider's offer while the project is still open. Returns whether there was one. */
    @Transactional
    public boolean withdraw(Long projectId, Long bidderId) {
        boolean open = projects.findById(projectId).filter(p -> p.getStatus() == ProjectStatus.OPEN).isPresent();
        if (!open) {
            return false;
        }
        return bids.findByProjectIdAndBidderId(projectId, bidderId)
                .map(bid -> {
                    bids.delete(bid);
                    return true;
                })
                .orElse(false);
    }

    /** The provider's own offer on the project. */
    @Transactional(readOnly = true)
    public Optional<Bid> findOwn(Long projectId, Long bidderId) {
        return bids.findByProjectIdAndBidderId(projectId, bidderId);
    }

    /** How many offers the project has. Safe to show to anyone: it says nothing about prices. */
    @Transactional(readOnly = true)
    public long count(Long projectId) {
        return bids.countByProjectId(projectId);
    }

    /** The project's offers, cheapest first. Empty if the project is not the owner's. */
    @Transactional(readOnly = true)
    public Optional<Page<Bid>> bidsFor(Long projectId, Long ownerId, int page, int size) {
        return projects.findById(projectId)
                .filter(project -> project.getOwnerId().equals(ownerId))
                .map(project -> bids.findByProjectId(projectId, PageRequest.of(Math.max(0, page), size,
                        Sort.by(Sort.Order.asc("amount"), Sort.Order.asc("id")))));
    }

    /** One offer, if it is on a project the caller owns. */
    @Transactional(readOnly = true)
    public Optional<Bid> findForOwner(Long bidId, Long ownerId) {
        return bids.findById(bidId)
                .filter(bid -> projects.findById(bid.getProjectId())
                        .filter(project -> project.getOwnerId().equals(ownerId))
                        .isPresent());
    }

    /** The provider's own offers, newest first. */
    @Transactional(readOnly = true)
    public Page<Bid> mine(Long bidderId, int page, int size) {
        return bids.findByBidderId(bidderId, PageRequest.of(Math.max(0, page), size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }
}
