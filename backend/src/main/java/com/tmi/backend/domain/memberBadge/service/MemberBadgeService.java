package com.tmi.backend.domain.memberBadge.service;

import com.tmi.backend.domain.auth.util.SecurityUtil;
import com.tmi.backend.domain.badge.entity.Badge;
import com.tmi.backend.domain.badge.repository.BadgeRepository;
import com.tmi.backend.domain.member.entity.Member;
import com.tmi.backend.domain.member.repository.MemberRepository;
import com.tmi.backend.domain.memberBadge.dto.response.MemberBadgeListResponse;
import com.tmi.backend.domain.memberBadge.entity.MemberBadge;
import com.tmi.backend.domain.memberBadge.repository.MemberBadgeRepository;
import com.tmi.backend.global.common.response.ServiceResult;
import com.tmi.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBadgeService {

  private final MemberRepository memberRepository;
  private final BadgeRepository badgeRepository;
  private final MemberBadgeRepository memberBadgeRepository;

  public ServiceResult<MemberBadgeListResponse> getMemberBadges(Long memberId) {
    Member member = memberRepository.findById(memberId).orElse(null);
    if (member == null) {
      return ServiceResult.fail(ErrorCode.USER_NOT_FOUND);
    }

    List<MemberBadge> memberBadgeList = memberBadgeRepository.findAllByMember_IdOrderByReceivedAtDesc(
        memberId);

    return ServiceResult.ok(MemberBadgeListResponse.of(memberBadgeList));
  }

  @Transactional
  public ServiceResult<Map<String, Long>> updateRepresentative(Long memberBadgeId) {
    MemberBadge memberBadge = memberBadgeRepository.findById(memberBadgeId).orElse(null);
    if (memberBadge == null) {
      return ServiceResult.fail(ErrorCode.BADGE_NOT_FOUND);
    }

    Long memberId = memberBadge.getMember().getId();

    if (!SecurityUtil.memberCheck(memberId)) {
      return ServiceResult.fail(ErrorCode.AUTH_ACCESS_DENIED);
    }

    Member member;
    try {
      member = memberRepository.findByIdWithLock(memberId).orElse(null);
      if (member == null) {
        return ServiceResult.fail(ErrorCode.USER_NOT_FOUND);
      }
    } catch (PessimisticLockingFailureException e) {
      return ServiceResult.fail(ErrorCode.BADGE_UPDATE_CONFLICT);
    }

    memberBadgeRepository.cancleRepresentative(memberId);
    memberBadgeRepository.setRepresentative(memberBadgeId);

    return ServiceResult.ok(Map.of("memberBadgeId", memberBadgeId));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean acceptedBadge(Long memberId, Long badgeId) {
    if (memberBadgeRepository.existsByMemberIdAndBadgeId(memberId, badgeId)) {
      return false;
    }

    Member member = memberRepository.findById(memberId).orElse(null);
    if (member == null) {
      return false;
    }
    Badge badge = badgeRepository.findById(badgeId).orElse(null);
    if (badge == null) {
      return false;
    }
    MemberBadge memberBadge = memberBadgeRepository.save(MemberBadge.of(member, badge,
        LocalDateTime.now(ZoneOffset.UTC)));
    return true;
  }
}