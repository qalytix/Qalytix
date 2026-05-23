package com.qalytix.controller;

import com.qalytix.dto.request.UpdateMemberRoleRequest;
import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.MemberResponse;
import com.qalytix.security.AuthenticatedUser;
import com.qalytix.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs/me/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ApiResponse<List<MemberResponse>> listMembers() {
        return ApiResponse.ok(memberService.listMembers());
    }

    @PatchMapping("/{memberId}/role")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ApiResponse<MemberResponse> updateRole(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.ok(memberService.updateRole(memberId, request, currentUser));
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public void removeMember(
            @PathVariable Long memberId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        memberService.removeMember(memberId, currentUser);
    }
}
