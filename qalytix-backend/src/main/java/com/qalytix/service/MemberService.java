package com.qalytix.service;

import com.qalytix.dto.request.UpdateMemberRoleRequest;
import com.qalytix.dto.response.MemberResponse;
import com.qalytix.security.AuthenticatedUser;

import java.util.List;

public interface MemberService {

    List<MemberResponse> listMembers();

    MemberResponse updateRole(Long memberId, UpdateMemberRoleRequest request, AuthenticatedUser caller);

    void removeMember(Long memberId, AuthenticatedUser caller);
}
