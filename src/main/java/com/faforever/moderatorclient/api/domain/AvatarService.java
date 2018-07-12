package com.faforever.moderatorclient.api.domain;

import com.faforever.commons.api.dto.Avatar;
import com.faforever.commons.api.dto.AvatarAssignment;
import com.faforever.moderatorclient.api.ElideRouteBuilder;
import com.faforever.moderatorclient.api.FafApiCommunicationService;
import com.faforever.moderatorclient.api.dto.AvatarAssignmentUpdate;
import com.faforever.moderatorclient.api.dto.AvatarMetadata;
import com.faforever.moderatorclient.mapstruct.AvatarAssignmentMapper;
import com.faforever.moderatorclient.ui.domain.AvatarAssignmentFX;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class AvatarService {
    private final FafApiCommunicationService fafApi;
    private final AvatarAssignmentMapper avatarAssignmentMapper;

    public AvatarService(FafApiCommunicationService fafApi, AvatarAssignmentMapper avatarAssignmentMapper) {
        this.fafApi = fafApi;
        this.avatarAssignmentMapper = avatarAssignmentMapper;
    }

    public List<Avatar> getAll() {
        log.debug("Retrieving all avatars");
        List<Avatar> result = fafApi.getAll(ElideRouteBuilder.of(Avatar.class)
                .addInclude("assignments")
                .addInclude("assignments.player"));
        log.trace("found {} avatars", result.size());
        return result;
    }

    private List<Avatar> findAvatarsByAttribute(@NotNull String attribute, @NotNull String pattern) {
        log.debug("Searching for avatars by attribute '{}' with pattern: {}", attribute, pattern);
        ElideRouteBuilder<Avatar> routeBuilder = ElideRouteBuilder.of(Avatar.class)
                .addInclude("assignments")
                .addInclude("assignments.player")
                .filter(ElideRouteBuilder.qBuilder().string(attribute).eq(pattern));

        List<Avatar> result = fafApi.getAll(routeBuilder);
        log.trace("found {} avatars", result.size());
        return result;
    }

    public List<Avatar> findAvatarsById(@NotNull String pattern) {
        return findAvatarsByAttribute("id", pattern);
    }

    public List<Avatar> findAvatarsByTooltip(@NotNull String pattern) {
        return findAvatarsByAttribute("tooltip", pattern);
    }

    public List<Avatar> findAvatarsByAssignedUser(@NotNull String pattern) {
        log.debug("Searching for avatars by assigned player with pattern: {}", pattern);
        boolean isNumeric = pattern.matches("^[0-9]+$");

        ElideRouteBuilder<Avatar> routeBuilder = ElideRouteBuilder.of(Avatar.class)
                .addInclude("assignments")
                .addInclude("assignments.player")
                .filter(ElideRouteBuilder.qBuilder().string(isNumeric ? "assignments.player.id" : "assignments.player.login").eq(pattern));

        List<Avatar> result = fafApi.getAll(routeBuilder);
        log.trace("found {} avatars", result.size());
        return result;
    }

    public void uploadAvatar(String name, File avatarImageFile) {
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = createAvatarMultipartRequest(name, avatarImageFile);
        final String route = "/avatars/upload";
        log.debug("Sending API request: {}", route);
        fafApi.getRestOperations().exchange(
                route,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }

    public void reuploadAvatar(String avatarId, String name, File avatarImageFile) {
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = createAvatarMultipartRequest(name, avatarImageFile);
        final String route = "/avatars/{0}/upload";
        log.debug("Sending API request: {}", route);
        fafApi.getRestOperations().exchange(
                route,
                HttpMethod.POST,
                requestEntity,
                String.class,
                avatarId
        );
    }

    public void deleteAvatar(String avatarId) {
        final String route = "/avatars/{0}";
        log.debug("Sending API request: {}", route);
        fafApi.getRestOperations().delete(route, avatarId);
    }

    public List<Avatar> getAllAvatarsWithPlayerAssignments() {
        return fafApi.getAll(ElideRouteBuilder.of(Avatar.class)
                .addInclude("assignments")
                .addInclude("assignments.player"));
    }

    public void updateAvatarMetadata(String avatarId, String name) {
        fafApi.patch(ElideRouteBuilder.of(Avatar.class).id(avatarId),
                (Avatar) new Avatar().setTooltip(name).setId(avatarId));
    }

    @NotNull
    private HttpEntity<LinkedMultiValueMap<String, Object>> createAvatarMultipartRequest(String name, File avatarImageFile) {
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new FileSystemResource(avatarImageFile));
        map.add("metadata", new AvatarMetadata().setName(name));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(map, headers);
    }

    public String createAvatarAssignment(AvatarAssignmentFX avatarAssignmentFX) {
        return createAvatarAssignment(avatarAssignmentMapper.map(avatarAssignmentFX));
    }

    private String createAvatarAssignment(AvatarAssignment avatarAssignment) {
        log.debug("Creating avatar assignment");
        return fafApi.post(ElideRouteBuilder.of(AvatarAssignment.class), avatarAssignment).getId();
    }

    public void patchAvatarAssignment(AvatarAssignmentFX avatarAssignmentFX) {
        patchAvatarAssignment(avatarAssignmentMapper.map(avatarAssignmentFX));
    }

    private void patchAvatarAssignment(AvatarAssignment avatarAssignment) {
        log.debug("Patching avatar assignmenet id: " + avatarAssignment.getId());
        ElideRouteBuilder<AvatarAssignment> routeBuilder = ElideRouteBuilder.of(AvatarAssignment.class)
                .id(avatarAssignment.getId());
        fafApi.patch(routeBuilder, avatarAssignment);
    }

    public void patchAvatarAssignment(AvatarAssignmentUpdate avatarAssignmentUpdate) {
        log.debug("Patching avatar assignmenet id: " + avatarAssignmentUpdate.getId());
        ElideRouteBuilder<AvatarAssignment> routeBuilder = ElideRouteBuilder.of(AvatarAssignment.class)
                .id(avatarAssignmentUpdate.getId());
        fafApi.patch(routeBuilder, avatarAssignmentUpdate);
    }

    public void removeAvatarAssignment(AvatarAssignmentFX avatarAssignmentFX) {
        removeAvatarAssignment(avatarAssignmentMapper.map(avatarAssignmentFX));
    }

    public void removeAvatarAssignment(AvatarAssignment avatarAssignment) {
        log.debug("Removing avatar assignmenet id: " + avatarAssignment.getId());
        ElideRouteBuilder<AvatarAssignment> routeBuilder = ElideRouteBuilder.of(AvatarAssignment.class)
                .id(avatarAssignment.getId());
        fafApi.delete(routeBuilder);
    }
}
