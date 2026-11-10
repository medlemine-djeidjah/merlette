package com.merlette.backend.observation;

import com.merlette.backend.api.ApiException;
import com.merlette.backend.user.UserRepository;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
public class ObservationService {
    @Getter @Setter
    public static class Creation {
        @NotNull @Positive private Integer categoryId;
        @NotNull @PastOrPresent private LocalDate observedAt;
        @Size(max = 120) private String title;
        @Size(max = 10000) private String description;
        @Size(max = 200) private String locationName;
        @DecimalMin("-90") @DecimalMax("90") private BigDecimal latitude;
        @DecimalMin("-180") @DecimalMax("180") private BigDecimal longitude;
        private List<MultipartFile> photos;
    }
    public record CategoryView(Integer id, String name, String slug) {
        public static CategoryView of(Category c) { return new CategoryView(c.getId(), c.getName(), c.getSlug()); }
    }
    public record AuthorView(Integer id, String username) {}
    public record PhotoView(Integer id, String url, short position) {}
    public record ObservationView(Integer id, String title, String description, Observation.Status status,
        CategoryView category, AuthorView author, LocalDate observedAt, String locationName,
        BigDecimal latitude, BigDecimal longitude, String coverUrl, List<PhotoView> photos,
        Instant createdAt, Instant updatedAt) {}
    public record Pagination(int page, int limit, long total, int totalPages) {}
    public record Feed(List<ObservationView> data, Pagination pagination) {}
    private final ObservationRepository observations;
    private final PhotoRepository photos;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final PhotoStorage storage;
    public ObservationService(ObservationRepository observations, PhotoRepository photos,
            CategoryRepository categories, UserRepository users, PhotoStorage storage) {
        this.observations = observations; this.photos = photos; this.categories = categories;
        this.users = users; this.storage = storage;
    }
    @Transactional
    public ObservationView create(Integer userId, Creation input) {
        var category = categories.findById(input.getCategoryId()).orElseThrow(() -> ApiException.invalid("Catégorie inconnue."));
        var author = users.findById(userId).orElseThrow(ApiException::notFound);
        var urls = storage.save(input.getPhotos());
        var observation = observations.saveAndFlush(new Observation(author, category, input));
        for (int i = 0; i < urls.size(); i++) photos.save(new Photo(observation, urls.get(i), i));
        photos.flush();
        return view(observation);
    }
    @Transactional(readOnly = true)
    public ObservationView detail(Integer id) { return view(observations.findById(id).orElseThrow(ApiException::notFound)); }
    @Transactional(readOnly = true)
    public Feed feed(String category, Observation.Status status, Integer userId, String sort, int page, int limit) {
        if (page < 1 || limit < 1 || limit > 50 || !List.of("recent", "oldest").contains(sort)) {
            throw ApiException.invalid("Pagination ou tri invalide : page ≥ 1, limit entre 1 et 50, sort recent ou oldest.");
        }
        var direction = sort.equals("oldest") ? Sort.Direction.ASC : Sort.Direction.DESC;
        org.springframework.data.jpa.domain.Specification<Observation> filters = (root, query, builder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("author"); root.fetch("category");
            }
            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (category != null) conditions.add(builder.equal(root.get("category").get("slug"), category));
            if (status != null) conditions.add(builder.equal(root.get("status"), status));
            if (userId != null) conditions.add(builder.equal(root.get("author").get("id"), userId));
            return builder.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = observations.findAll(filters, PageRequest.of(page - 1, limit, Sort.by(direction, "createdAt", "id")));
        return new Feed(result.stream().map(this::view).toList(), new Pagination(page, limit, result.getTotalElements(), result.getTotalPages()));
    }
    private ObservationView view(Observation o) {
        var images = photos.findByObservationIdOrderByPosition(o.getId()).stream()
            .map(p -> new PhotoView(p.getId(), p.getUrl(), p.getPosition())).toList();
        return new ObservationView(o.getId(), o.getTitle(), o.getDescription(), o.getStatus(),
            CategoryView.of(o.getCategory()), new AuthorView(o.getAuthor().getId(), o.getAuthor().getUsername()),
            o.getObservedAt(), o.getLocationName(), o.getLatitude(), o.getLongitude(),
            images.isEmpty() ? null : images.getFirst().url(), images, o.getCreatedAt(), o.getUpdatedAt());
    }
}
