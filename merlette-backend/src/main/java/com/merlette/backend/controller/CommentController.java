package com.merlette.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.merlette.backend.dto.CommentDto;
import com.merlette.backend.dto.CommentRequest;
import com.merlette.backend.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Commentaires", description = "Commentaires sur les observations")
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/observations/{id}/comments")
    public List<CommentDto> list(@PathVariable Integer id) {
        return commentService.list(id);
    }

    @PostMapping("/observations/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@PathVariable Integer id, @Valid @RequestBody CommentRequest request) {
        return commentService.create(id, request.content());
    }

    @PatchMapping("/comments/{id}")
    public CommentDto update(@PathVariable Integer id, @Valid @RequestBody CommentRequest request) {
        return commentService.update(id, request.content());
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        commentService.delete(id);
    }
}
