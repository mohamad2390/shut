package com.shut.studio;

import com.shut.auth.InvalidPhoneException;
import com.shut.auth.PhoneNumbers;
import com.shut.user.entity.User;
import com.shut.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** A studio's people list: finding a specialist, inviting them, and their answer. */
@Service
public class StudioMemberService {

    public static final int MAX_PEOPLE = 30;

    /** What came of an invitation. */
    public enum InviteResult { INVITED, NOT_FOUND, NOT_SPECIALIST, SELF, ALREADY_IN, ALREADY_INVITED, LIMIT_REACHED }

    /** The result, and the invited specialist when there is one. */
    public record Invitation(InviteResult result, User user, StudioMember member) {
        static Invitation of(InviteResult result) {
            return new Invitation(result, null, null);
        }
    }

    private final StudioMemberRepository members;
    private final UserRepository users;

    public StudioMemberService(StudioMemberRepository members, UserRepository users) {
        this.members = members;
        this.users = users;
    }

    /** Finds the member by a mobile number or a Telegram username, with or without the @. */
    @Transactional(readOnly = true)
    public Optional<User> find(String query) {
        String text = query == null ? "" : query.strip();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            return users.findByPhoneNumber(PhoneNumbers.normalize(text));
        } catch (InvalidPhoneException e) {
            return users.findByTelegramUsernameIgnoreCase(text.startsWith("@") ? text.substring(1) : text);
        }
    }

    /** Invites an already registered specialist; they join once they accept. */
    @Transactional
    public Invitation invite(Studio studio, String query) {
        Optional<User> found = find(query);
        if (found.isEmpty()) {
            return Invitation.of(InviteResult.NOT_FOUND);
        }
        User user = found.get();
        if (!user.isProvider()) {
            return Invitation.of(InviteResult.NOT_SPECIALIST);
        }
        if (user.getId().equals(studio.getOwnerId())) {
            return Invitation.of(InviteResult.SELF);
        }
        Optional<StudioMember> existing = members.findByStudioIdAndUserId(studio.getId(), user.getId());
        if (existing.isPresent()) {
            return Invitation.of(existing.get().getStatus() == MemberStatus.ACCEPTED
                    ? InviteResult.ALREADY_IN : InviteResult.ALREADY_INVITED);
        }
        if (members.countByStudioId(studio.getId()) >= MAX_PEOPLE) {
            return Invitation.of(InviteResult.LIMIT_REACHED);
        }
        StudioMember member = members.save(new StudioMember(studio.getId(), user.getId()));
        return new Invitation(InviteResult.INVITED, user, member);
    }

    @Transactional(readOnly = true)
    public Optional<StudioMember> find(Long memberId) {
        return members.findById(memberId);
    }

    /** The specialist's answer to an invitation addressed to them. Empty if it is not theirs or already answered. */
    @Transactional
    public Optional<StudioMember> respond(Long memberId, Long userId, boolean accept) {
        return members.findById(memberId)
                .filter(member -> member.getUserId().equals(userId) && member.getStatus() == MemberStatus.PENDING)
                .map(member -> {
                    if (accept) {
                        member.accept();
                        return members.save(member);
                    }
                    members.delete(member);
                    return member;
                });
    }

    /** The owner takes a person (or a pending invitation) off the list. */
    @Transactional
    public boolean remove(Studio studio, Long userId) {
        return members.findByStudioIdAndUserId(studio.getId(), userId).map(member -> {
            members.delete(member);
            return true;
        }).orElse(false);
    }

    /** The specialist leaves a studio. */
    @Transactional
    public boolean leave(Long studioId, Long userId) {
        return members.findByStudioIdAndUserId(studioId, userId)
                .filter(member -> member.getStatus() == MemberStatus.ACCEPTED)
                .map(member -> {
                    members.delete(member);
                    return true;
                }).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<StudioMember> people(Long studioId) {
        return members.findByStudioIdAndStatusOrderByIdAsc(studioId, MemberStatus.ACCEPTED);
    }

    @Transactional(readOnly = true)
    public List<StudioMember> pending(Long studioId) {
        return members.findByStudioIdAndStatusOrderByIdAsc(studioId, MemberStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public boolean isIn(Long studioId, Long userId) {
        return members.existsByStudioIdAndUserIdAndStatus(studioId, userId, MemberStatus.ACCEPTED);
    }

    @Transactional(readOnly = true)
    public long peopleCount(Long studioId) {
        return members.countByStudioIdAndStatus(studioId, MemberStatus.ACCEPTED);
    }
}
