package com.merlette.backend.service;

import com.merlette.backend.dto.CategoryDto;
import com.merlette.backend.dto.ObservationDetailDto;
import com.merlette.backend.dto.ObservationForm;
import com.merlette.backend.dto.ObservationSummaryDto;
import com.merlette.backend.dto.PageResponse;
import com.merlette.backend.dto.PhotoDto;
import com.merlette.backend.dto.SpeciesSummaryDto;
import com.merlette.backend.dto.UpdateObservationRequest;
import com.merlette.backend.dto.UserSummaryDto;
import com.merlette.backend.entity.Observation;
import com.merlette.backend.entity.ObservationStatus;
import com.merlette.backend.entity.Photo;
import com.merlette.backend.exception.ApiException;
import com.merlette.backend.repository.CommentRepository;
import com.merlette.backend.repository.ObservationRepository;
import com.merlette.backend.repository.PhotoRepository;
import com.merlette.backend.repository.ProposalRepository;
import com.merlette.backend.repository.UserRepository;
import com.merlette.backend.security.CurrentUser;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** Observations et leurs photos (#1, #8, #9, #13, #14, #15). */
@Service
@RequiredArgsConstructor
public class ObservationService {

    private final ObservationRepository observationRepository;
    private final PhotoRepository photoRepository;
    private final ProposalRepository proposalRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final ProposalService proposalService;
    private final FileStorageService fileStorage;
    private final CurrentUser currentUser;
    private final TransactionTemplate transactionTemplate;

    // --- Lecture -----------------------------------------------------------

    /** Fil paginé, filtrable par catégorie (slug), statut et auteur. */
    @Transactional(readOnly = true)
    public PageResponse<ObservationSummaryDto> list(String category, String status, Integer userId,
                                                    String sort, int page, int limit) {
        ObservationStatus statusFilter = parseStatus(status);
        Sort.Direction direction = switch (sort == null ? "recent" : sort) {
            case "recent" -> Sort.Direction.DESC;
            case "oldest" -> Sort.Direction.ASC;
            default -> throw ApiException.badRequest("sort doit valoir recent ou oldest.");
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, Math.clamp(limit, 1, 50),
                Sort.by(direction, "createdAt", "id"));
        String categoryFilter = (category == null || category.isBlank()) ? null : category;

        Page<Observation> result = observationRepository.search(categoryFilter,
                statusFilter == null ? null : statusFilter.name(), userId, pageable);
        return PageResponse.of(toSummaries(result.getContent()), result);
    }

    @Transactional(readOnly = true)
    public ObservationDetailDto get(Integer id) {
        return toDetail(findWithDetails(id));
    }

    // --- Écriture ----------------------------------------------------------

    /** Publie une observation avec 1 à 5 photos ; les fichiers sont supprimés si l'insertion échoue. */
    public ObservationDetailDto create(ObservationForm form, List<MultipartFile> files) {
        Integer userId = currentUser.id();
        List<MultipartFile> photos = nonEmpty(files);
        if (photos.isEmpty()) {
            throw ApiException.badRequest("Au moins une photo est obligatoire.");
        }
        if (photos.size() > FileStorageService.MAX_PHOTOS) {
            throw ApiException.badRequest("5 photos maximum.");
        }

        List<String> urls = fileStorage.saveAll(photos);
        try {
            return transactionTemplate.execute(tx -> {
                Observation o = new Observation();
                o.setAuthor(userRepository.findById(userId).orElseThrow(ApiException::unauthorized));
                o.setCategory(categoryService.getForInput(form.categoryId()));
                o.setObservedAt(form.observedAt());
                o.setTitle(blankToNull(form.title()));
                o.setDescription(blankToNull(form.description()));
                o.setLocationName(blankToNull(form.locationName()));
                o.setLatitude(form.latitude());
                o.setLongitude(form.longitude());
                observationRepository.save(o);
                savePhotos(o, urls);
                return toDetail(o);
            });
        } catch (RuntimeException e) {
            fileStorage.deleteAll(urls);
            throw e;
        }
    }

    @Transactional
    public ObservationDetailDto update(Integer id, UpdateObservationRequest request) {
        Observation o = findWithDetails(id);
        currentUser.requireOwner(o.getAuthor().getId());

        if (request.categoryId() != null) o.setCategory(categoryService.getForInput(request.categoryId()));
        if (request.observedAt() != null) o.setObservedAt(request.observedAt());
        if (request.title() != null) o.setTitle(blankToNull(request.title()));
        if (request.description() != null) o.setDescription(blankToNull(request.description()));
        if (request.locationName() != null) o.setLocationName(blankToNull(request.locationName()));
        if (request.latitude() != null) o.setLatitude(request.latitude());
        if (request.longitude() != null) o.setLongitude(request.longitude());

        observationRepository.flush(); // met à jour updatedAt avant de renvoyer la réponse
        return toDetail(o);
    }

    /** Suppression par l'auteur ou un admin : cascade en base, puis suppression des fichiers. */
    public void delete(Integer id) {
        List<String> urls = transactionTemplate.execute(tx -> {
            Observation o = findObservation(id);
            currentUser.requireOwnerOrAdmin(o.getAuthor().getId());
            List<String> photoUrls = photoRepository.findByObservationIdOrderByPosition(id).stream()
                    .map(Photo::getUrl).toList();
            observationRepository.deleteWithCascade(id);
            return photoUrls;
        });
        fileStorage.deleteAll(urls);
    }

    /** Ajoute des photos (auteur uniquement, 5 au total maximum). */
    public ObservationDetailDto addPhotos(Integer id, List<MultipartFile> files) {
        List<MultipartFile> photos = nonEmpty(files);
        transactionTemplate.executeWithoutResult(tx -> {
            Observation o = findObservation(id);
            currentUser.requireOwner(o.getAuthor().getId());
            if (photos.isEmpty()) {
                throw ApiException.badRequest("Au moins une photo est obligatoire.");
            }
            int existing = photoRepository.findByObservationIdOrderByPosition(id).size();
            if (existing + photos.size() > FileStorageService.MAX_PHOTOS) {
                throw ApiException.badRequest("5 photos maximum par observation.");
            }
        });

        List<String> urls = fileStorage.saveAll(photos);
        try {
            return transactionTemplate.execute(tx -> {
                Observation o = findWithDetails(id);
                savePhotos(o, urls);
                return toDetail(o);
            });
        } catch (RuntimeException e) {
            fileStorage.deleteAll(urls);
            throw e;
        }
    }

    /** Retire une photo (auteur uniquement) ; il doit en rester au moins une. */
    public void deletePhoto(Integer id, Integer photoId) {
        String url = transactionTemplate.execute(tx -> {
            Observation o = findObservation(id);
            currentUser.requireOwner(o.getAuthor().getId());
            List<Photo> photos = photoRepository.findByObservationIdOrderByPosition(id);
            Photo photo = photos.stream().filter(p -> p.getId().equals(photoId)).findFirst()
                    .orElseThrow(() -> ApiException.notFound("Photo introuvable."));
            if (photos.size() == 1) {
                throw ApiException.badRequest("Impossible de supprimer la dernière photo.");
            }
            photoRepository.delete(photo);
            return photo.getUrl();
        });
        fileStorage.delete(url);
    }

    // --- Outils ------------------------------------------------------------

    /** Enregistre les photos aux positions libres (0 à 4). */
    private void savePhotos(Observation o, List<String> urls) {
        Set<Integer> used = new HashSet<>();
        photoRepository.findByObservationIdOrderByPosition(o.getId()).forEach(p -> used.add(p.getPosition()));
        int position = 0;
        for (String url : urls) {
            while (used.contains(position)) {
                position++;
            }
            Photo photo = new Photo();
            photo.setObservation(o);
            photo.setUrl(url);
            photo.setPosition(position);
            photoRepository.save(photo);
            used.add(position);
        }
    }

    private List<ObservationSummaryDto> toSummaries(List<Observation> observations) {
        List<Integer> ids = observations.stream().map(Observation::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> covers = new HashMap<>();
        photoRepository.findByObservationIdInOrderByPosition(ids)
                .forEach(p -> covers.putIfAbsent(p.getObservation().getId(), p.getUrl()));
        Map<Integer, Long> proposalCounts = toMap(proposalRepository.countByObservationIds(ids));
        Map<Integer, Long> commentCounts = toMap(commentRepository.countByObservationIds(ids));

        return observations.stream().map(o -> new ObservationSummaryDto(
                o.getId(), o.getTitle(), o.getStatus().name(),
                CategoryDto.from(o.getCategory()), UserSummaryDto.from(o.getAuthor()),
                covers.get(o.getId()), o.getObservedAt(), o.getCreatedAt(),
                proposalCounts.getOrDefault(o.getId(), 0L), commentCounts.getOrDefault(o.getId(), 0L),
                SpeciesSummaryDto.from(o.getIdentifiedSpecies()))).toList();
    }

    private ObservationDetailDto toDetail(Observation o) {
        List<PhotoDto> photos = photoRepository.findByObservationIdOrderByPosition(o.getId()).stream()
                .map(PhotoDto::from).toList();
        return new ObservationDetailDto(
                o.getId(), o.getTitle(), o.getDescription(), o.getStatus().name(),
                SpeciesSummaryDto.from(o.getIdentifiedSpecies()),
                CategoryDto.from(o.getCategory()), UserSummaryDto.from(o.getAuthor()),
                o.getObservedAt(), o.getLocationName(), o.getLatitude(), o.getLongitude(),
                photos,
                proposalService.buildProposals(o).proposals(),
                proposalService.myVoteProposalId(o.getId(), currentUser.idOrNull()),
                commentRepository.countByObservationId(o.getId()),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private static Map<Integer, Long> toMap(List<Object[]> rows) {
        Map<Integer, Long> map = new HashMap<>();
        rows.forEach(r -> map.put((Integer) r[0], (Long) r[1]));
        return map;
    }

    private static ObservationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ObservationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("status doit valoir NEEDS_ID ou IDENTIFIED.");
        }
    }

    private static List<MultipartFile> nonEmpty(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> !f.isEmpty()).toList();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private Observation findObservation(Integer id) {
        return observationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Observation introuvable."));
    }

    private Observation findWithDetails(Integer id) {
        return observationRepository.findWithDetails(id)
                .orElseThrow(() -> ApiException.notFound("Observation introuvable."));
    }
}
