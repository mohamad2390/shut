package com.shut.browse;

import com.shut.profile.entity.ProfessionalProfile;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Active members with a display name, other than the searcher, where every word matches the name, headline, city,
 * country, a skill or a service city. Text is compared without spaces, half-spaces and letter-form differences.
 */
final class ProviderSearchSpecification {

    private ProviderSearchSpecification() {
    }

    /** @param patterns LIKE patterns from {@link SearchPattern#words}; all of them must match */
    static Specification<ProfessionalProfile> matching(List<String> patterns, Long excludeUserId) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isTrue(root.get("user").get("active")));
            all.add(cb.notEqual(root.get("user").get("id"), excludeUserId));
            all.add(cb.isNotNull(root.get("displayName")));
            for (String pattern : patterns) {
                all.add(cb.or(
                        like(cb, root.get("displayName"), pattern),
                        like(cb, root.get("headline"), pattern),
                        like(cb, root.get("baseCity"), pattern),
                        like(cb, root.get("baseCountry"), pattern),
                        cb.exists(inCollection(query, cb, root, "skills", pattern)),
                        cb.exists(inCollection(query, cb, root, "serviceCities", pattern))));
            }
            return cb.and(all.toArray(Predicate[]::new));
        };
    }

    /** One member can have several skills, so each word is looked for in all of them, not in one joined row. */
    private static Subquery<Integer> inCollection(CriteriaQuery<?> query, CriteriaBuilder cb,
                                                  Root<ProfessionalProfile> root, String attribute, String pattern) {
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<ProfessionalProfile> own = sub.correlate(root);
        Expression<String> value = own.join(attribute).as(String.class);
        return sub.select(cb.literal(1)).where(like(cb, value, pattern));
    }

    private static Predicate like(CriteriaBuilder cb, Expression<String> column, String pattern) {
        return cb.like(normalized(cb, column), pattern, '\\');
    }

    private static Expression<String> normalized(CriteriaBuilder cb, Expression<String> column) {
        Expression<String> text = cb.lower(cb.coalesce(column, ""));
        for (String[] change : new String[][]{{" ", ""}, {String.valueOf(SearchPattern.ZWNJ), ""}, {"ي", "ی"}, {"ك", "ک"}}) {
            text = cb.function("replace", String.class, text, cb.literal(change[0]), cb.literal(change[1]));
        }
        return text;
    }
}
